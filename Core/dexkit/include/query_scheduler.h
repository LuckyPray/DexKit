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

#include <algorithm>
#include <cstdint>
#include <deque>
#include <functional>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <utility>
#include <vector>

#include "ThreadPool.h"
#include "internal_metrics_config.h"
#include "query_context.h"

namespace dexkit {

struct QuerySchedulerMetricsSnapshot {
    size_t share_count_syncs = 0;
    size_t share_count_changes = 0;
    size_t budget_rebalances = 0;
    size_t runnable_queue_rebuilds = 0;
    size_t refill_rounds = 0;
    size_t dispatched_tasks = 0;
    size_t base_dispatched_tasks = 0;
    size_t bonus_dispatched_tasks = 0;
    size_t max_total_in_flight = 0;
    size_t max_visible_query_share_count = 0;
    size_t max_runnable_queue_size = 0;
};

class QueryScheduler final : public std::enable_shared_from_this<QueryScheduler> {
public:
    QueryScheduler(std::shared_ptr<ThreadPool> pool, size_t worker_count)
            : pool_(std::move(pool)), worker_count_(std::max<size_t>(1, worker_count)) {}

    void AttachQuery(uint64_t query_id, QueryPriority priority, QueryContext *query_context) {
        std::lock_guard lock(mutex_);
        auto &slot = query_slots_[query_id];
        slot.query_id = query_id;
        slot.priority = priority;
        slot.query_context = query_context;
        slot.attached = true;
    }

    [[nodiscard]] QuerySchedulerMetricsSnapshot GetMetricsSnapshot() const {
        std::lock_guard lock(mutex_);
        QuerySchedulerMetricsSnapshot snapshot;
#if DEXKIT_ENABLE_INTERNAL_METRICS
        snapshot.share_count_syncs = metrics_.share_count_syncs;
        snapshot.share_count_changes = metrics_.share_count_changes;
        snapshot.budget_rebalances = metrics_.budget_rebalances;
        snapshot.runnable_queue_rebuilds = metrics_.runnable_queue_rebuilds;
        snapshot.refill_rounds = metrics_.refill_rounds;
        snapshot.dispatched_tasks = metrics_.dispatched_tasks;
        snapshot.base_dispatched_tasks = metrics_.base_dispatched_tasks;
        snapshot.bonus_dispatched_tasks = metrics_.bonus_dispatched_tasks;
        snapshot.max_total_in_flight = metrics_.max_total_in_flight;
        snapshot.max_visible_query_share_count = metrics_.max_visible_query_share_count;
        snapshot.max_runnable_queue_size = metrics_.max_runnable_queue_size;
#endif
        return snapshot;
    }

    void ResetMetrics() {
        std::lock_guard lock(mutex_);
#if DEXKIT_ENABLE_INTERNAL_METRICS
        metrics_ = {};
#endif
    }

    void ActivateQuery(uint64_t query_id) {
        std::vector<DispatchTask> dispatch_tasks;
        {
            std::lock_guard lock(mutex_);
            auto &slot = query_slots_[query_id];
            slot.query_id = query_id;
            slot.attached = true;
            if (!slot.activated) {
                slot.activated = true;
                slot.activation_sequence = next_activation_sequence_++;
            }
            auto query_share_count = SyncQueryShareCountLocked();
            if (!slot.pending_tasks.empty() && slot.TotalDispatchBudget() == 0) {
                AssignDispatchBudgetsLocked(slot, query_share_count);
            }
            TryEnqueueRunnableLocked(slot);
            DispatchReadyTasksLocked(dispatch_tasks);
        }
        EnqueueDispatchTasks(std::move(dispatch_tasks));
    }

    void DetachQuery(uint64_t query_id) {
        std::vector<DispatchTask> dispatch_tasks;
        {
            std::lock_guard lock(mutex_);
            auto it = query_slots_.find(query_id);
            if (it == query_slots_.end()) {
                return;
            }
            it->second.attached = false;
            it->second.query_context = nullptr;
            (void) SyncQueryShareCountLocked();
            TryEraseSlotLocked(it);
            DispatchReadyTasksLocked(dispatch_tasks);
        }
        EnqueueDispatchTasks(std::move(dispatch_tasks));
    }

