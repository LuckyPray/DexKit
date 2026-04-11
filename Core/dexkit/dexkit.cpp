// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#include "include/dexkit.h"
#include "include/query_context.h"

#include <algorithm>

#include "zip_archive.h"
#include "ThreadPool.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"
#include "utils/dex_descriptor_util.h"

#define WRITE_FILE_BLOCK_SIZE (1024 * 1024)

namespace dexkit {

bool comp(std::unique_ptr<DexItem> &a, std::unique_ptr<DexItem> &b) {
    return a->GetDexId() < b->GetDexId();
}

static uint32_t NormalizeThreadNum(uint32_t thread_num) {
    return std::max<uint32_t>(1U, thread_num);
}

template<typename T>
static void DrainRemainingFutures(std::vector<std::future<T>> &futures, size_t start_index) {
    for (size_t i = start_index; i < futures.size(); ++i) {
        (void) futures[i].get();
    }
}

#if DEXKIT_ENABLE_INTERNAL_METRICS
static void PublishLastQueryMetrics(const QueryContext &query_context) {
    query_context.PublishSnapshotToCurrentThread();
}
#endif

template<typename QueryType>
static bool ConfigureFindFirstQuery(QueryContext &query_context, const QueryType *query) {
    auto find_first = query->find_first();
    if (!find_first) {
        return false;
    }
    query_context.EnableEarlyExit();
    query_context.SetQueryPriority(QueryPriority::LatencySensitive);
    return true;
}

static std::string NormalizeDeclaredClassLookupName(std::string_view class_name) {
    if (class_name.empty()) {
        return {};
    }
    if (class_name.starts_with('L') || class_name.starts_with('[') || IsPrimitiveType(class_name)) {
        return std::string(class_name);
    }
    return NameToDescriptor(class_name);
}

static bool HasCompositeBatchUsingStringsMatchers(
        const flatbuffers::Vector<flatbuffers::Offset<schema::BatchUsingStringsMatcher>> *matchers
) {
    if (matchers == nullptr) {
        return false;
    }
    for (int i = 0; i < matchers->size(); ++i) {
        auto using_strings = matchers->Get(i)->using_strings();
        if (using_strings == nullptr) {
            continue;
        }
        for (int j = 0; j < using_strings->size(); ++j) {
            if (HasComposite(using_strings->Get(j))) {
                return true;
            }
        }
    }
    return false;
}

DexKit::QueryExecutionGuard::~QueryExecutionGuard() {
    if (owner_ != nullptr) {
        owner_->LeaveQueryExecution();
    }
}

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    if (unzip_thread_num > 0) {
        _thread_num.store(NormalizeThreadNum(static_cast<uint32_t>(unzip_thread_num)), std::memory_order_release);
    }
    std::lock_guard lock(_mutex);
    AddZipPath(apk_path, unzip_thread_num);
    std::sort(dex_items.begin(), dex_items.end(), comp);
}

void DexKit::SetThreadNum(int num) {
    auto thread_num = NormalizeThreadNum(num > 0 ? static_cast<uint32_t>(num) : 1U);
    _thread_num.store(thread_num, std::memory_order_release);
    std::lock_guard lock(query_executor_mutex);
    shared_query_scheduler_.reset();
    shared_query_pool_.reset();
    shared_query_pool_thread_num_ = 0;
}

void DexKit::SetMaxConcurrentQueries(uint32_t max_concurrent_queries) {
    max_concurrent_queries_.store(max_concurrent_queries, std::memory_order_release);
    query_execution_cv.notify_all();
}

#if DEXKIT_ENABLE_INTERNAL_METRICS
void DexKit::SetQueryMetricsEnabled(bool enabled) {
    query_metrics_enabled_.store(enabled, std::memory_order_release);
    if (!enabled) {
        std::lock_guard lock(query_metrics_history_mutex);
        query_metrics_history_.clear();
        dropped_query_metrics_history_records_ = 0;
    }
}

QuerySchedulerMetricsSnapshot DexKit::GetQuerySchedulerMetricsSnapshot() const {
    std::lock_guard lock(query_executor_mutex);
    if (!shared_query_scheduler_) {
        return {};
    }
    return shared_query_scheduler_->GetMetricsSnapshot();
}

void DexKit::ResetQuerySchedulerMetrics() const {
    std::lock_guard lock(query_executor_mutex);
    if (!shared_query_scheduler_) {
        return;
    }
    shared_query_scheduler_->ResetMetrics();
}

QueryMetricsSnapshot DexKit::GetLastQueryMetricsSnapshot() {
    return QueryContext::LastQueryMetricsSnapshot();
}

QueryMetricsHistorySnapshot DexKit::GetQueryMetricsHistorySnapshot() const {
    std::lock_guard lock(query_metrics_history_mutex);
    QueryMetricsHistorySnapshot snapshot;
    snapshot.dropped_records = dropped_query_metrics_history_records_;
    snapshot.records.assign(query_metrics_history_.begin(), query_metrics_history_.end());
    return snapshot;
}

void DexKit::ResetQueryMetricsHistory() {
    std::lock_guard lock(query_metrics_history_mutex);
    query_metrics_history_.clear();
    dropped_query_metrics_history_records_ = 0;
}
#endif

Error DexKit::InitFullCache() {
    auto execution_guard = EnterQueryExecution(UINT32_MAX);
    return Error::SUCCESS;
}

