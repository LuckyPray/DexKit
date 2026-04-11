package org.luckypray.dexkit;

final class InternalMetricsBridge {
    private InternalMetricsBridge() {
    }

    static native void nativeSetQueryMetricsEnabled(long nativePtr, boolean enabled);

    static native long[] nativeGetSchedulerMetrics(long nativePtr);

    static native long[] nativeGetLastQueryMetrics(long nativePtr);

    static native long[] nativeGetQueryMetricsHistory(long nativePtr);

    static native void nativeResetSchedulerMetrics(long nativePtr);

    static native void nativeResetQueryMetricsHistory(long nativePtr);
}
