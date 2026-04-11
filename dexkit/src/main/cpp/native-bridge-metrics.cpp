#include <jni.h>
#include <vector>
#include "dexkit.h"

#define DEXKIT_JNI JNIEXPORT JNICALL

extern "C" {

DEXKIT_JNI void
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeSetQueryMetricsEnabled(JNIEnv *env, jclass clazz,
                                                                             jlong native_ptr,
                                                                             jboolean enabled
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    dexkit->SetQueryMetricsEnabled(enabled == JNI_TRUE);
}

DEXKIT_JNI jlongArray
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeGetSchedulerMetrics(JNIEnv *env, jclass clazz,
                                                                          jlong native_ptr
) {
    constexpr jsize kSchedulerMetricCount = 11;
    jlong values[kSchedulerMetricCount] = {};
    if (native_ptr) {
        auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
        auto snapshot = dexkit->GetQuerySchedulerMetricsSnapshot();
        values[0] = static_cast<jlong>(snapshot.share_count_syncs);
        values[1] = static_cast<jlong>(snapshot.share_count_changes);
        values[2] = static_cast<jlong>(snapshot.budget_rebalances);
        values[3] = static_cast<jlong>(snapshot.runnable_queue_rebuilds);
        values[4] = static_cast<jlong>(snapshot.refill_rounds);
        values[5] = static_cast<jlong>(snapshot.dispatched_tasks);
        values[6] = static_cast<jlong>(snapshot.base_dispatched_tasks);
        values[7] = static_cast<jlong>(snapshot.bonus_dispatched_tasks);
        values[8] = static_cast<jlong>(snapshot.max_total_in_flight);
        values[9] = static_cast<jlong>(snapshot.max_visible_query_share_count);
        values[10] = static_cast<jlong>(snapshot.max_runnable_queue_size);
    }
    auto ret = env->NewLongArray(kSchedulerMetricCount);
    if (ret != nullptr) {
        env->SetLongArrayRegion(ret, 0, kSchedulerMetricCount, values);
    }
    return ret;
}

DEXKIT_JNI jlongArray
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeGetLastQueryMetrics(JNIEnv *env, jclass clazz,
                                                                          jlong native_ptr
) {
    constexpr jsize kQueryMetricCount = 17;
    jlong values[kQueryMetricCount] = {};
    if (native_ptr) {
        auto snapshot = dexkit::DexKit::GetLastQueryMetricsSnapshot();
        values[0] = static_cast<jlong>(snapshot.submitted_tasks);
        values[1] = static_cast<jlong>(snapshot.dispatched_tasks);
        values[2] = static_cast<jlong>(snapshot.base_dispatched_tasks);
        values[3] = static_cast<jlong>(snapshot.bonus_dispatched_tasks);
        values[4] = static_cast<jlong>(snapshot.completed_tasks);
        values[5] = static_cast<jlong>(snapshot.max_in_flight);
        values[6] = static_cast<jlong>(snapshot.max_query_share_count);
        values[7] = static_cast<jlong>(snapshot.first_dispatch_delay_ns);
        values[8] = static_cast<jlong>(snapshot.first_bonus_dispatch_delay_ns);
        values[9] = static_cast<jlong>(snapshot.first_task_start_delay_ns);
        values[10] = static_cast<jlong>(snapshot.last_task_finish_delay_ns);
        values[11] = static_cast<jlong>(snapshot.task_runtime_total_ns);
        values[12] = static_cast<jlong>(snapshot.task_runtime_max_ns);
        values[13] = static_cast<jlong>(snapshot.preprocess_completed_ns);
        values[14] = static_cast<jlong>(snapshot.submission_completed_ns);
        values[15] = static_cast<jlong>(snapshot.workers_completed_ns);
        values[16] = static_cast<jlong>(snapshot.completed_ns);
    }
    auto ret = env->NewLongArray(kQueryMetricCount);
    if (ret != nullptr) {
        env->SetLongArrayRegion(ret, 0, kQueryMetricCount, values);
    }
    return ret;
}

DEXKIT_JNI jlongArray
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeGetQueryMetricsHistory(JNIEnv *env, jclass clazz,
                                                                             jlong native_ptr
) {
    constexpr jsize kQueryMetricCount = 17;
    constexpr jsize kQueryMetricsHistoryHeaderSize = 2;
    constexpr jsize kQueryMetricsHistoryRecordStride = 2 + kQueryMetricCount;

    std::vector<jlong> values(kQueryMetricsHistoryHeaderSize, 0);
    if (native_ptr) {
        auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
        auto snapshot = dexkit->GetQueryMetricsHistorySnapshot();
        values[0] = static_cast<jlong>(snapshot.dropped_records);
        values[1] = static_cast<jlong>(snapshot.records.size());
        values.resize(kQueryMetricsHistoryHeaderSize +
                      static_cast<jsize>(snapshot.records.size()) * kQueryMetricsHistoryRecordStride);
        size_t offset = kQueryMetricsHistoryHeaderSize;
        for (const auto &record: snapshot.records) {
            values[offset++] = static_cast<jlong>(record.kind);
            values[offset++] = static_cast<jlong>(record.priority);
            values[offset++] = static_cast<jlong>(record.metrics.submitted_tasks);
            values[offset++] = static_cast<jlong>(record.metrics.dispatched_tasks);
            values[offset++] = static_cast<jlong>(record.metrics.base_dispatched_tasks);
            values[offset++] = static_cast<jlong>(record.metrics.bonus_dispatched_tasks);
            values[offset++] = static_cast<jlong>(record.metrics.completed_tasks);
            values[offset++] = static_cast<jlong>(record.metrics.max_in_flight);
            values[offset++] = static_cast<jlong>(record.metrics.max_query_share_count);
            values[offset++] = static_cast<jlong>(record.metrics.first_dispatch_delay_ns);
            values[offset++] = static_cast<jlong>(record.metrics.first_bonus_dispatch_delay_ns);
            values[offset++] = static_cast<jlong>(record.metrics.first_task_start_delay_ns);
            values[offset++] = static_cast<jlong>(record.metrics.last_task_finish_delay_ns);
            values[offset++] = static_cast<jlong>(record.metrics.task_runtime_total_ns);
            values[offset++] = static_cast<jlong>(record.metrics.task_runtime_max_ns);
            values[offset++] = static_cast<jlong>(record.metrics.preprocess_completed_ns);
            values[offset++] = static_cast<jlong>(record.metrics.submission_completed_ns);
            values[offset++] = static_cast<jlong>(record.metrics.workers_completed_ns);
            values[offset++] = static_cast<jlong>(record.metrics.completed_ns);
        }
    }
    auto ret = env->NewLongArray(static_cast<jsize>(values.size()));
    if (ret != nullptr) {
        env->SetLongArrayRegion(ret, 0, static_cast<jsize>(values.size()), values.data());
    }
    return ret;
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeResetSchedulerMetrics(JNIEnv *env, jclass clazz,
                                                                            jlong native_ptr
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    dexkit->ResetQuerySchedulerMetrics();
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_InternalMetricsBridge_nativeResetQueryMetricsHistory(JNIEnv *env, jclass clazz,
                                                                               jlong native_ptr
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    dexkit->ResetQueryMetricsHistory();
}

}