DexKit::QueryExecutionGuard DexKit::EnterQueryExecution(uint32_t required_flags) {
    std::unique_lock lock(query_execution_mutex);
    uint64_t shared_pool_admission_ticket = 0;
    bool required_warmup_pending = false;
    auto refresh_required_warmup_state = [this, required_flags, &required_warmup_pending]() {
        required_warmup_pending = required_flags != 0 && NeedWarmUp(required_flags);
    };
    auto track_required_warmup_request = [this, required_flags, &required_warmup_pending]() {
        if (required_warmup_pending) {
            pending_warmup_flags |= required_flags;
        }
    };
    auto enqueue_shared_pool_admission_ticket = [this, &shared_pool_admission_ticket]() {
        if (shared_pool_admission_ticket != 0) {
            return;
        }
        shared_pool_admission_ticket = next_shared_pool_admission_ticket_++;
        shared_pool_admission_wait_queue_.push_back(shared_pool_admission_ticket);
    };
    auto dequeue_shared_pool_admission_ticket = [this, &shared_pool_admission_ticket]() {
        if (shared_pool_admission_ticket == 0) {
            return false;
        }
        auto it = std::find(
                shared_pool_admission_wait_queue_.begin(),
                shared_pool_admission_wait_queue_.end(),
                shared_pool_admission_ticket
        );
        if (it == shared_pool_admission_wait_queue_.end()) {
            shared_pool_admission_ticket = 0;
            return false;
        }
        shared_pool_admission_wait_queue_.erase(it);
        shared_pool_admission_ticket = 0;
        return true;
    };

    refresh_required_warmup_state();
    track_required_warmup_request();

    while (true) {
        if (warmup_inflight) {
            query_execution_cv.wait(lock, [this] {
                return !warmup_inflight;
            });
            refresh_required_warmup_state();
            track_required_warmup_request();
            continue;
        }

        if (pending_warmup_flags != 0) {
            if (active_query_count == 0) {
                auto warmup_flags = pending_warmup_flags;
                pending_warmup_flags = 0;
                warmup_inflight = true;
                lock.unlock();
                InitDexCache(warmup_flags);
                lock.lock();
                warmup_inflight = false;
                query_execution_cv.notify_all();
            } else {
                query_execution_cv.wait(lock, [this] {
                    return warmup_inflight || active_query_count == 0;
                });
            }
            refresh_required_warmup_state();
            track_required_warmup_request();
            continue;
        }

        if (required_warmup_pending) {
            track_required_warmup_request();
            continue;
        }

        auto max_concurrent_queries = max_concurrent_queries_.load(std::memory_order_acquire);
        if (max_concurrent_queries == 0) {
            if (dequeue_shared_pool_admission_ticket()) {
                query_execution_cv.notify_all();
            }
        } else if (shared_pool_admission_ticket != 0 ||
                   active_query_count >= max_concurrent_queries ||
                   !shared_pool_admission_wait_queue_.empty()) {
            enqueue_shared_pool_admission_ticket();
            auto is_ticket_turn = !shared_pool_admission_wait_queue_.empty() &&
                                  shared_pool_admission_wait_queue_.front() == shared_pool_admission_ticket;
            if (active_query_count >= max_concurrent_queries || !is_ticket_turn) {
                auto admission_ticket = shared_pool_admission_ticket;
                query_execution_cv.wait(lock, [this, max_concurrent_queries, admission_ticket] {
                    auto current_max_concurrent_queries = max_concurrent_queries_.load(std::memory_order_acquire);
                    auto is_ticket_turn = !shared_pool_admission_wait_queue_.empty() &&
                                          shared_pool_admission_wait_queue_.front() == admission_ticket;
                    auto ticket_can_enter = is_ticket_turn &&
                                            active_query_count < current_max_concurrent_queries;
                    return warmup_inflight ||
                           pending_warmup_flags != 0 ||
                           current_max_concurrent_queries != max_concurrent_queries ||
                           ticket_can_enter;
                });
                continue;
            }
        }

        ++active_query_count;
        if (shared_pool_admission_ticket != 0) {
            DEXKIT_CHECK(!shared_pool_admission_wait_queue_.empty());
            DEXKIT_CHECK(shared_pool_admission_wait_queue_.front() == shared_pool_admission_ticket);
            shared_pool_admission_wait_queue_.pop_front();
            shared_pool_admission_ticket = 0;
            query_execution_cv.notify_all();
        }
        return QueryExecutionGuard(this);
    }
}

void DexKit::LeaveQueryExecution() {
    std::lock_guard lock(query_execution_mutex);
    DEXKIT_CHECK(active_query_count > 0);
    --active_query_count;
    query_execution_cv.notify_all();
}

bool DexKit::NeedWarmUp(uint32_t init_flags) const {
    if (init_flags == 0) {
        return false;
    }

    uint32_t cross_ref_flags = init_flags & (kCallerMethod | kRwFieldMethod);
    uint32_t cache_flags = init_flags & ~cross_ref_flags;

    if (cache_flags != 0) {
        for (const auto &dex_item: dex_items) {
            if (dex_item->NeedInitCache(cache_flags)) {
                return true;
            }
        }
    }

    if (cross_ref_flags != 0) {
        for (const auto &dex_item: dex_items) {
            if (dex_item->NeedPutCrossRef(cross_ref_flags)) {
                return true;
            }
        }
        auto aggregate_ready_flags = cross_ref_aggregate_flag.load(std::memory_order_acquire);
        if ((aggregate_ready_flags & cross_ref_flags) != cross_ref_flags) {
            return true;
        }
    }

    return false;
}

#if DEXKIT_ENABLE_INTERNAL_METRICS
void DexKit::RecordQueryMetrics(const QueryContext &query_context) {
    if (!query_context.AreMetricsEnabled()) {
        return;
    }
    QueryMetricsRecord record;
    record.kind = query_context.GetKind();
    record.priority = query_context.GetQueryPriority();
    record.metrics = query_context.SnapshotMetrics();

    std::lock_guard lock(query_metrics_history_mutex);
    if (query_metrics_history_.size() >= kQueryMetricsHistoryCapacity) {
        query_metrics_history_.pop_front();
        ++dropped_query_metrics_history_records_;
    }
    query_metrics_history_.push_back(std::move(record));
}
#endif

std::unique_ptr<IQueryExecutor> DexKit::CreateQueryExecutor(QueryContext &query_context) const {
    auto thread_num = NormalizeThreadNum(_thread_num.load(std::memory_order_acquire));
    std::function<bool()> should_skip_task;
    if (query_context.IsEarlyExitEnabled()) {
        should_skip_task = [&query_context]() {
            return query_context.ShouldEarlyExit();
        };
    }

    return std::make_unique<SharedThreadPoolQueryExecutor>(
            GetOrCreateSharedQueryScheduler(thread_num),
            query_context.GetQueryId(),
            query_context,
            query_context.GetQueryPriority(),
            std::move(should_skip_task)
    );
}

std::shared_ptr<QueryScheduler> DexKit::GetOrCreateSharedQueryScheduler(uint32_t thread_num) const {
    auto normalized_thread_num = NormalizeThreadNum(thread_num);
    std::lock_guard lock(query_executor_mutex);
    if (!shared_query_pool_ || shared_query_pool_thread_num_ != normalized_thread_num) {
        shared_query_scheduler_.reset();
        shared_query_pool_ = std::make_shared<ThreadPool>(normalized_thread_num);
        shared_query_pool_thread_num_ = normalized_thread_num;
    }
    if (!shared_query_scheduler_) {
        shared_query_scheduler_ = std::make_shared<QueryScheduler>(shared_query_pool_, normalized_thread_num);
    }
    return shared_query_scheduler_;
}

static inline std::vector<uint32_t> ParseLogicalDexOffsets(const std::shared_ptr<MemMap> &image) {
    if (!image) return {};
    std::vector<uint32_t> offs;
    const uint8_t *base = image->data();
    const size_t n = image->len();

    size_t off = 0;
    while (true) {
        if (off + sizeof(dex::Header) > n) break;
        const auto header = reinterpret_cast<const dex::Header *>(base + off);
        uint32_t sz = header->file_size;
        if (sz < sizeof(dex::Header) || off + sz > n) break;
        offs.push_back(static_cast<uint32_t>(off));
        off += sz;
        if (off >= n) break;
    }
    return offs;
}

