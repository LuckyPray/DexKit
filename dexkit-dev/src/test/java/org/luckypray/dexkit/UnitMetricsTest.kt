package org.luckypray.dexkit

import org.junit.Test
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UnitMetricsTest {

    companion object {

        private val demoApkPath: String

        init {
            loadLibrary("dexkit")
            val path = System.getProperty("apk.path")
            val demoApk = File(path, "demo.apk")
            demoApkPath = demoApk.absolutePath
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testSharedSchedulerMetricsSnapshot() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(4)
            try {
                val futures = listOf(
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(8) {
                            val result = parallelBridge.findMethod {
                                excludePackages("org.luckypray.dexkit.demo.hook")
                                matcher {
                                    usingNumbers(114514)
                                }
                            }
                            assert(result.size == 2)
                        }
                    },
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(8) {
                            val result = parallelBridge.findMethod {
                                findFirst = true
                                excludePackages("org.luckypray.dexkit.demo.hook")
                                matcher {
                                    usingNumbers(114514)
                                }
                            }
                            assert(result.size == 1)
                        }
                    }
                )
                start.countDown()
                futures.forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            val metrics = parallelBridge.getSchedulerMetricsSnapshot()
            println(metrics)
            assert(metrics.dispatchedTasks > 0)
            assert(metrics.baseDispatchedTasks + metrics.bonusDispatchedTasks == metrics.dispatchedTasks)
            assert(metrics.baseDispatchedTasks > 0)
            assert(metrics.bonusDispatchedTasks > 0)
            assert(metrics.maxTotalInFlight > 0)
            assert(metrics.maxVisibleQueryShareCount >= 2)
            assert(metrics.shareCountSyncs > 0)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testSharedSchedulerMetricsReset() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)

            repeat(4) {
                val result = parallelBridge.findMethod {
                    excludePackages("org.luckypray.dexkit.demo.hook")
                    matcher {
                        usingNumbers(114514)
                    }
                }
                assert(result.size == 2)
            }

            val beforeReset = parallelBridge.getSchedulerMetricsSnapshot()
            assert(beforeReset.dispatchedTasks > 0)

            parallelBridge.resetSchedulerMetrics()

            val afterReset = parallelBridge.getSchedulerMetricsSnapshot()
            assert(afterReset.dispatchedTasks == 0L)
            assert(afterReset.baseDispatchedTasks == 0L)
            assert(afterReset.bonusDispatchedTasks == 0L)
            assert(afterReset.shareCountSyncs == 0L)
            assert(afterReset.maxTotalInFlight == 0L)
            assert(afterReset.maxVisibleQueryShareCount == 0L)
            assert(afterReset.maxRunnableQueueSize == 0L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testLastQueryMetricsSnapshotOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val result = parallelBridge.findMethod {
                excludePackages("org.luckypray.dexkit.demo.hook")
                matcher {
                    usingNumbers(114514)
                }
            }
            assert(result.size == 2)

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            println(metrics)
            assert(metrics.submittedTasks > 0)
            assert(metrics.dispatchedTasks > 0)
            assert(metrics.completedTasks == metrics.submittedTasks)
            assert(metrics.baseDispatchedTasks + metrics.bonusDispatchedTasks == metrics.dispatchedTasks)
            assert(metrics.maxInFlight > 0)
            assert(metrics.maxQueryShareCount > 0)
            assert(metrics.firstDispatchDelayNs >= 0)
            assert(metrics.firstBonusDispatchDelayNs == -1L)
            assert(metrics.firstTaskStartDelayNs >= 0)
            assert(metrics.lastTaskFinishDelayNs >= metrics.firstTaskStartDelayNs)
            assert(metrics.taskRuntimeTotalNs >= metrics.taskRuntimeMaxNs)
            assert(metrics.preprocessCompletedNs >= 0)
            assert(metrics.submissionCompletedNs >= metrics.preprocessCompletedNs)
            assert(metrics.workersCompletedNs >= metrics.submissionCompletedNs)
            assert(metrics.completedNs >= metrics.workersCompletedNs)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testFindFirstFastDeclaredClassPathOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()
            parallelBridge.resetSchedulerMetrics()

            val result = parallelBridge.findMethod {
                findFirst = true
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                    usingNumbers(114514)
                }
            }
            assert(result.size == 1)

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            println(metrics)
            assert(metrics.submittedTasks == 0L)
            assert(metrics.dispatchedTasks == 0L)
            assert(metrics.completedTasks == 0L)
            assert(metrics.firstDispatchDelayNs == -1L)
            assert(metrics.firstBonusDispatchDelayNs == -1L)
            assert(metrics.preprocessCompletedNs >= 0)
            assert(metrics.submissionCompletedNs >= metrics.preprocessCompletedNs)
            assert(metrics.workersCompletedNs >= metrics.submissionCompletedNs)
            assert(metrics.completedNs >= metrics.workersCompletedNs)

            val schedulerMetrics = parallelBridge.getSchedulerMetricsSnapshot()
            assert(schedulerMetrics.dispatchedTasks == 0L)

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            assert(history.records.size == 1)
            val record = history.records.single()
            assert(record.priority == QueryMetricsPriority.LATENCY_SENSITIVE)
            assert(record.metrics.submittedTasks == 0L)
            assert(record.metrics.dispatchedTasks == 0L)
            assert(record.metrics.firstDispatchDelayNs == -1L)
            assert(record.metrics.firstBonusDispatchDelayNs == -1L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testFindMethodFastDeclaredPathFindsNamedNonConstructorOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()
            parallelBridge.resetSchedulerMetrics()

            val result = parallelBridge.findMethod {
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                    name = "onCreate"
                    paramTypes("android.os.Bundle")
                }
            }
            assert(result.size == 1)
            assert(result.single().className == "org.luckypray.dexkit.demo.PlayActivity")
            assert(result.single().name == "onCreate")

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            assert(metrics.submittedTasks == 0L)
            assert(metrics.dispatchedTasks == 0L)
            assert(metrics.completedTasks == 0L)
            assert(metrics.firstDispatchDelayNs == -1L)
            assert(metrics.firstBonusDispatchDelayNs == -1L)
            assert(metrics.preprocessCompletedNs >= 0)
            assert(metrics.submissionCompletedNs >= metrics.preprocessCompletedNs)
            assert(metrics.workersCompletedNs >= metrics.submissionCompletedNs)
            assert(metrics.completedNs >= metrics.workersCompletedNs)
            assert(parallelBridge.getSchedulerMetricsSnapshot().dispatchedTasks == 0L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testFindClassFastDeclaredPathRespectsSearchClassesOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val mainActivity = parallelBridge.getClassData("Lorg/luckypray/dexkit/demo/MainActivity;")
            assert(mainActivity != null)

            parallelBridge.resetSchedulerMetrics()

            val result = parallelBridge.findClass {
                findFirst = true
                searchIn(listOf(mainActivity!!))
                matcher {
                    className("org.luckypray.dexkit.demo.PlayActivity")
                }
            }
            assert(result.isEmpty())

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            assert(metrics.submittedTasks == 0L)
            assert(metrics.dispatchedTasks == 0L)
            assert(parallelBridge.getSchedulerMetricsSnapshot().dispatchedTasks == 0L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testFindMethodFastDeclaredPathRespectsSearchMethodsOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val allMethods = parallelBridge.findMethod {
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                }
            }
            assert(allMethods.size > 1)
            val expectedMethod = allMethods.last()

            parallelBridge.resetSchedulerMetrics()

            val result = parallelBridge.findMethod {
                findFirst = true
                searchInMethod(listOf(expectedMethod))
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                }
            }
            assert(result.size == 1)
            assert(result.single().getEncodeId() == expectedMethod.getEncodeId())

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            assert(metrics.submittedTasks == 0L)
            assert(metrics.dispatchedTasks == 0L)
            assert(parallelBridge.getSchedulerMetricsSnapshot().dispatchedTasks == 0L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testFindFieldFastDeclaredPathRespectsSearchFieldsOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val allFields = parallelBridge.findField {
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                }
            }
            assert(allFields.size > 1)
            val expectedField = allFields.last()

            parallelBridge.resetSchedulerMetrics()

            val result = parallelBridge.findField {
                findFirst = true
                searchInField(listOf(expectedField))
                matcher {
                    declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                }
            }
            assert(result.size == 1)
            assert(result.single().getEncodeId() == expectedField.getEncodeId())

            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
            assert(metrics.submittedTasks == 0L)
            assert(metrics.dispatchedTasks == 0L)
            assert(parallelBridge.getSchedulerMetricsSnapshot().dispatchedTasks == 0L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testLastQueryMetricsSnapshotIsThreadLocalOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)
            val workers = 2
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(4) {
                            val result = parallelBridge.findMethod {
                                excludePackages("org.luckypray.dexkit.demo.hook")
                                matcher {
                                    usingNumbers(114514)
                                }
                            }
                            assert(result.size == 2)
                            val metrics = parallelBridge.getLastQueryMetricsSnapshot()
                            assert(metrics.submittedTasks > 0)
                            assert(metrics.dispatchedTasks > 0)
                            assert(metrics.completedTasks == metrics.submittedTasks)
                            assert(metrics.baseDispatchedTasks + metrics.bonusDispatchedTasks == metrics.dispatchedTasks)
                            assert(metrics.maxInFlight > 0)
                            assert(metrics.firstBonusDispatchDelayNs == -1L)
                            assert(metrics.firstTaskStartDelayNs >= 0)
                            assert(metrics.lastTaskFinishDelayNs >= metrics.firstTaskStartDelayNs)
                            assert(metrics.taskRuntimeTotalNs >= metrics.taskRuntimeMaxNs)
                            assert(metrics.completedNs >= metrics.workersCompletedNs)
                        }
                    }
                }
                start.countDown()
                futures.forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testQueryMetricsHistorySnapshotOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val regularResult = parallelBridge.findMethod {
                excludePackages("org.luckypray.dexkit.demo.hook")
                matcher {
                    usingNumbers(114514)
                }
            }
            assert(regularResult.size == 2)

            val firstResult = parallelBridge.findMethod {
                findFirst = true
                excludePackages("org.luckypray.dexkit.demo.hook")
                matcher {
                    usingNumbers(114514)
                }
            }
            assert(firstResult.size == 1)

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            println(history)
            assert(history.droppedRecords == 0L)
            assert(history.records.size == 2)

            val firstRecord = history.records[0]
            assert(firstRecord.kind == QueryMetricsKind.FIND_METHOD)
            assert(firstRecord.priority == QueryMetricsPriority.NORMAL)

            val secondRecord = history.records[1]
            assert(secondRecord.kind == QueryMetricsKind.FIND_METHOD)
            assert(secondRecord.priority == QueryMetricsPriority.LATENCY_SENSITIVE)

            history.records.forEach { record ->
                assert(record.metrics.submittedTasks > 0)
                assert(record.metrics.dispatchedTasks > 0)
                assert(record.metrics.completedTasks <= record.metrics.submittedTasks)
                assert(record.metrics.dispatchedTasks <= record.metrics.submittedTasks)
                assert(record.metrics.baseDispatchedTasks + record.metrics.bonusDispatchedTasks == record.metrics.dispatchedTasks)
                assert(record.metrics.preprocessCompletedNs >= 0)
                assert(record.metrics.submissionCompletedNs >= record.metrics.preprocessCompletedNs)
                assert(record.metrics.workersCompletedNs >= record.metrics.submissionCompletedNs)
                assert(record.metrics.completedNs >= record.metrics.workersCompletedNs)
            }
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testConcurrentMixedQueryHistoryOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()

            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val futures = listOf(
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 2)
                    },
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            findFirst = true
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 1)
                    }
                )
                start.countDown()
                futures.forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            println(history)
            assert(history.records.size == 2)

            val recordsByPriority = history.records.associateBy { it.priority }
            val normal = recordsByPriority[QueryMetricsPriority.NORMAL]
                ?: error("Missing normal query metrics record")
            val latencySensitive = recordsByPriority[QueryMetricsPriority.LATENCY_SENSITIVE]
                ?: error("Missing latency-sensitive query metrics record")

            assert(normal.metrics.baseDispatchedTasks > 0)
            assert(normal.metrics.bonusDispatchedTasks == 0L)
            assert(normal.metrics.firstBonusDispatchDelayNs == -1L)
            assert(latencySensitive.metrics.baseDispatchedTasks > 0)
            assert(latencySensitive.metrics.bonusDispatchedTasks > 0)
            assert(latencySensitive.metrics.firstBonusDispatchDelayNs >= latencySensitive.metrics.firstDispatchDelayNs)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testLatencySensitiveBonusDispatchStartsAfterNormalBaseDispatch() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()
            parallelBridge.resetSchedulerMetrics()

            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val futures = listOf(
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 2)
                    },
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            findFirst = true
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 1)
                    }
                )
                start.countDown()

                var observedContention = false
                repeat(200) {
                    if (parallelBridge.getSchedulerMetricsSnapshot().maxVisibleQueryShareCount >= 2L) {
                        observedContention = true
                        return@repeat
                    }
                    Thread.sleep(10)
                }
                assert(observedContention)

                futures.forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            println(history)
            assert(history.records.size == 2)

            val recordsByPriority = history.records.associateBy { it.priority }
            val normal = recordsByPriority[QueryMetricsPriority.NORMAL]
                ?: error("Missing normal query metrics record")
            val latencySensitive = recordsByPriority[QueryMetricsPriority.LATENCY_SENSITIVE]
                ?: error("Missing latency-sensitive query metrics record")

            assert(normal.metrics.firstDispatchDelayNs >= 0)
            assert(normal.metrics.firstBonusDispatchDelayNs == -1L)
            assert(normal.metrics.maxQueryShareCount >= 2L)
            assert(latencySensitive.metrics.firstDispatchDelayNs >= 0)
            assert(latencySensitive.metrics.firstBonusDispatchDelayNs >= latencySensitive.metrics.firstDispatchDelayNs)
            assert(latencySensitive.metrics.maxQueryShareCount >= 2L)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testThreeWayConcurrentQueryHistoryOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(3)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()

            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(3)
            try {
                val futures = listOf(
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 2)
                    },
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 2)
                    },
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        val result = parallelBridge.findMethod {
                            findFirst = true
                            excludePackages("org.luckypray.dexkit.demo.hook")
                            matcher {
                                usingNumbers(114514)
                            }
                        }
                        assert(result.size == 1)
                    }
                )
                start.countDown()
                futures.forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            println(history)
            assert(history.records.size == 3)

            val normalRecords = history.records.filter { it.priority == QueryMetricsPriority.NORMAL }
            val latencySensitiveRecords = history.records.filter { it.priority == QueryMetricsPriority.LATENCY_SENSITIVE }

            assert(normalRecords.size == 2)
            assert(latencySensitiveRecords.size == 1)
            normalRecords.forEach { record ->
                assert(record.metrics.baseDispatchedTasks > 0)
                assert(record.metrics.bonusDispatchedTasks == 0L)
            }
            val latencySensitive = latencySensitiveRecords.single()
            assert(latencySensitive.metrics.baseDispatchedTasks > 0)
            assert(latencySensitive.metrics.bonusDispatchedTasks > 0)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testSharedSchedulerAdmissionOrderWithConcurrentCap() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(1)
            parallelBridge.setMaxConcurrentQueries(1)
            parallelBridge.setQueryMetricsEnabled(true)
            parallelBridge.resetQueryMetricsHistory()
            parallelBridge.resetSchedulerMetrics()

            val startFirst = CountDownLatch(1)
            val startSecond = CountDownLatch(1)
            val startThird = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(3)
            try {
                val first = executor.submit<Unit> {
                    startFirst.await(10, TimeUnit.SECONDS)
                    val result = parallelBridge.findClass {
                        matcher {
                            superClass("androidx.appcompat.app.AppCompatActivity")
                        }
                    }
                    assert(result.size == 2)
                }
                val second = executor.submit<Unit> {
                    startSecond.await(10, TimeUnit.SECONDS)
                    val result = parallelBridge.findMethod {
                        excludePackages("org.luckypray.dexkit.demo.hook")
                        matcher {
                            usingNumbers(114514)
                        }
                    }
                    assert(result.size == 2)
                }
                val third = executor.submit<Unit> {
                    startThird.await(10, TimeUnit.SECONDS)
                    val result = parallelBridge.findMethod {
                        findFirst = true
                        excludePackages("org.luckypray.dexkit.demo.hook")
                        matcher {
                            usingNumbers(114514)
                        }
                    }
                    assert(result.size == 1)
                }

                startFirst.countDown()
                var observedFirstDispatch = false
                repeat(200) {
                    if (parallelBridge.getSchedulerMetricsSnapshot().dispatchedTasks > 0L) {
                        observedFirstDispatch = true
                        return@repeat
                    }
                    Thread.sleep(10)
                }
                assert(observedFirstDispatch)

                startSecond.countDown()
                Thread.sleep(20)
                startThird.countDown()

                first.get(60, TimeUnit.SECONDS)
                second.get(60, TimeUnit.SECONDS)
                third.get(60, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
            }

            val history = parallelBridge.getQueryMetricsHistorySnapshot()
            println(history)
            assert(history.records.size == 3)

            val firstRecord = history.records[0]
            assert(firstRecord.kind == QueryMetricsKind.FIND_CLASS)
            assert(firstRecord.priority == QueryMetricsPriority.NORMAL)

            val secondRecord = history.records[1]
            assert(secondRecord.kind == QueryMetricsKind.FIND_METHOD)
            assert(secondRecord.priority == QueryMetricsPriority.NORMAL)

            val thirdRecord = history.records[2]
            assert(thirdRecord.kind == QueryMetricsKind.FIND_METHOD)
            assert(thirdRecord.priority == QueryMetricsPriority.LATENCY_SENSITIVE)
        }
    }

    @OptIn(DexKitExperimentalApi::class)
    @Test
    fun testQueryMetricsHistoryResetOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            parallelBridge.setQueryMetricsEnabled(true)

            val result = parallelBridge.findMethod {
                excludePackages("org.luckypray.dexkit.demo.hook")
                matcher {
                    usingNumbers(114514)
                }
            }
            assert(result.size == 2)

            val beforeReset = parallelBridge.getQueryMetricsHistorySnapshot()
            assert(beforeReset.droppedRecords == 0L)
            assert(beforeReset.records.size == 1)

            parallelBridge.resetQueryMetricsHistory()

            val afterReset = parallelBridge.getQueryMetricsHistorySnapshot()
            assert(afterReset.droppedRecords == 0L)
            assert(afterReset.records.isEmpty())
        }
    }
}
