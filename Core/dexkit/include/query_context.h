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

#include <atomic>
#include <chrono>
#include <cstdint>
#include <functional>
#include <memory>
#include <mutex>
#include <utility>
#include <vector>

#include "internal_metrics_config.h"
#include "parallel_hashmap/phmap.h"

namespace dexkit {

enum class QueryKind : uint8_t {
    FindClass,
    FindMethod,
    FindField,
    BatchFindClassUsingStrings,
    BatchFindMethodUsingStrings,
};

enum class QueryPriority : uint8_t {
    Normal = 0,
    LatencySensitive = 1,
};

struct QueryMetrics {
    std::chrono::steady_clock::time_point created_at = std::chrono::steady_clock::now();
    std::atomic<uint32_t> submitted_tasks = 0;
    std::atomic<uint32_t> dispatched_tasks = 0;
    std::atomic<uint32_t> base_dispatched_tasks = 0;
    std::atomic<uint32_t> bonus_dispatched_tasks = 0;
    std::atomic<uint32_t> completed_tasks = 0;
    std::atomic<uint32_t> max_in_flight = 0;
    std::atomic<uint32_t> max_query_share_count = 0;
    std::atomic<int64_t> first_dispatch_delay_ns = -1;
    std::atomic<int64_t> first_bonus_dispatch_delay_ns = -1;
    std::atomic<int64_t> first_task_start_delay_ns = -1;
    std::atomic<int64_t> last_task_finish_delay_ns = -1;
    std::atomic<int64_t> task_runtime_total_ns = 0;
    std::atomic<int64_t> task_runtime_max_ns = 0;
    std::atomic<int64_t> preprocess_completed_ns = -1;
    std::atomic<int64_t> submission_completed_ns = -1;
    std::atomic<int64_t> workers_completed_ns = -1;
    std::atomic<int64_t> completed_ns = -1;
};

struct QueryMetricsSnapshot {
    uint32_t submitted_tasks = 0;
    uint32_t dispatched_tasks = 0;
    uint32_t base_dispatched_tasks = 0;
    uint32_t bonus_dispatched_tasks = 0;
    uint32_t completed_tasks = 0;
    uint32_t max_in_flight = 0;
    uint32_t max_query_share_count = 0;
    int64_t first_dispatch_delay_ns = -1;
    int64_t first_bonus_dispatch_delay_ns = -1;
    int64_t first_task_start_delay_ns = -1;
    int64_t last_task_finish_delay_ns = -1;
    int64_t task_runtime_total_ns = 0;
    int64_t task_runtime_max_ns = 0;
    int64_t preprocess_completed_ns = -1;
    int64_t submission_completed_ns = -1;
    int64_t workers_completed_ns = -1;
    int64_t completed_ns = -1;
};

struct QueryMetricsRecord {
    QueryKind kind = QueryKind::FindClass;
    QueryPriority priority = QueryPriority::Normal;
    QueryMetricsSnapshot metrics{};
};

struct QueryMetricsHistorySnapshot {
    uint64_t dropped_records = 0;
    std::vector<QueryMetricsRecord> records;
};

struct QueryCacheKey {
    uint8_t scope = 0;
    std::uintptr_t key = 0;

    [[nodiscard]] bool operator==(const QueryCacheKey &other) const {
        return scope == other.scope && key == other.key;
    }
};

struct QueryCacheKeyHash {
    [[nodiscard]] size_t operator()(const QueryCacheKey &value) const {
        auto h1 = std::hash<uint8_t>{}(value.scope);
        auto h2 = std::hash<std::uintptr_t>{}(value.key);
        return h1 ^ (h2 + 0x9e3779b97f4a7c15ULL + (h1 << 6U) + (h1 >> 2U));
    }
};

class QueryContext {
public:
    class TaskExecutionScope {
    public:
        TaskExecutionScope() = default;

        explicit TaskExecutionScope(QueryContext &query_context)
                : query_context_(&query_context), start_time_(std::chrono::steady_clock::now()) {
            query_context_->MarkTaskExecutionStarted(start_time_);
        }

        TaskExecutionScope(const TaskExecutionScope &) = delete;
        TaskExecutionScope &operator=(const TaskExecutionScope &) = delete;