Error DexKit::AddDex(uint8_t *data, size_t size) {
    std::lock_guard lock(_mutex);
    auto image = std::make_shared<MemMap>(data, size);
    images.emplace_back(image);
    for (auto off : ParseLogicalDexOffsets(image)) {
        dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, image, off, this));
    }
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::unique_ptr<MemMap> dex_image) {
    std::lock_guard lock(_mutex);
    std::shared_ptr<MemMap> image = std::move(dex_image);
    images.emplace_back(image);
    for (auto off : ParseLogicalDexOffsets(image)) {
        dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, image, off, this));
    }
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::vector<std::unique_ptr<MemMap>> dex_images) {
    std::lock_guard lock(_mutex);
    const auto old_size = images.size();
    const auto new_size = old_size + dex_images.size();
    images.resize(new_size);
    std::vector<std::pair<std::shared_ptr<MemMap>, uint32_t>> add_items;
    for (size_t i = old_size, j = 0; i < new_size; i++, j++) {
        images[i] = std::move(dex_images[j]);
        for (auto off : ParseLogicalDexOffsets(images[i])) {
            add_items.emplace_back(images[i], off);
        }
    }
    const auto old_item_size = dex_items.size();
    dex_items.resize(old_item_size + add_items.size());
    {
        ThreadPool pool(NormalizeThreadNum(_thread_num.load(std::memory_order_acquire)));
        auto index = old_item_size;
        for (auto &[image, offset]: add_items) {
            pool.enqueue([this, &image, index, offset]() {
                dex_items[index] = std::make_unique<DexItem>(index, std::move(image), offset, this);
            });
            index++;
        }
    }
    dex_cnt += add_items.size();
    return Error::SUCCESS;
}

Error DexKit::AddZipPath(std::string_view apk_path, int unzip_thread_num) {
    auto map = MemMap(apk_path);
    if (!map.ok()) {
        return Error::FILE_NOT_FOUND;
    }
    auto zip_file = ZipArchive::Open(map);
    if (!zip_file) return Error::OPEN_ZIP_FILE_FAILED;
    std::vector<std::pair<int, const Entry *>> image_pairs;
    for (int idx = 1;; ++idx) {
        auto entry_name = "classes" + (idx == 1 ? std::string() : std::to_string(idx)) + ".dex";
        auto entry = zip_file->Find(entry_name);
        if (!entry) {
            break;
        }
        image_pairs.emplace_back(idx, entry);
    }
    const auto old_size = images.size();
    const auto new_size = old_size + image_pairs.size();
    images.resize(new_size);
    {
        auto thread_num = unzip_thread_num == 0
                          ? NormalizeThreadNum(_thread_num.load(std::memory_order_acquire))
                          : NormalizeThreadNum(static_cast<uint32_t>(unzip_thread_num));
        ThreadPool pool(thread_num);
        for (auto &dex_pair: image_pairs) {
            pool.enqueue([this, &dex_pair, old_size, &zip_file]() {
                auto dex_image = zip_file->GetUncompressData(*dex_pair.second);
                auto ptr = std::make_unique<MemMap>(std::move(dex_image));
                if (!ptr->ok()) {
                    return;
                }
                auto idx = old_size + dex_pair.first - 1;
                images[idx] = std::move(ptr);
            });
        }
    }
    std::vector<std::pair<std::shared_ptr<MemMap>, uint32_t>> add_items;
    for (auto i = old_size; i < new_size; i++) {
        for (auto off : ParseLogicalDexOffsets(images[i])) {
            add_items.emplace_back(images[i], off);
        }
    }
    const auto old_item_size = dex_items.size();
    dex_items.resize(old_item_size + add_items.size());
    {
        ThreadPool pool(NormalizeThreadNum(_thread_num.load(std::memory_order_acquire)));
        auto index = old_item_size;
        for (auto &[image, offset]: add_items) {
            pool.enqueue([this, &image, index, offset]() {
                dex_items[index] = std::make_unique<DexItem>(index, std::move(image), offset, this);
            });
            index++;
        }
    }
    dex_cnt += add_items.size();
    return Error::SUCCESS;
}

Error DexKit::ExportDexFile(std::string_view path) const {
    for (const auto &image: images) {
        std::string file_name(path);
        if (file_name.back() != '/') {
            file_name += '/';
        }
        file_name += "classes_" + std::to_string(image->len()) + ".dex";
        // write file
        FILE *fp = fopen(file_name.c_str(), "wb");
        if (fp == nullptr) {
            return Error::OPEN_FILE_FAILED;
        }
        int len = (int) image->len();
        int offset = 0;
        while (offset < len) {
            int size = std::min(WRITE_FILE_BLOCK_SIZE, len - offset);
            size_t write_size = fwrite(image->data() + offset, 1, size, fp);
            if (write_size != size) {
                fclose(fp);
                return Error::WRITE_FILE_INCOMPLETE;
            }
            offset += size;
            fflush(fp);
        }
        fclose(fp);
    }
    return Error::SUCCESS;
}

