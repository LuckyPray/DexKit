package org.luckypray.dexkit

import org.luckypray.dexkit.annotations.DexKitExperimentalApi

@DexKitExperimentalApi
fun DexKitBridge.setQueryMetricsEnabled(enabled: Boolean) {
    withNativeReadToken { InternalMetricsBridge.nativeSetQueryMetricsEnabled(it, enabled) }
}

@DexKitExperimentalApi
fun DexKitBridge.getSchedulerMetricsSnapshot(): SchedulerMetricsSnapshot {
    return SchedulerMetricsSnapshot.fromNative(
        withNativeReadToken { InternalMetricsBridge.nativeGetSchedulerMetrics(it) }
    )
}

@DexKitExperimentalApi
fun DexKitBridge.getLastQueryMetricsSnapshot(): QueryMetricsSnapshot {
    return QueryMetricsSnapshot.fromNative(
        withNativeReadToken { InternalMetricsBridge.nativeGetLastQueryMetrics(it) }
    )
}

@DexKitExperimentalApi
fun DexKitBridge.getQueryMetricsHistorySnapshot(): QueryMetricsHistorySnapshot {
    return QueryMetricsHistorySnapshot.fromNative(
        withNativeReadToken { InternalMetricsBridge.nativeGetQueryMetricsHistory(it) }
    )
}

@DexKitExperimentalApi
fun DexKitBridge.resetSchedulerMetrics() {
    withNativeReadToken { InternalMetricsBridge.nativeResetSchedulerMetrics(it) }
}

@DexKitExperimentalApi
fun DexKitBridge.resetQueryMetricsHistory() {
    withNativeReadToken { InternalMetricsBridge.nativeResetQueryMetricsHistory(it) }
}

@DexKitExperimentalApi
data class QueryMetricsSnapshot(
    val submittedTasks: Long,
    val dispatchedTasks: Long,
    val baseDispatchedTasks: Long,
    val bonusDispatchedTasks: Long,
    val completedTasks: Long,
    val maxInFlight: Long,
    val maxQueryShareCount: Long,
    val firstDispatchDelayNs: Long,
    val firstBonusDispatchDelayNs: Long,
    val firstTaskStartDelayNs: Long,
    val lastTaskFinishDelayNs: Long,
    val taskRuntimeTotalNs: Long,
    val taskRuntimeMaxNs: Long,
    val preprocessCompletedNs: Long,
    val submissionCompletedNs: Long,
    val workersCompletedNs: Long,
    val completedNs: Long,
) {
    internal companion object {
        const val NATIVE_SIZE = 17

        fun fromNative(values: LongArray): QueryMetricsSnapshot {
            require(values.size == NATIVE_SIZE) {
                "Unexpected query metrics size: ${values.size}"
            }
            return fromNative(values, 0)
        }

        fun fromNative(values: LongArray, offset: Int): QueryMetricsSnapshot {
            require(offset >= 0 && values.size >= offset + NATIVE_SIZE) {
                "Unexpected query metrics slice: size=${values.size}, offset=$offset"
            }
            return QueryMetricsSnapshot(
                submittedTasks = values[offset],
                dispatchedTasks = values[offset + 1],
                baseDispatchedTasks = values[offset + 2],
                bonusDispatchedTasks = values[offset + 3],
                completedTasks = values[offset + 4],
                maxInFlight = values[offset + 5],
                maxQueryShareCount = values[offset + 6],
                firstDispatchDelayNs = values[offset + 7],
                firstBonusDispatchDelayNs = values[offset + 8],
                firstTaskStartDelayNs = values[offset + 9],
                lastTaskFinishDelayNs = values[offset + 10],
                taskRuntimeTotalNs = values[offset + 11],
                taskRuntimeMaxNs = values[offset + 12],
                preprocessCompletedNs = values[offset + 13],
                submissionCompletedNs = values[offset + 14],
                workersCompletedNs = values[offset + 15],
                completedNs = values[offset + 16],
            )
        }
    }
}