        TaskExecutionScope(TaskExecutionScope &&other) noexcept
                : query_context_(other.query_context_), start_time_(other.start_time_) {
            other.query_context_ = nullptr;
        }

        TaskExecutionScope &operator=(TaskExecutionScope &&other) = delete;

        ~TaskExecutionScope() {
            if (query_context_ != nullptr) {
                query_context_->MarkTaskExecutionFinished(start_time_, std::chrono::steady_clock::now());
            }
        }

    private:
        QueryContext *query_context_ = nullptr;
        std::chrono::steady_clock::time_point start_time_{};
    };

    class ScopedBinding {
    public:
        explicit ScopedBinding(QueryContext &query_context)
                : previous_(current_), current_query_(&query_context) {
            current_ = &query_context;
        }

        ScopedBinding(const ScopedBinding &) = delete;
        ScopedBinding &operator=(const ScopedBinding &) = delete;

        ScopedBinding(ScopedBinding &&other) noexcept
                : previous_(other.previous_), current_query_(other.current_query_) {
            other.current_query_ = nullptr;
        }

        ScopedBinding &operator=(ScopedBinding &&other) = delete;

        ~ScopedBinding() {
            if (current_query_ != nullptr) {
                current_ = previous_;
            }
        }

    private:
        QueryContext *previous_ = nullptr;
        QueryContext *current_query_ = nullptr;
    };

    explicit QueryContext(QueryKind kind, bool metrics_enabled = false)
            : kind_(kind), query_id_(NextQueryId())
#if DEXKIT_ENABLE_INTERNAL_METRICS
            , metrics_enabled_(metrics_enabled)
#endif
    {
#if !DEXKIT_ENABLE_INTERNAL_METRICS
        (void) metrics_enabled;
#endif
    }

    ~QueryContext() {
        ClearMatcherCaches();
    }

    [[nodiscard]] uint64_t GetQueryId() const {
        return query_id_;
    }

    [[nodiscard]] QueryKind GetKind() const {
        return kind_;
    }

    void SetQueryPriority(QueryPriority priority) {
        priority_ = priority;
    }

    [[nodiscard]] QueryPriority GetQueryPriority() const {
        return priority_;
    }

    void EnableEarlyExit() {
        early_exit_enabled_.store(true, std::memory_order_relaxed);
    }

    [[nodiscard]] bool IsEarlyExitEnabled() const {
        return early_exit_enabled_.load(std::memory_order_relaxed);
    }

    [[nodiscard]] bool RequestEarlyExit() {
        return !early_exit_.exchange(true, std::memory_order_acq_rel);
    }

    [[nodiscard]] bool ShouldEarlyExit() const {
        return early_exit_.load(std::memory_order_acquire);
    }

    [[nodiscard]] bool AreMetricsEnabled() const {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        return metrics_enabled_;
#else
        return false;
#endif
    }

    void MarkTaskSubmitted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        metrics_.submitted_tasks.fetch_add(1, std::memory_order_relaxed);
#endif
    }

    void MarkTaskCompleted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        metrics_.completed_tasks.fetch_add(1, std::memory_order_relaxed);
#endif
    }

    void MarkPreprocessCompleted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        StoreTimestampIfUnset(metrics_.preprocess_completed_ns, RelativeNowNs());
#endif
    }

    void MarkSubmissionCompleted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        StoreTimestampIfUnset(metrics_.submission_completed_ns, RelativeNowNs());
#endif
    }

    void MarkWorkersCompleted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        StoreTimestampIfUnset(metrics_.workers_completed_ns, RelativeNowNs());
#endif
    }

    void MarkCompleted() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        StoreTimestampIfUnset(metrics_.completed_ns, RelativeNowNs());