int DexKit::GetDexNum() const {
    return (int) dex_items.size();
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindClass(const schema::FindClass *query) {
    QueryContext query_context(
            QueryKind::FindClass,
#if DEXKIT_ENABLE_INTERNAL_METRICS
            query_metrics_enabled_.load(std::memory_order_acquire)
#else
            false
#endif
    );
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    auto has_composite_matcher = HasComposite(query->matcher());
    if (has_composite_matcher) {
        analyze_ret.declare_class.clear();
    }
    auto execution_guard = EnterQueryExecution(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    auto find_first = ConfigureFindFirstQuery(query_context, query);
    std::vector<ClassBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher() && !has_composite_matcher) {
        auto class_name = query->matcher()->class_name();
        if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
            auto declared_class_name = NormalizeDeclaredClassLookupName(class_name->value()->string_view());
            auto [dex, type_idx] = GetClassDeclaredPair(declared_class_name);
            if (dex) {
                fast_search_dex = dex;
                auto &class_set = dex_class_map[dex->GetDexId()];
                auto res = dex->FindClass(query, class_set, packageTrie, type_idx, query_context);
                result.insert(result.end(), res.begin(), res.end());
            }
        }
    }

    query_context.MarkPreprocessCompleted();

    if (fast_search_dex == nullptr) {
        auto executor = CreateQueryExecutor(query_context);
        std::vector<std::future<std::vector<ClassBean>>> futures;
        for (auto &dex_item: dex_items) {
            if (find_first && executor->ShouldSkipTask()) break;
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            if (has_composite_matcher || dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindClass(query, class_set, packageTrie, *executor, BATCH_SIZE / 2, query_context);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }
        executor->OnSubmissionComplete();
        query_context.MarkSubmissionCompleted();

        bool should_drain_pending_futures = false;
        size_t future_index = 0;
        for (; future_index < futures.size(); ++future_index) {
            auto vec = futures[future_index].get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (find_first) {
                should_drain_pending_futures = true;
                ++future_index;
                break;
            }
        }
        if (should_drain_pending_futures) {
            DrainRemainingFutures(futures, future_index);
        }
        query_context.MarkWorkersCompleted();
    } else {
        query_context.MarkSubmissionCompleted();
        query_context.MarkWorkersCompleted();
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::ClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateClassMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    query_context.MarkCompleted();
#if DEXKIT_ENABLE_INTERNAL_METRICS
    PublishLastQueryMetrics(query_context);
    RecordQueryMetrics(query_context);
#endif
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindMethod(const schema::FindMethod *query) {
    QueryContext query_context(
            QueryKind::FindMethod,
#if DEXKIT_ENABLE_INTERNAL_METRICS
            query_metrics_enabled_.load(std::memory_order_acquire)
#else
            false
#endif
    );
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_method_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_methods()) {
        for (auto encode_idx: *query->in_methods()) {
            dex_method_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    auto has_composite_matcher = HasComposite(query->matcher());
    if (has_composite_matcher) {
        analyze_ret.declare_class.clear();
    }
    auto execution_guard = EnterQueryExecution(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    auto find_first = ConfigureFindFirstQuery(query_context, query);
    std::vector<MethodBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher() && !has_composite_matcher) {
        auto declaring_class = query->matcher()->declaring_class();
        if (declaring_class) {
            auto class_name = declaring_class->class_name();
            if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
                auto declared_class_name = NormalizeDeclaredClassLookupName(class_name->value()->string_view());
                auto [dex, type_idx] = GetClassDeclaredPair(declared_class_name);
                if (dex) {
                    fast_search_dex = dex;
                    auto &class_set = dex_class_map[dex->GetDexId()];
                    auto &method_set = dex_method_map[dex->GetDexId()];
                    auto res = dex->FindMethod(query, class_set, method_set, packageTrie, type_idx, query_context);
                    result.insert(result.end(), res.begin(), res.end());
                }
            }
        }
    }

    query_context.MarkPreprocessCompleted();

    if (fast_search_dex == nullptr) {
        auto executor = CreateQueryExecutor(query_context);
        std::vector<std::future<std::vector<MethodBean>>> futures;
        for (auto &dex_item: dex_items) {
            if (find_first && executor->ShouldSkipTask()) break;
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            auto &method_set = dex_method_map[dex_item->GetDexId()];
            if (has_composite_matcher || dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindMethod(query, class_set, method_set, packageTrie, *executor, BATCH_SIZE, query_context);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }
        executor->OnSubmissionComplete();
        query_context.MarkSubmissionCompleted();

        bool should_drain_pending_futures = false;
        size_t future_index = 0;
        for (; future_index < futures.size(); ++future_index) {
            auto vec = futures[future_index].get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (find_first) {
                should_drain_pending_futures = true;
                ++future_index;
                break;
            }
        }
        if (should_drain_pending_futures) {
            DrainRemainingFutures(futures, future_index);
        }
        query_context.MarkWorkersCompleted();
    } else {
        query_context.MarkSubmissionCompleted();
        query_context.MarkWorkersCompleted();
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    std::set<std::string_view> declared_set;
    for (auto &bean: result) {
        if (declared_set.contains(bean.dex_descriptor)) {
            continue;
        }
        declared_set.emplace(bean.dex_descriptor);
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    query_context.MarkCompleted();
#if DEXKIT_ENABLE_INTERNAL_METRICS
    PublishLastQueryMetrics(query_context);
    RecordQueryMetrics(query_context);
#endif
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindField(const schema::FindField *query) {
    QueryContext query_context(
            QueryKind::FindField,
#if DEXKIT_ENABLE_INTERNAL_METRICS
            query_metrics_enabled_.load(std::memory_order_acquire)
#else
            false
#endif
    );
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_field_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_fields()) {
        for (auto encode_idx: *query->in_fields()) {
            dex_field_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    auto has_composite_matcher = HasComposite(query->matcher());
    if (has_composite_matcher) {
        analyze_ret.declare_class.clear();
    }
    auto execution_guard = EnterQueryExecution(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    auto find_first = ConfigureFindFirstQuery(query_context, query);
    std::vector<FieldBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher() && !has_composite_matcher) {
        auto declaring_class = query->matcher()->declaring_class();
        if (declaring_class) {
            auto class_name = declaring_class->class_name();
            if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
                auto declared_class_name = NormalizeDeclaredClassLookupName(class_name->value()->string_view());
                auto [dex, type_idx] = GetClassDeclaredPair(declared_class_name);
                if (dex) {
                    fast_search_dex = dex;
                    auto &class_set = dex_class_map[dex->GetDexId()];
                    auto &field_set = dex_field_map[dex->GetDexId()];
                    auto res = dex->FindField(query, class_set, field_set, packageTrie, type_idx, query_context);
                    result.insert(result.end(), res.begin(), res.end());
                }
            }
        }
    }

    query_context.MarkPreprocessCompleted();

    if (fast_search_dex == nullptr) {
        auto executor = CreateQueryExecutor(query_context);
        std::vector<std::future<std::vector<FieldBean>>> futures;
        for (auto &dex_item: dex_items) {
            if (find_first && executor->ShouldSkipTask()) break;
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            auto &field_set = dex_field_map[dex_item->GetDexId()];
            if (has_composite_matcher || dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindField(query, class_set, field_set, packageTrie, *executor, BATCH_SIZE, query_context);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }
        executor->OnSubmissionComplete();
        query_context.MarkSubmissionCompleted();

        bool should_drain_pending_futures = false;
        size_t future_index = 0;
        for (; future_index < futures.size(); ++future_index) {
            auto vec = futures[future_index].get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (find_first) {
                should_drain_pending_futures = true;
                ++future_index;
                break;
            }
        }
        if (should_drain_pending_futures) {
            DrainRemainingFutures(futures, future_index);
        }
        query_context.MarkWorkersCompleted();
    } else {
        query_context.MarkSubmissionCompleted();
        query_context.MarkWorkersCompleted();
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::FieldMeta>> offsets;
    std::set<std::string_view> declared_set;
    for (auto &bean: result) {
        if (declared_set.contains(bean.dex_descriptor)) {
            continue;
        }
        declared_set.emplace(bean.dex_descriptor);
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    query_context.MarkCompleted();
#if DEXKIT_ENABLE_INTERNAL_METRICS
    PublishLastQueryMetrics(query_context);
    RecordQueryMetrics(query_context);
#endif
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::BatchFindClassUsingStrings(const schema::BatchFindClassUsingStrings *query) {
    QueryContext query_context(
            QueryKind::BatchFindClassUsingStrings,
#if DEXKIT_ENABLE_INTERNAL_METRICS
            query_metrics_enabled_.load(std::memory_order_acquire)
#else
            false
#endif
    );
    auto execution_guard = EnterQueryExecution(kUsingString);
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    // build keywords trie
    std::vector<std::pair<std::string_view, bool>> keywords;
    phmap::flat_hash_map<std::string_view, schema::StringMatchType> match_type_map;
    std::map<std::string_view, std::set<std::string_view>> keywords_map;
    auto has_composite_matchers = HasCompositeBatchUsingStringsMatchers(query->matchers());
    if (!has_composite_matchers) {
        keywords_map = BuildBatchFindKeywordsMap(query->matchers(), keywords, match_type_map);
    }
    acdat::AhoCorasickDoubleArrayTrie<std::string_view> acTrie;
    if (!has_composite_matchers) {
        acdat::Builder<std::string_view>().Build(keywords, &acTrie);
    }

    std::map<std::string_view, std::vector<ClassBean>> find_result_map;
    // init find_result_map keys
    for (int i = 0; i < query->matchers()->size(); ++i) {
        auto matchers = query->matchers();
        for (int j = 0; j < matchers->size(); ++j) {
            find_result_map[matchers->Get(j)->union_key()->string_view()] = {};
        }
    }
    query_context.MarkPreprocessCompleted();
    auto executor = CreateQueryExecutor(query_context);
    std::vector<std::future<std::vector<BatchFindClassItemBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_map = dex_class_map[dex_item->GetDexId()];
        query_context.MarkTaskSubmitted();
        futures.push_back(SubmitQueryTask(*executor, [&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_map, &packageTrie,
                                                      &query_context]() {
            auto task_scope = query_context.TrackTaskExecution();
            auto result = dex_item->BatchFindClassUsingStrings(query, acTrie, keywords_map, match_type_map, class_map,
                                                               packageTrie, query_context);
            query_context.MarkTaskCompleted();
            return result;
        }));
    }
    executor->OnSubmissionComplete();
    query_context.MarkSubmissionCompleted();

    // fetch and merge result
    for (auto &f: futures) {
        auto items = f.get();
        for (auto &item: items) {
            auto &beans = find_result_map[item.union_key];
            beans.insert(beans.end(), item.classes.begin(), item.classes.end());
        }
    }
    query_context.MarkWorkersCompleted();

    std::vector<BatchFindClassItemBean> result;
    for (auto &[key, value]: find_result_map) {
        BatchFindClassItemBean bean;
        bean.union_key = key;
        bean.classes = value;
        result.emplace_back(bean);
    }

    auto fbb = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::BatchClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateBatchClassMeta(*fbb);
        fbb->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateBatchClassMetaArrayHolder(*fbb, fbb->CreateVector(offsets));
    fbb->Finish(array_holder);
    query_context.MarkCompleted();
#if DEXKIT_ENABLE_INTERNAL_METRICS
    PublishLastQueryMetrics(query_context);
    RecordQueryMetrics(query_context);
#endif
    return fbb;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query) {
    QueryContext query_context(
            QueryKind::BatchFindMethodUsingStrings,
#if DEXKIT_ENABLE_INTERNAL_METRICS
            query_metrics_enabled_.load(std::memory_order_acquire)
#else
            false
#endif
    );
    auto execution_guard = EnterQueryExecution(kUsingString);
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_method_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_methods()) {
        for (auto encode_idx: *query->in_methods()) {
            dex_method_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    // build keywords trie
    std::vector<std::pair<std::string_view, bool>> keywords;
    phmap::flat_hash_map<std::string_view, schema::StringMatchType> match_type_map;
    std::map<std::string_view, std::set<std::string_view>> keywords_map;
    auto has_composite_matchers = HasCompositeBatchUsingStringsMatchers(query->matchers());
    if (!has_composite_matchers) {
        keywords_map = BuildBatchFindKeywordsMap(query->matchers(), keywords, match_type_map);
    }
    acdat::AhoCorasickDoubleArrayTrie<std::string_view> acTrie;
    if (!has_composite_matchers) {
        acdat::Builder<std::string_view>().Build(keywords, &acTrie);
    }

    std::map<std::string_view, std::vector<MethodBean>> find_result_map;
    // init find_result_map keys
    for (int i = 0; i < query->matchers()->size(); ++i) {
        auto matchers = query->matchers();
        for (int j = 0; j < matchers->size(); ++j) {
            find_result_map[matchers->Get(j)->union_key()->string_view()] = {};
        }
    }
    query_context.MarkPreprocessCompleted();
    auto executor = CreateQueryExecutor(query_context);
    std::vector<std::future<std::vector<BatchFindMethodItemBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &method_set = dex_method_map[dex_item->GetDexId()];
        query_context.MarkTaskSubmitted();
        futures.push_back(SubmitQueryTask(*executor, [&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_set, &method_set,
                                                      &packageTrie, &query_context]() {
            auto task_scope = query_context.TrackTaskExecution();
            auto result = dex_item->BatchFindMethodUsingStrings(query, acTrie, keywords_map, match_type_map, class_set,
                                                                method_set, packageTrie, query_context);
            query_context.MarkTaskCompleted();
            return result;
        }));
    }
    executor->OnSubmissionComplete();
    query_context.MarkSubmissionCompleted();

    // fetch and merge result
    for (auto &f: futures) {
        auto items = f.get();
        for (auto &item: items) {
            auto &beans = find_result_map[item.union_key];
            beans.insert(beans.end(), item.methods.begin(), item.methods.end());
        }
    }
    query_context.MarkWorkersCompleted();

    std::vector<BatchFindMethodItemBean> result;
    for (auto &[key, value]: find_result_map) {
        BatchFindMethodItemBean bean;
        bean.union_key = key;
        bean.methods = value;
        result.emplace_back(bean);
    }

    auto fbb = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::BatchMethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateBatchMethodMeta(*fbb);
        fbb->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateBatchMethodMetaArrayHolder(*fbb, fbb->CreateVector(offsets));
    fbb->Finish(array_holder);
    query_context.MarkCompleted();
#if DEXKIT_ENABLE_INTERNAL_METRICS
    PublishLastQueryMetrics(query_context);
    RecordQueryMetrics(query_context);
#endif
    return fbb;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassData(const std::string_view descriptor) {
    auto execution_guard = EnterQueryExecution(0);
    auto [dex, type_id] = this->GetClassDeclaredPair(descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetClassBean(type_id);
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean.CreateClassMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodData(const std::string_view descriptor) {
    auto execution_guard = EnterQueryExecution(0);
    auto class_descriptor = descriptor.substr(0, descriptor.find("->"));
    auto [dex, type_id] = this->GetClassDeclaredPair(class_descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetMethodBean(type_id, descriptor);
    if (!bean.has_value()) {
        return nullptr;
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean->CreateMethodMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldData(const std::string_view descriptor) {
    auto execution_guard = EnterQueryExecution(0);
    auto class_descriptor = descriptor.substr(0, descriptor.find("->"));
    auto [dex, type_id] = this->GetClassDeclaredPair(class_descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetFieldBean(type_id, descriptor);
    if (!bean.has_value()) {
        return nullptr;
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean->CreateFieldMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassByIds(const std::vector<int64_t> &encode_ids) {
    auto execution_guard = EnterQueryExecution(0);
    std::vector<ClassBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto class_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetClassBean(class_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::ClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateClassMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodByIds(const std::vector<int64_t> &encode_ids) {
    auto execution_guard = EnterQueryExecution(0);
    std::vector<MethodBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto method_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetMethodBean(method_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldByIds(const std::vector<int64_t> &encode_ids) {
    auto execution_guard = EnterQueryExecution(0);
    std::vector<FieldBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto field_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetFieldBean(field_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::FieldMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassAnnotations(int64_t encode_class_id) {
    // By-id annotation metadata intentionally stays on EnterQueryExecution(0): these
    // getters are cold-path friendly and each DexItem accessor owns its own lazy fallback.
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_class_id >> 32;
    auto class_id = encode_class_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetClassAnnotationBeans(class_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldAnnotations(int64_t encode_field_id) {
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetFieldAnnotationBeans(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodAnnotations(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetMethodAnnotationBeans(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetParameterAnnotations(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetParameterAnnotationBeans(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMetaArrayHolder>> holder_offsets;
    for (auto &param_annotations: result) {
        std::vector<flatbuffers::Offset<schema::AnnotationMeta>> meta_offsets;
        for (auto &annotation: param_annotations) {
            auto res = annotation.CreateAnnotationMeta(*builder);
            builder->Finish(res);
            meta_offsets.emplace_back(res);
        }
        auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(meta_offsets));
        builder->Finish(array_holder);
        holder_offsets.emplace_back(array_holder);
    }
    auto array_holder = schema::CreateParametersAnnotationMetaArrayHoler(*builder, builder->CreateVector(holder_offsets));
    builder->Finish(array_holder);
    return builder;
}

std::optional<std::vector<std::optional<std::string_view>>>
DexKit::GetParameterNames(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetParameterNames(method_id);
}

std::vector<uint8_t>
DexKit::GetMethodOpCodes(int64_t encode_method_id) {
    // Same boundary as annotation getters: metadata reads keep their member-scoped lazy path
    // instead of forcing a bridge-level warm-up for all methods.
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetMethodOpCodes(method_id);
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetCallMethods(int64_t encode_method_id) {
    // Cross-ref metadata reads consume final shared indexes, so they must stay behind the
    // outer barrier instead of growing a second fallback implementation here.
    auto execution_guard = EnterQueryExecution(kCallerMethod | kMethodInvoking);

    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetCallMethods(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetInvokeMethods(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(kCallerMethod | kMethodInvoking);

    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetInvokeMethods(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::vector<std::string_view>
DexKit::GetUsingStrings(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(0);
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetUsingStrings(method_id);
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetUsingFields(int64_t encode_method_id) {
    auto execution_guard = EnterQueryExecution(kRwFieldMethod | kMethodUsingField);

    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetUsingFields(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::UsingFieldMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateUsingFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateUsingFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FieldGetMethods(int64_t encode_field_id) {
    auto execution_guard = EnterQueryExecution(kRwFieldMethod | kMethodUsingField);

    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->FieldGetMethods(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FieldPutMethods(int64_t encode_field_id) {
    auto execution_guard = EnterQueryExecution(kRwFieldMethod | kMethodUsingField);

    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->FieldPutMethods(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::pair<DexItem *, uint32_t> DexKit::GetClassDeclaredPair(std::string_view class_name) {
    auto find = this->class_declare_dex_map.find(class_name);
    if (find == this->class_declare_dex_map.end()) {
        return {nullptr, 0};
    }
    auto class_info = find->second;
    return {this->dex_items[class_info.first].get(), class_info.second};
}

DexItem *DexKit::GetDexItem(uint16_t dex_id) {
    return this->dex_items[dex_id].get();
}

void DexKit::PutDeclaredClass(std::string_view class_name, uint16_t dex_id, uint32_t type_idx) {
    std::lock_guard lock(this->_put_class_mutex);
    this->class_declare_dex_map[class_name] = {dex_id, type_idx};
}

uint32_t DexKit::BeginBuildCrossRefAggregates(uint32_t aggregate_flags) {
    DEXKIT_CHECK((aggregate_flags & ~(kCallerMethod | kRwFieldMethod)) == 0);
    std::unique_lock lock(cross_ref_aggregate_state_mutex);
    while (true) {
        auto ready_flags = cross_ref_aggregate_flag.load(std::memory_order_acquire);
        auto missing_flags = aggregate_flags & ~ready_flags;
        if (missing_flags == 0) {
            return 0;
        }
        if (cross_ref_aggregate_inflight_flags == 0) {
            cross_ref_aggregate_inflight_flags = missing_flags;
            return missing_flags;
        }
        cross_ref_aggregate_state_cv.wait(lock, [this] {
            return cross_ref_aggregate_inflight_flags == 0;
        });
    }
}

void DexKit::FinishBuildCrossRefAggregates(uint32_t aggregate_flags) {
    {
        std::lock_guard lock(cross_ref_aggregate_state_mutex);
        cross_ref_aggregate_flag.fetch_or(aggregate_flags, std::memory_order_release);
        cross_ref_aggregate_inflight_flags &= ~aggregate_flags;
    }
    cross_ref_aggregate_state_cv.notify_all();
}

void DexKit::WaitBuildCrossRefAggregates(uint32_t aggregate_flags) const {
    std::unique_lock lock(cross_ref_aggregate_state_mutex);
    cross_ref_aggregate_state_cv.wait(lock, [this, aggregate_flags] {
        auto ready_flags = cross_ref_aggregate_flag.load(std::memory_order_acquire);
        return (ready_flags & aggregate_flags) == aggregate_flags;
    });
}

void DexKit::BuildCrossRefAggregates(uint32_t aggregate_flags) {
    DEXKIT_CHECK((aggregate_flags & ~(kCallerMethod | kRwFieldMethod)) == 0);
    auto thread_num = NormalizeThreadNum(_thread_num.load(std::memory_order_acquire));

    if ((aggregate_flags & kCallerMethod) != 0) {
        struct MethodAggregateWorkItem {
            uint16_t source_dex_id;
            uint32_t source_method_idx;
            uint32_t target_method_idx;
        };

        std::vector<std::vector<MethodAggregateWorkItem>> work_items(dex_items.size());
        std::vector<phmap::flat_hash_map<uint32_t, size_t>> reserve_counts(dex_items.size());

        for (uint16_t source_dex_id = 0; source_dex_id < dex_items.size(); ++source_dex_id) {
            auto &source_dex = dex_items[source_dex_id];
            for (const auto &pending_item: source_dex->pending_aggregate_method_work_items) {
                auto &source_callers = source_dex->method_caller_ids[pending_item.source_method_idx];
                DEXKIT_CHECK(!source_callers.empty());
                work_items[pending_item.target_dex_id].push_back(MethodAggregateWorkItem{
                        .source_dex_id = source_dex_id,
                        .source_method_idx = pending_item.source_method_idx,
                        .target_method_idx = pending_item.target_method_idx
                });
                reserve_counts[pending_item.target_dex_id][pending_item.target_method_idx] += source_callers.size();
            }
        }

        auto aggregate_target_methods = [this, &work_items, &reserve_counts](uint16_t target_dex_id) {
            auto *target_dex = dex_items[target_dex_id].get();
            for (const auto &[target_method_idx, extra_count]: reserve_counts[target_dex_id]) {
                auto &target_callers = target_dex->method_caller_ids[target_method_idx];
                target_callers.reserve(target_callers.size() + extra_count);
            }
            for (const auto &work_item: work_items[target_dex_id]) {
                auto *source_dex = dex_items[work_item.source_dex_id].get();
                auto &source_callers = source_dex->method_caller_ids[work_item.source_method_idx];
                auto &target_callers = target_dex->method_caller_ids[work_item.target_method_idx];
                target_callers.insert(target_callers.end(), source_callers.begin(), source_callers.end());
                source_callers.clear();
                source_callers.shrink_to_fit();
            }
        };

        size_t target_count = 0;
        for (const auto &target_work_items: work_items) {
            if (!target_work_items.empty()) {
                ++target_count;
            }
        }
        if (target_count > 1 && thread_num > 1) {
            ThreadPool pool(std::min(static_cast<size_t>(thread_num), target_count));
            std::vector<std::future<void>> futures;
            futures.reserve(target_count);
            for (uint16_t target_dex_id = 0; target_dex_id < work_items.size(); ++target_dex_id) {
                if (work_items[target_dex_id].empty()) {
                    continue;
                }
                futures.emplace_back(pool.enqueue([aggregate_target_methods, target_dex_id]() {
                    aggregate_target_methods(target_dex_id);
                }));
            }
            for (auto &future: futures) {
                future.get();
            }
        } else {
            for (uint16_t target_dex_id = 0; target_dex_id < work_items.size(); ++target_dex_id) {
                if (work_items[target_dex_id].empty()) {
                    continue;
                }
                aggregate_target_methods(target_dex_id);
            }
        }

        for (auto &source_dex: dex_items) {
            source_dex->pending_aggregate_method_work_items.clear();
            source_dex->pending_aggregate_method_work_items.shrink_to_fit();
        }
    }

    if ((aggregate_flags & kRwFieldMethod) != 0) {
        struct FieldAggregateWorkItem {
            uint16_t source_dex_id;
            uint32_t source_field_idx;
            uint32_t target_field_idx;
        };

        std::vector<std::vector<FieldAggregateWorkItem>> work_items(dex_items.size());
        std::vector<phmap::flat_hash_map<uint32_t, size_t>> get_reserve_counts(dex_items.size());
        std::vector<phmap::flat_hash_map<uint32_t, size_t>> put_reserve_counts(dex_items.size());

        for (uint16_t source_dex_id = 0; source_dex_id < dex_items.size(); ++source_dex_id) {
            auto &source_dex = dex_items[source_dex_id];
            for (const auto &pending_item: source_dex->pending_aggregate_field_work_items) {
                auto &source_get_methods = source_dex->field_get_method_ids[pending_item.source_field_idx];
                auto &source_put_methods = source_dex->field_put_method_ids[pending_item.source_field_idx];
                DEXKIT_CHECK(!source_get_methods.empty() || !source_put_methods.empty());
                work_items[pending_item.target_dex_id].push_back(FieldAggregateWorkItem{
                        .source_dex_id = source_dex_id,
                        .source_field_idx = pending_item.source_field_idx,
                        .target_field_idx = pending_item.target_field_idx
                });
                get_reserve_counts[pending_item.target_dex_id][pending_item.target_field_idx] += source_get_methods.size();
                put_reserve_counts[pending_item.target_dex_id][pending_item.target_field_idx] += source_put_methods.size();
            }
        }

        auto aggregate_target_fields = [this, &work_items, &get_reserve_counts, &put_reserve_counts](uint16_t target_dex_id) {
            auto *target_dex = dex_items[target_dex_id].get();
            for (const auto &[target_field_idx, get_extra_count]: get_reserve_counts[target_dex_id]) {
                auto &target_get_methods = target_dex->field_get_method_ids[target_field_idx];
                target_get_methods.reserve(target_get_methods.size() + get_extra_count);
            }
            for (const auto &[target_field_idx, put_extra_count]: put_reserve_counts[target_dex_id]) {
                auto &target_put_methods = target_dex->field_put_method_ids[target_field_idx];
                target_put_methods.reserve(target_put_methods.size() + put_extra_count);
            }
            for (const auto &work_item: work_items[target_dex_id]) {
                auto *source_dex = dex_items[work_item.source_dex_id].get();

                auto &source_get_methods = source_dex->field_get_method_ids[work_item.source_field_idx];
                if (!source_get_methods.empty()) {
                    auto &target_get_methods = target_dex->field_get_method_ids[work_item.target_field_idx];
                    target_get_methods.insert(target_get_methods.end(), source_get_methods.begin(), source_get_methods.end());
                    source_get_methods.clear();
                    source_get_methods.shrink_to_fit();
                }

                auto &source_put_methods = source_dex->field_put_method_ids[work_item.source_field_idx];
                if (!source_put_methods.empty()) {
                    auto &target_put_methods = target_dex->field_put_method_ids[work_item.target_field_idx];
                    target_put_methods.insert(target_put_methods.end(), source_put_methods.begin(), source_put_methods.end());
                    source_put_methods.clear();
                    source_put_methods.shrink_to_fit();
                }
            }
        };

        size_t target_count = 0;
        for (const auto &target_work_items: work_items) {
            if (!target_work_items.empty()) {
                ++target_count;
            }
        }
        if (target_count > 1 && thread_num > 1) {
            ThreadPool pool(std::min(static_cast<size_t>(thread_num), target_count));
            std::vector<std::future<void>> futures;
            futures.reserve(target_count);
            for (uint16_t target_dex_id = 0; target_dex_id < work_items.size(); ++target_dex_id) {
                if (work_items[target_dex_id].empty()) {
                    continue;
                }
                futures.emplace_back(pool.enqueue([aggregate_target_fields, target_dex_id]() {
                    aggregate_target_fields(target_dex_id);
                }));
            }
            for (auto &future: futures) {
                future.get();
            }
        } else {
            for (uint16_t target_dex_id = 0; target_dex_id < work_items.size(); ++target_dex_id) {
                if (work_items[target_dex_id].empty()) {
                    continue;
                }
                aggregate_target_fields(target_dex_id);
            }
        }

        for (auto &source_dex: dex_items) {
            source_dex->pending_aggregate_field_work_items.clear();
            source_dex->pending_aggregate_field_work_items.shrink_to_fit();
        }
    }
}

void DexKit::InitDexCache(uint32_t init_flags) {
    uint32_t cross_ref_flags = init_flags & (kCallerMethod | kRwFieldMethod);
    auto thread_num = NormalizeThreadNum(_thread_num.load(std::memory_order_acquire));
    std::vector<std::pair<DexItem *, uint32_t>> init_jobs;
    init_jobs.reserve(dex_items.size());
    for (auto &dex_item: dex_items) {
        auto claimed_flags = dex_item->BeginInitCache(init_flags);
        if (claimed_flags != 0) {
            init_jobs.emplace_back(dex_item.get(), claimed_flags);
        }
    }

    if (!init_jobs.empty()) {
        ThreadPool pool(std::min(static_cast<size_t>(thread_num), init_jobs.size()));
        for (auto &[dex_item, claimed_flags]: init_jobs) {
            pool.enqueue([dex_item, claimed_flags]() {
                dex_item->InitCache(claimed_flags);
                dex_item->FinishInitCache(claimed_flags);
            });
        }
    }
    for (auto &dex_item: dex_items) {
        dex_item->WaitInitCache(init_flags);
    }

    if (cross_ref_flags == 0) {
        return;
    }

    std::vector<std::pair<DexItem *, uint32_t>> cross_ref_jobs;
    cross_ref_jobs.reserve(dex_items.size());
    for (auto &dex_item: dex_items) {
        auto claimed_flags = dex_item->BeginPutCrossRef(cross_ref_flags);
        if (claimed_flags != 0) {
            cross_ref_jobs.emplace_back(dex_item.get(), claimed_flags);
        }
    }
    if (!cross_ref_jobs.empty()) {
        ThreadPool pool(std::min(static_cast<size_t>(thread_num), cross_ref_jobs.size()));
        for (auto &[dex_item, claimed_flags]: cross_ref_jobs) {
            pool.enqueue([dex_item, claimed_flags]() {
                dex_item->PutCrossRef(claimed_flags);
                dex_item->FinishPutCrossRef(claimed_flags);
            });
        }
    }
    for (auto &dex_item: dex_items) {
        dex_item->WaitPutCrossRef(cross_ref_flags);
    }

    auto aggregate_flags = BeginBuildCrossRefAggregates(cross_ref_flags);
    if (aggregate_flags != 0) {
        BuildCrossRefAggregates(aggregate_flags);
        FinishBuildCrossRefAggregates(aggregate_flags);
    }
    WaitBuildCrossRefAggregates(cross_ref_flags);
}

void DexKit::BuildPackagesMatchTrie(
        const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *search_packages,
        const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *exclude_packages,
        const bool ignore_packages_case,
        trie::PackageTrie &trie
) {
    std::vector<std::string> packages;
    if (search_packages) {
        for (auto i = 0; i < search_packages->size(); ++i) {
            auto package = search_packages->Get(i);
            std::string package_str(package->string_view());
            std::replace(package_str.begin(), package_str.end(), '.', '/');
            if (package_str[0] != 'L') {
                package_str = "L" + package_str; // NOLINT
            }
            if (package_str.back() != '/') {
                package_str += '/';
            }
            trie.insert(package_str, true, ignore_packages_case);
            packages.emplace_back(std::move(package_str));
        }
    }
    if (exclude_packages) {
        for (auto i = 0; i < exclude_packages->size(); ++i) {
            auto package = exclude_packages->Get(i);
            std::string package_str(package->string_view());
            std::replace(package_str.begin(), package_str.end(), '.', '/');
            if (package_str[0] != 'L') {
                package_str = "L" + package_str; // NOLINT
            }
            if (package_str.back() != '/') {
                package_str += '/';
            }
            trie.insert(package_str, false, ignore_packages_case);
            packages.emplace_back(std::move(package_str));
        }
    }
}

std::map<std::string_view, std::set<std::string_view>>
DexKit::BuildBatchFindKeywordsMap(
        const flatbuffers::Vector<flatbuffers::Offset<dexkit::schema::BatchUsingStringsMatcher>> *matchers,
        std::vector<std::pair<std::string_view, bool>> &keywords,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    DEXKIT_CHECK(!HasCompositeBatchUsingStringsMatchers(matchers));
    std::map<std::string_view, std::set<std::string_view>> keywords_map;
    for (int i = 0; i < matchers->size(); ++i) {
        auto matcher = matchers->Get(i);
        auto union_key = matcher->union_key()->string_view();
        for (int j = 0; j < matcher->using_strings()->size(); ++j) {
            auto string_matcher = matcher->using_strings()->Get(j);
            auto value = string_matcher->value()->string_view();
            auto type = string_matcher->match_type();
            auto ignore_case = string_matcher->ignore_case();
            if (type == schema::StringMatchType::SimilarRegex) {
                type = schema::StringMatchType::Contains;
                int l = 0, r = (int) value.size();
                if (value.starts_with('^')) {
                    l = 1;
                    type = schema::StringMatchType::StartWith;
                }
                if (value.ends_with('$')) {
                    r = (int) value.size() - 1;
                    if (type == schema::StringMatchType::StartWith) {
                        type = schema::StringMatchType::Equal;
                    } else {
                        type = schema::StringMatchType::EndWith;
                    }
                }
                value = value.substr(l, r - l);
            }
            keywords_map[union_key].insert(value);
            keywords.emplace_back(value, ignore_case);
            match_type_map[value] = type;
        }
    }
    return keywords_map;
}

} // namespace dexkit