    void Submit(uint64_t query_id, std::function<void()> task) {
        std::vector<DispatchTask> dispatch_tasks;
        {
            std::lock_guard lock(mutex_);
            auto &slot = query_slots_[query_id];
            slot.query_id = query_id;
            slot.attached = true;
            slot.submission_started = true;
            auto query_share_count = SyncQueryShareCountLocked();
            auto was_idle = slot.pending_tasks.empty() && slot.in_flight == 0;
            slot.pending_tasks.emplace_back(std::move(task));
            if (slot.activated && was_idle && slot.TotalDispatchBudget() == 0) {
                AssignDispatchBudgetsLocked(slot, query_share_count);
            }
            TryEnqueueRunnableLocked(slot);
            DispatchReadyTasksLocked(dispatch_tasks);
        }
        EnqueueDispatchTasks(std::move(dispatch_tasks));
    }

private:
    struct QuerySlot {
        uint64_t query_id = 0;
        QueryContext *query_context = nullptr;
        QueryPriority priority = QueryPriority::Normal;
        uint64_t activation_sequence = 0;
        uint64_t last_base_dispatch_sequence = 0;
        uint64_t last_bonus_dispatch_sequence = 0;
        bool attached = true;
        bool activated = false;
        bool submission_started = false;
        bool queued = false;
        size_t in_flight = 0;
        size_t base_dispatch_budget = 0;
        size_t bonus_dispatch_budget = 0;
        std::deque<std::function<void()>> pending_tasks;

        [[nodiscard]] size_t TotalDispatchBudget() const {
            return base_dispatch_budget + bonus_dispatch_budget;
        }
    };

#if DEXKIT_ENABLE_INTERNAL_METRICS
    struct QuerySchedulerMetricsState {
        size_t share_count_syncs = 0;
        size_t share_count_changes = 0;
        size_t budget_rebalances = 0;
        size_t runnable_queue_rebuilds = 0;
        size_t refill_rounds = 0;
        size_t dispatched_tasks = 0;
        size_t base_dispatched_tasks = 0;
        size_t bonus_dispatched_tasks = 0;
        size_t max_total_in_flight = 0;
        size_t max_visible_query_share_count = 0;
        size_t max_runnable_queue_size = 0;
    };
#endif

    struct DispatchTask {
        uint64_t query_id = 0;
        std::function<void()> task;
    };

    struct DispatchRoundPolicy {
        size_t visible_query_share_count = 1;
        size_t query_in_flight_limit = 1;
        size_t base_dispatch_budget_cap = 1;
        bool latency_sensitive_bonus_phase_enabled = false;
    };

    using QuerySlotMap = std::unordered_map<uint64_t, QuerySlot>;

    class TaskCompletionGuard {
    public:
        TaskCompletionGuard(std::shared_ptr<QueryScheduler> scheduler, uint64_t query_id)
                : scheduler_(std::move(scheduler)), query_id_(query_id) {}

        TaskCompletionGuard(const TaskCompletionGuard &) = delete;
        TaskCompletionGuard &operator=(const TaskCompletionGuard &) = delete;

        ~TaskCompletionGuard() {
            if (scheduler_ != nullptr) {
                scheduler_->OnTaskFinished(query_id_);
            }
        }

    private:
        std::shared_ptr<QueryScheduler> scheduler_;
        uint64_t query_id_ = 0;
    };

    [[nodiscard]] size_t QueryInFlightLimitForActiveQueryCount(size_t active_query_count) const {
        return std::max<size_t>(1, (worker_count_ + active_query_count - 1) / active_query_count);
    }

    [[nodiscard]] DispatchRoundPolicy ComputeDispatchRoundPolicy(size_t visible_query_share_count) const {
        DispatchRoundPolicy policy;
        policy.visible_query_share_count = std::max<size_t>(1, visible_query_share_count);
        policy.query_in_flight_limit = QueryInFlightLimitForActiveQueryCount(policy.visible_query_share_count);
        if (policy.visible_query_share_count <= 1) {
            policy.base_dispatch_budget_cap = worker_count_;
            return policy;
        }

        // Base phase: every visible query first receives an equal share based on
        // the current in-flight cap. Bonus phase: latency-sensitive queries may
        // receive one extra share only after the common base phase is exhausted.
        policy.base_dispatch_budget_cap = policy.query_in_flight_limit;
        policy.latency_sensitive_bonus_phase_enabled = policy.base_dispatch_budget_cap < worker_count_;
        return policy;
    }

    [[nodiscard]] size_t VisibleQueryShareCountLocked() const {
        size_t visible_query_count = 0;
        for (const auto &[query_id, slot]: query_slots_) {
            (void) query_id;
            if (!slot.attached) {
                continue;
            }
            if (slot.activated) {
                if (!slot.pending_tasks.empty() || slot.in_flight != 0) {
                    ++visible_query_count;
                }
                continue;
            }
            if (slot.submission_started) {
                ++visible_query_count;
            }
        }
        return visible_query_count;
    }