@DexKitExperimentalApi
data class QueryMetricsHistorySnapshot(
    val droppedRecords: Long,
    val records: List<QueryMetricsRecord>,
) {
    internal companion object {
        private const val NATIVE_HEADER_SIZE = 2
        private const val NATIVE_RECORD_STRIDE = 2 + QueryMetricsSnapshot.NATIVE_SIZE

        fun fromNative(values: LongArray): QueryMetricsHistorySnapshot {
            require(values.size >= NATIVE_HEADER_SIZE) {
                "Unexpected query metrics history size: ${values.size}"
            }

            val recordCount = values[1].toInt()
            require(recordCount >= 0) {
                "Unexpected query metrics history record count: ${values[1]}"
            }

            val expectedSize = NATIVE_HEADER_SIZE + recordCount * NATIVE_RECORD_STRIDE
            require(values.size == expectedSize) {
                "Unexpected query metrics history payload size: ${values.size}, expected: $expectedSize"
            }

            val records = ArrayList<QueryMetricsRecord>(recordCount)
            var offset = NATIVE_HEADER_SIZE
            repeat(recordCount) {
                val kind = QueryMetricsKind.fromNative(values[offset++])
                val priority = QueryMetricsPriority.fromNative(values[offset++])
                val metrics = QueryMetricsSnapshot.fromNative(values, offset)
                offset += QueryMetricsSnapshot.NATIVE_SIZE
                records.add(
                    QueryMetricsRecord(
                        kind = kind,
                        priority = priority,
                        metrics = metrics,
                    )
                )
            }

            return QueryMetricsHistorySnapshot(
                droppedRecords = values[0],
                records = records,
            )
        }
    }
}

@DexKitExperimentalApi
data class QueryMetricsRecord(
    val kind: QueryMetricsKind,
    val priority: QueryMetricsPriority,
    val metrics: QueryMetricsSnapshot,
)

@DexKitExperimentalApi
enum class QueryMetricsKind(internal val nativeValue: Long) {
    FIND_CLASS(0),
    FIND_METHOD(1),
    FIND_FIELD(2),
    BATCH_FIND_CLASS_USING_STRINGS(3),
    BATCH_FIND_METHOD_USING_STRINGS(4),
    ;

    internal companion object {
        fun fromNative(value: Long): QueryMetricsKind {
            return values().firstOrNull { it.nativeValue == value }
                ?: error("Unknown query metrics kind: $value")
        }
    }
}

@DexKitExperimentalApi
enum class QueryMetricsPriority(internal val nativeValue: Long) {
    NORMAL(0),
    LATENCY_SENSITIVE(1),
    ;

    internal companion object {
        fun fromNative(value: Long): QueryMetricsPriority {
            return values().firstOrNull { it.nativeValue == value }
                ?: error("Unknown query metrics priority: $value")
        }
    }
}

@DexKitExperimentalApi
data class SchedulerMetricsSnapshot(
    val shareCountSyncs: Long,
    val shareCountChanges: Long,
    val budgetRebalances: Long,
    val runnableQueueRebuilds: Long,
    val refillRounds: Long,
    val dispatchedTasks: Long,
    val baseDispatchedTasks: Long,
    val bonusDispatchedTasks: Long,
    val maxTotalInFlight: Long,
    val maxVisibleQueryShareCount: Long,
    val maxRunnableQueueSize: Long,
) {
    internal companion object {
        const val NATIVE_SIZE = 11

        fun fromNative(values: LongArray): SchedulerMetricsSnapshot {
            require(values.size == NATIVE_SIZE) {
                "Unexpected scheduler metrics size: ${values.size}"
            }
            return SchedulerMetricsSnapshot(
                shareCountSyncs = values[0],
                shareCountChanges = values[1],
                budgetRebalances = values[2],
                runnableQueueRebuilds = values[3],
                refillRounds = values[4],
                dispatchedTasks = values[5],
                baseDispatchedTasks = values[6],
                bonusDispatchedTasks = values[7],
                maxTotalInFlight = values[8],
                maxVisibleQueryShareCount = values[9],
                maxRunnableQueueSize = values[10],
            )
        }
    }
}