#endif
    }

    void MarkTaskDispatched(bool used_bonus_dispatch, uint32_t query_in_flight, uint32_t query_share_count) {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) return;
        metrics_.dispatched_tasks.fetch_add(1, std::memory_order_relaxed);
        if (used_bonus_dispatch) {
            metrics_.bonus_dispatched_tasks.fetch_add(1, std::memory_order_relaxed);
        } else {
            metrics_.base_dispatched_tasks.fetch_add(1, std::memory_order_relaxed);
        }
        UpdateMax(metrics_.max_in_flight, query_in_flight);
        UpdateMax(metrics_.max_query_share_count, query_share_count);

        int64_t expected = -1;
        auto first_dispatch_delay_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
                std::chrono::steady_clock::now() - metrics_.created_at
        ).count();
        (void) metrics_.first_dispatch_delay_ns.compare_exchange_strong(
                expected,
                first_dispatch_delay_ns,
                std::memory_order_acq_rel,
                std::memory_order_relaxed
        );
        if (used_bonus_dispatch) {
            StoreTimestampIfUnset(metrics_.first_bonus_dispatch_delay_ns, first_dispatch_delay_ns);
        }
#else
        (void) used_bonus_dispatch;
        (void) query_in_flight;
        (void) query_share_count;
#endif
    }

#if DEXKIT_ENABLE_INTERNAL_METRICS
    [[nodiscard]] const QueryMetrics &GetMetrics() const {
        return metrics_;
    }
#endif

    [[nodiscard]] QueryMetricsSnapshot SnapshotMetrics() const {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) {
            return {};
        }
        QueryMetricsSnapshot snapshot;
        snapshot.submitted_tasks = metrics_.submitted_tasks.load(std::memory_order_relaxed);
        snapshot.dispatched_tasks = metrics_.dispatched_tasks.load(std::memory_order_relaxed);
        snapshot.base_dispatched_tasks = metrics_.base_dispatched_tasks.load(std::memory_order_relaxed);
        snapshot.bonus_dispatched_tasks = metrics_.bonus_dispatched_tasks.load(std::memory_order_relaxed);
        snapshot.completed_tasks = metrics_.completed_tasks.load(std::memory_order_relaxed);
        snapshot.max_in_flight = metrics_.max_in_flight.load(std::memory_order_relaxed);
        snapshot.max_query_share_count = metrics_.max_query_share_count.load(std::memory_order_relaxed);
        snapshot.first_dispatch_delay_ns = metrics_.first_dispatch_delay_ns.load(std::memory_order_relaxed);
        snapshot.first_bonus_dispatch_delay_ns = metrics_.first_bonus_dispatch_delay_ns.load(std::memory_order_relaxed);
        snapshot.first_task_start_delay_ns = metrics_.first_task_start_delay_ns.load(std::memory_order_relaxed);
        snapshot.last_task_finish_delay_ns = metrics_.last_task_finish_delay_ns.load(std::memory_order_relaxed);
        snapshot.task_runtime_total_ns = metrics_.task_runtime_total_ns.load(std::memory_order_relaxed);
        snapshot.task_runtime_max_ns = metrics_.task_runtime_max_ns.load(std::memory_order_relaxed);
        snapshot.preprocess_completed_ns = metrics_.preprocess_completed_ns.load(std::memory_order_relaxed);
        snapshot.submission_completed_ns = metrics_.submission_completed_ns.load(std::memory_order_relaxed);
        snapshot.workers_completed_ns = metrics_.workers_completed_ns.load(std::memory_order_relaxed);
        snapshot.completed_ns = metrics_.completed_ns.load(std::memory_order_relaxed);
        return snapshot;
#else
        return {};
#endif
    }

    void PublishSnapshotToCurrentThread() const {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        last_query_metrics_snapshot_ = SnapshotMetrics();
#endif
    }

    [[nodiscard]] static QueryMetricsSnapshot LastQueryMetricsSnapshot() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        return last_query_metrics_snapshot_;
#else
        return {};
#endif
    }

    [[nodiscard]] TaskExecutionScope TrackTaskExecution() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        if (!metrics_enabled_) {
            return {};
        }
        return TaskExecutionScope(*this);
#else
        return {};
#endif
    }

    [[nodiscard]] ScopedBinding BindToCurrentThread() {
        return ScopedBinding(*this);
    }

    [[nodiscard]] static QueryContext *Current() {
        return current_;
    }

    template<typename T, typename Factory>
    T *GetOrCreateMatcherCache(uint8_t scope, std::uintptr_t key, Factory &&factory) {
        auto cache_key = QueryCacheKey{scope, key};
        std::lock_guard lock(matcher_cache_mutex_);
        auto it = matcher_cache_.find(cache_key);
        if (it != matcher_cache_.end()) {
            return reinterpret_cast<T *>(it->second.value);
        }
        auto *value = new T(std::forward<Factory>(factory)());
        matcher_cache_.emplace(cache_key, MatcherCacheOwnership{
                .value = value,
                .deleter = [](void *ptr) {
                    delete reinterpret_cast<T *>(ptr);
                },
        });
        return value;
    }