    [[nodiscard]] size_t QueryShareCountLocked() const {
        return std::max<size_t>(1, VisibleQueryShareCountLocked());
    }

    [[nodiscard]] bool IsRunnableCandidateLocked(const QuerySlot &slot, size_t query_in_flight_limit) const {
        if (!slot.activated) {
            return false;
        }
        if (slot.queued || slot.pending_tasks.empty()) {
            return false;
        }
        if (slot.in_flight >= query_in_flight_limit) {
            return false;
        }
        return slot.TotalDispatchBudget() != 0;
    }

    void UpdateMaxMetric(size_t &target, size_t value) {
        if (target < value) {
            target = value;
        }
    }

    void UpdateMaxRunnableQueueSizeLocked() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        UpdateMaxMetric(
                metrics_.max_runnable_queue_size,
                base_runnable_queries_.size() +
                latency_sensitive_bonus_runnable_queries_.size()
        );
#endif
    }

    [[nodiscard]] size_t BonusDispatchBudgetCap(const QuerySlot &slot, const DispatchRoundPolicy &policy) const {
        if (!policy.latency_sensitive_bonus_phase_enabled) {
            return 0;
        }
        return static_cast<size_t>(slot.priority == QueryPriority::LatencySensitive);
    }

    void AssignDispatchBudgetsLocked(QuerySlot &slot, size_t query_share_count) {
        auto policy = ComputeDispatchRoundPolicy(query_share_count);
        slot.base_dispatch_budget = policy.base_dispatch_budget_cap;
        slot.bonus_dispatch_budget = BonusDispatchBudgetCap(slot, policy);
    }

    void RebalanceDispatchBudgetsLocked(size_t query_share_count) {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        ++metrics_.budget_rebalances;
#endif
        auto policy = ComputeDispatchRoundPolicy(query_share_count);
        for (auto &[query_id, slot]: query_slots_) {
            (void) query_id;
            if (!slot.activated) {
                continue;
            }
            if (slot.base_dispatch_budget > policy.base_dispatch_budget_cap) {
                slot.base_dispatch_budget = policy.base_dispatch_budget_cap;
            }
            auto max_bonus_budget = BonusDispatchBudgetCap(slot, policy);
            if (slot.bonus_dispatch_budget > max_bonus_budget) {
                slot.bonus_dispatch_budget = max_bonus_budget;
            }
        }
    }

    void RebuildRunnableQueuesLocked() {
        base_runnable_queries_.clear();
        latency_sensitive_bonus_runnable_queries_.clear();
        for (auto &[query_id, slot]: query_slots_) {
            (void) query_id;
            slot.queued = false;
        }
        (void) CollectAndEnqueueRunnableCandidatesLocked(QueryInFlightLimitLocked());
#if DEXKIT_ENABLE_INTERNAL_METRICS
        ++metrics_.runnable_queue_rebuilds;
#endif
        UpdateMaxRunnableQueueSizeLocked();
    }

