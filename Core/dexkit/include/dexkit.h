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

#pragma once

#include <string_view>
#include <thread>
#include <vector>
#include <deque>
#include <atomic>
#include <condition_variable>
#include <memory>

#include "flatbuffers/flatbuffers.h"
#include "zip_archive.h"
#include "dexkit_error.h"
#include "dex_item.h"
#include "package_trie.h"
#include "analyze.h"
#include "query_executor.h"

#define BATCH_SIZE 1000

namespace dexkit {

class DexItem;

class DexKit {
public:
    class QueryExecutionGuard {
    public:
        explicit QueryExecutionGuard(DexKit *owner) : owner_(owner) {}
        QueryExecutionGuard(const QueryExecutionGuard &) = delete;
        QueryExecutionGuard &operator=(const QueryExecutionGuard &) = delete;
        QueryExecutionGuard(QueryExecutionGuard &&other) noexcept : owner_(other.owner_) {
            other.owner_ = nullptr;
        }
        QueryExecutionGuard &operator=(QueryExecutionGuard &&other) = delete;
        ~QueryExecutionGuard();

    private:
        DexKit *owner_ = nullptr;
    };


    explicit DexKit() = default;
    explicit DexKit(std::string_view apk_path, int unzip_thread_num = 0);
    ~DexKit() = default;

    void SetThreadNum(int num);
    void SetMaxConcurrentQueries(uint32_t max_concurrent_queries);
#if DEXKIT_ENABLE_INTERNAL_METRICS
    void SetQueryMetricsEnabled(bool enabled);
    [[nodiscard]] QuerySchedulerMetricsSnapshot GetQuerySchedulerMetricsSnapshot() const;
    void ResetQuerySchedulerMetrics() const;
    [[nodiscard]] static QueryMetricsSnapshot GetLastQueryMetricsSnapshot();
    [[nodiscard]] QueryMetricsHistorySnapshot GetQueryMetricsHistorySnapshot() const;
    void ResetQueryMetricsHistory();
#endif
    Error InitFullCache();
    Error AddDex(uint8_t *data, size_t size);
    Error AddImage(std::unique_ptr<MemMap> dex_image);
    Error AddImage(std::vector<std::unique_ptr<MemMap>> dex_images);
    Error AddZipPath(std::string_view apk_path, int unzip_thread_num = 0);
    [[nodiscard]] Error ExportDexFile(std::string_view path) const;
    [[nodiscard]] int GetDexNum() const;

    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindClass(const schema::FindClass *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindMethod(const schema::FindMethod *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindField(const schema::FindField *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> BatchFindClassUsingStrings(const schema::BatchFindClassUsingStrings *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query);

    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassData(std::string_view descriptor);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodData(std::string_view descriptor);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldData(std::string_view descriptor);

    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassAnnotations(int64_t encode_class_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldAnnotations(int64_t encode_field_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodAnnotations(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetParameterAnnotations(int64_t encode_method_id);
    std::optional<std::vector<std::optional<std::string_view>>> GetParameterNames(int64_t encode_method_id);
    std::vector<uint8_t> GetMethodOpCodes(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetCallMethods(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetInvokeMethods(int64_t encode_method_id);
    std::vector<std::string_view> GetUsingStrings(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetUsingFields(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldGetMethods(int64_t encode_field_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldPutMethods(int64_t encode_field_id);

    std::pair<DexItem *, uint32_t> GetClassDeclaredPair(std::string_view class_name);
    DexItem *GetDexItem(uint16_t dex_id);
    void PutDeclaredClass(std::string_view class_name, uint16_t dex_id, uint32_t type_idx);

private:
    std::mutex _mutex;
    std::shared_mutex _put_class_mutex;
    mutable std::mutex query_execution_mutex;
    mutable std::condition_variable query_execution_cv;
    mutable std::mutex query_executor_mutex;
    uint32_t active_query_count = 0;
    uint32_t pending_warmup_flags = 0;
    bool warmup_inflight = false;
    uint64_t next_shared_pool_admission_ticket_ = 1;
    std::deque<uint64_t> shared_pool_admission_wait_queue_;
    std::atomic<uint32_t> dex_cnt = 0;
    std::atomic<uint32_t> _thread_num = std::thread::hardware_concurrency();
    std::atomic<uint32_t> max_concurrent_queries_ = 0;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    std::atomic<bool> query_metrics_enabled_ = false;
#endif
    mutable std::shared_ptr<ThreadPool> shared_query_pool_;
    mutable std::shared_ptr<QueryScheduler> shared_query_scheduler_;
    mutable uint32_t shared_query_pool_thread_num_ = 0;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    mutable std::mutex query_metrics_history_mutex;
    mutable std::deque<QueryMetricsRecord> query_metrics_history_;
    uint64_t dropped_query_metrics_history_records_ = 0;
#endif
    std::vector<std::shared_ptr<MemMap>> images;
    std::vector<std::unique_ptr<DexItem>> dex_items;
    phmap::flat_hash_map<std::string_view, std::pair<uint16_t /*dex_id*/, uint32_t /*type_idx*/>> class_declare_dex_map;
    std::atomic<uint32_t> cross_ref_aggregate_flag = 0;
    mutable std::mutex cross_ref_aggregate_state_mutex;
    mutable std::condition_variable cross_ref_aggregate_state_cv;
    uint32_t cross_ref_aggregate_inflight_flags = 0;

    void InitDexCache(uint32_t init_flags);
    [[nodiscard]] QueryExecutionGuard EnterQueryExecution(uint32_t required_flags);
    void LeaveQueryExecution();
    [[nodiscard]] bool NeedWarmUp(uint32_t init_flags) const;
    [[nodiscard]] std::shared_ptr<QueryScheduler> GetOrCreateSharedQueryScheduler(uint32_t thread_num) const;
    [[nodiscard]] std::unique_ptr<IQueryExecutor> CreateQueryExecutor(QueryContext &query_context) const;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    void RecordQueryMetrics(const QueryContext &query_context);
#endif
    uint32_t BeginBuildCrossRefAggregates(uint32_t aggregate_flags);
    void FinishBuildCrossRefAggregates(uint32_t aggregate_flags);
    void WaitBuildCrossRefAggregates(uint32_t aggregate_flags) const;
    void BuildCrossRefAggregates(uint32_t aggregate_flags);

#if DEXKIT_ENABLE_INTERNAL_METRICS
    static constexpr size_t kQueryMetricsHistoryCapacity = 256;
#endif

    static void BuildPackagesMatchTrie(
            const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *search_packages,
            const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *exclude_packages,
            bool ignore_package_case,
            trie::PackageTrie &trie
    );

    static std::map<std::string_view, std::set<std::string_view>>
    BuildBatchFindKeywordsMap(
            const flatbuffers::Vector<flatbuffers::Offset<dexkit::schema::BatchUsingStringsMatcher>> *matchers,
            std::vector<std::pair<std::string_view, bool>> &keywords,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );
};

} // namespace dexkit