private:
    struct MatcherCacheOwnership {
        void *value = nullptr;
        void (*deleter)(void *) = nullptr;
    };

    void ClearMatcherCaches() {
        phmap::flat_hash_map<QueryCacheKey, MatcherCacheOwnership, QueryCacheKeyHash> caches;
        {
            std::lock_guard lock(matcher_cache_mutex_);
            caches.swap(matcher_cache_);
        }
        for (auto &[cache_key, ownership]: caches) {
            (void) cache_key;
            if (ownership.value != nullptr && ownership.deleter != nullptr) {
                ownership.deleter(ownership.value);
            }
        }
    }

    [[nodiscard]] int64_t RelativeNs(std::chrono::steady_clock::time_point time_point) const {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        return std::chrono::duration_cast<std::chrono::nanoseconds>(time_point - metrics_.created_at).count();
#else
        (void) time_point;
        return 0;
#endif
    }

    [[nodiscard]] int64_t RelativeNowNs() const {
        return RelativeNs(std::chrono::steady_clock::now());
    }

    void MarkTaskExecutionStarted(std::chrono::steady_clock::time_point start_time) {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        UpdateMinOrSet(metrics_.first_task_start_delay_ns, RelativeNs(start_time));
#else
        (void) start_time;
#endif
    }

    void MarkTaskExecutionFinished(
            std::chrono::steady_clock::time_point start_time,
            std::chrono::steady_clock::time_point finish_time
    ) {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        auto runtime_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(finish_time - start_time).count();
        metrics_.task_runtime_total_ns.fetch_add(runtime_ns, std::memory_order_relaxed);
        UpdateMax(metrics_.task_runtime_max_ns, runtime_ns);
        UpdateMax(metrics_.last_task_finish_delay_ns, RelativeNs(finish_time));
#else
        (void) start_time;
        (void) finish_time;
#endif
    }

    static void UpdateMax(std::atomic<uint32_t> &target, uint32_t value) {
        auto current = target.load(std::memory_order_relaxed);
        while (current < value &&
               !target.compare_exchange_weak(current, value, std::memory_order_relaxed, std::memory_order_relaxed)) {}
    }

    static void UpdateMax(std::atomic<int64_t> &target, int64_t value) {
        auto current = target.load(std::memory_order_relaxed);
        while (current < value &&
               !target.compare_exchange_weak(current, value, std::memory_order_relaxed, std::memory_order_relaxed)) {}
    }

    static void UpdateMinOrSet(std::atomic<int64_t> &target, int64_t value) {
        auto current = target.load(std::memory_order_relaxed);
        while ((current < 0 || value < current) &&
               !target.compare_exchange_weak(current, value, std::memory_order_relaxed, std::memory_order_relaxed)) {}
    }

    static void StoreTimestampIfUnset(std::atomic<int64_t> &target, int64_t value) {
        auto expected = static_cast<int64_t>(-1);
        (void) target.compare_exchange_strong(
                expected,
                value,
                std::memory_order_acq_rel,
                std::memory_order_relaxed
        );
    }

    static uint64_t NextQueryId() {
        return next_query_id_.fetch_add(1, std::memory_order_relaxed);
    }

    QueryKind kind_;
    uint64_t query_id_;
    QueryPriority priority_ = QueryPriority::Normal;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    bool metrics_enabled_ = false;
#endif
    std::atomic<bool> early_exit_enabled_ = false;
    std::atomic<bool> early_exit_ = false;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    QueryMetrics metrics_{};
#endif
    std::mutex matcher_cache_mutex_;
    phmap::flat_hash_map<QueryCacheKey, MatcherCacheOwnership, QueryCacheKeyHash> matcher_cache_;

    inline static std::atomic<uint64_t> next_query_id_ = 1;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    inline static thread_local QueryMetricsSnapshot last_query_metrics_snapshot_{};
#endif
    inline static thread_local QueryContext *current_ = nullptr;
};

} // namespace dexkit