    [[nodiscard]] size_t SyncQueryShareCountLocked() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        ++metrics_.share_count_syncs;
#endif
        auto query_share_count = QueryShareCountLocked();
#if DEXKIT_ENABLE_INTERNAL_METRICS
        UpdateMaxMetric(metrics_.max_visible_query_share_count, query_share_count);
        if (query_share_count == last_query_share_count_) {
            return query_share_count;
        }
        last_query_share_count_ = query_share_count;
        ++metrics_.share_count_changes;
#else
        if (query_share_count == last_query_share_count_) {
            return query_share_count;
        }
        last_query_share_count_ = query_share_count;
#endif
        RebalanceDispatchBudgetsLocked(query_share_count);
        RebuildRunnableQueuesLocked();
        return query_share_count;
    }

    [[nodiscard]] size_t QueryInFlightLimitLocked() const {
        return ComputeDispatchRoundPolicy(QueryShareCountLocked()).query_in_flight_limit;
    }

    [[nodiscard]] bool HasRunnableQueriesLocked() const {
        return !base_runnable_queries_.empty() ||
               !latency_sensitive_bonus_runnable_queries_.empty();
    }

    [[nodiscard]] uint64_t PopRunnableQueryLocked() {
        if (!base_runnable_queries_.empty()) {
            auto query_id = base_runnable_queries_.front();
            base_runnable_queries_.pop_front();
            return query_id;
        }
        auto query_id = latency_sensitive_bonus_runnable_queries_.front();
        latency_sensitive_bonus_runnable_queries_.pop_front();
        return query_id;
    }

    void PushRunnableQueryLocked(const QuerySlot &slot) {
        if (slot.base_dispatch_budget != 0) {
            base_runnable_queries_.push_back(slot.query_id);
            UpdateMaxRunnableQueueSizeLocked();
            return;
        }
        latency_sensitive_bonus_runnable_queries_.push_back(slot.query_id);
        UpdateMaxRunnableQueueSizeLocked();
    }

    void InsertRunnableQueryOrderedLocked(const QuerySlot &slot) {
        auto bonus_phase = slot.base_dispatch_budget == 0;
        auto &queue = bonus_phase ? latency_sensitive_bonus_runnable_queries_ : base_runnable_queries_;
        auto insert_it = queue.end();
        for (auto it = queue.begin(); it != queue.end(); ++it) {
            if (RunnableQueryLessLocked(slot.query_id, *it, bonus_phase)) {
                insert_it = it;
                break;
            }
        }
        queue.insert(insert_it, slot.query_id);
        UpdateMaxRunnableQueueSizeLocked();
    }

    [[nodiscard]] bool RunnableQueryLessLocked(uint64_t lhs_query_id, uint64_t rhs_query_id, bool bonus_phase) const {
        const auto &lhs = query_slots_.at(lhs_query_id);
        const auto &rhs = query_slots_.at(rhs_query_id);
        auto lhs_dispatch_sequence = bonus_phase ? lhs.last_bonus_dispatch_sequence : lhs.last_base_dispatch_sequence;
        auto rhs_dispatch_sequence = bonus_phase ? rhs.last_bonus_dispatch_sequence : rhs.last_base_dispatch_sequence;
        return std::tie(lhs_dispatch_sequence, lhs.activation_sequence, lhs.query_id) <
               std::tie(rhs_dispatch_sequence, rhs.activation_sequence, rhs.query_id);
    }

    void EnqueueSortedRunnableQueriesLocked(std::vector<uint64_t> &query_ids, bool bonus_phase) {
        std::sort(query_ids.begin(), query_ids.end(), [this, bonus_phase](uint64_t lhs_query_id, uint64_t rhs_query_id) {
            return RunnableQueryLessLocked(lhs_query_id, rhs_query_id, bonus_phase);
        });
        for (auto query_id: query_ids) {
            auto it = query_slots_.find(query_id);
            if (it == query_slots_.end()) {
                continue;
            }
            auto &slot = it->second;
            if (slot.queued) {
                continue;
            }
            slot.queued = true;
            PushRunnableQueryLocked(slot);
        }
    }

    [[nodiscard]] bool CollectAndEnqueueRunnableCandidatesLocked(size_t query_in_flight_limit) {
        std::vector<uint64_t> base_query_ids;
        std::vector<uint64_t> bonus_query_ids;
        for (auto &[query_id, slot]: query_slots_) {
            (void) query_id;
            if (!IsRunnableCandidateLocked(slot, query_in_flight_limit)) {
                continue;
            }
            if (slot.base_dispatch_budget != 0) {
                base_query_ids.push_back(slot.query_id);
            } else {
                bonus_query_ids.push_back(slot.query_id);
            }
        }
        if (base_query_ids.empty() && bonus_query_ids.empty()) {
            return false;
        }
        EnqueueSortedRunnableQueriesLocked(base_query_ids, false);
        EnqueueSortedRunnableQueriesLocked(bonus_query_ids, true);
        return true;
    }

    bool RefillDispatchBudgetsLocked() {
#if DEXKIT_ENABLE_INTERNAL_METRICS
        ++metrics_.refill_rounds;
#endif
        auto query_share_count = SyncQueryShareCountLocked();
        auto policy = ComputeDispatchRoundPolicy(query_share_count);
        auto query_in_flight_limit = policy.query_in_flight_limit;

        for (auto &[query_id, slot]: query_slots_) {
            (void) query_id;
            if (!slot.activated) {
                continue;
            }
            if (slot.pending_tasks.empty()) {
                continue;
            }
            if (slot.in_flight >= query_in_flight_limit) {
                continue;
            }
            if (slot.TotalDispatchBudget() == 0) {
                slot.base_dispatch_budget = policy.base_dispatch_budget_cap;
                slot.bonus_dispatch_budget = BonusDispatchBudgetCap(slot, policy);
            }
        }

        return CollectAndEnqueueRunnableCandidatesLocked(query_in_flight_limit);
    }

    void TryEnqueueRunnableLocked(QuerySlot &slot) {
        if (!IsRunnableCandidateLocked(slot, QueryInFlightLimitLocked())) {
            return;
        }
        slot.queued = true;
        InsertRunnableQueryOrderedLocked(slot);
    }

    void TryEraseSlotLocked(QuerySlotMap::iterator it) {
        if (it->second.attached) {
            return;
        }
        if (!it->second.pending_tasks.empty()) {
            return;
        }
        if (it->second.in_flight != 0) {
            return;
        }
        query_slots_.erase(it);
    }

    void DispatchReadyTasksLocked(std::vector<DispatchTask> &dispatch_tasks) {
        while (total_in_flight_ < worker_count_) {
            if (!HasRunnableQueriesLocked() && !RefillDispatchBudgetsLocked()) {
                break;
            }
            if (!HasRunnableQueriesLocked()) {
                break;
            }

            auto query_id = PopRunnableQueryLocked();

            auto it = query_slots_.find(query_id);
            if (it == query_slots_.end()) {
                continue;
            }

            auto &slot = it->second;
            slot.queued = false;

            auto query_in_flight_limit = QueryInFlightLimitLocked();
            if (slot.pending_tasks.empty() || slot.in_flight >= query_in_flight_limit || slot.TotalDispatchBudget() == 0) {
                TryEnqueueRunnableLocked(slot);
                TryEraseSlotLocked(it);
                continue;
            }

            auto task = std::move(slot.pending_tasks.front());
            slot.pending_tasks.pop_front();
            auto query_share_count = QueryShareCountLocked();
            auto used_bonus_dispatch = slot.base_dispatch_budget == 0;
            if (!used_bonus_dispatch) {
                --slot.base_dispatch_budget;
                slot.last_base_dispatch_sequence = next_dispatch_sequence_++;
            } else {
                --slot.bonus_dispatch_budget;
                slot.last_bonus_dispatch_sequence = next_dispatch_sequence_++;
            }
            ++slot.in_flight;
            ++total_in_flight_;
#if DEXKIT_ENABLE_INTERNAL_METRICS
            ++metrics_.dispatched_tasks;
            if (used_bonus_dispatch) {
                ++metrics_.bonus_dispatched_tasks;
            } else {
                ++metrics_.base_dispatched_tasks;
            }
            UpdateMaxMetric(metrics_.max_total_in_flight, total_in_flight_);
#endif
            if (slot.query_context != nullptr) {
                slot.query_context->MarkTaskDispatched(
                        used_bonus_dispatch,
                        static_cast<uint32_t>(slot.in_flight),
                        static_cast<uint32_t>(query_share_count)
                );
            }
            TryEnqueueRunnableLocked(slot);

            dispatch_tasks.push_back(DispatchTask{query_id, std::move(task)});
        }
    }

    void EnqueueDispatchTasks(std::vector<DispatchTask> dispatch_tasks) {
        if (dispatch_tasks.empty()) {
            return;
        }

        auto self = shared_from_this();
        for (auto &dispatch_task: dispatch_tasks) {
            pool_->enqueue([self, dispatch_task = std::move(dispatch_task)]() mutable {
                TaskCompletionGuard completion_guard(self, dispatch_task.query_id);
                dispatch_task.task();
            });
        }
    }

    void OnTaskFinished(uint64_t query_id) {
        std::vector<DispatchTask> dispatch_tasks;
        {
            std::lock_guard lock(mutex_);
            if (total_in_flight_ > 0) {
                --total_in_flight_;
            }

            auto it = query_slots_.find(query_id);
            if (it != query_slots_.end()) {
                auto &slot = it->second;
                if (slot.in_flight > 0) {
                    --slot.in_flight;
                }
                (void) SyncQueryShareCountLocked();
                TryEnqueueRunnableLocked(slot);
                TryEraseSlotLocked(it);
            }

            DispatchReadyTasksLocked(dispatch_tasks);
        }
        EnqueueDispatchTasks(std::move(dispatch_tasks));
    }

    std::shared_ptr<ThreadPool> pool_;
    size_t worker_count_;
    mutable std::mutex mutex_;
    QuerySlotMap query_slots_;
    std::deque<uint64_t> base_runnable_queries_;
    std::deque<uint64_t> latency_sensitive_bonus_runnable_queries_;
#if DEXKIT_ENABLE_INTERNAL_METRICS
    QuerySchedulerMetricsState metrics_;
#endif
    uint64_t next_activation_sequence_ = 1;
    uint64_t next_dispatch_sequence_ = 1;
    size_t last_query_share_count_ = 1;
    size_t total_in_flight_ = 0;
};

} // namespace dexkit
