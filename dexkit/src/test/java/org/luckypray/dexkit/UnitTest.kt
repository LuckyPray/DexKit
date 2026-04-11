package org.luckypray.dexkit

import org.junit.Test
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.UsingType
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class UnitTest {

    companion object {

        private val demoApkPath: String
        private var bridge: DexKitBridge
        private val tokenField = DexKitBridge::class.java.getDeclaredField("token").apply {
            isAccessible = true
        }
        private val nativeGetMethodUsingStringsMethod = DexKitBridge::class.java.getDeclaredMethod(
            "nativeGetMethodUsingStrings",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!
        ).apply {
            isAccessible = true
        }
        private val nativeGetMethodOpCodesMethod = DexKitBridge::class.java.getDeclaredMethod(
            "nativeGetMethodOpCodes",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!
        ).apply {
            isAccessible = true
        }

        init {
            loadLibrary("dexkit")
            val path = System.getProperty("apk.path")
            val demoApk = File(path, "demo.apk")
            demoApkPath = demoApk.absolutePath
            bridge = DexKitBridge.create(demoApk.absolutePath)
        }
    }

    private fun getBridgeToken(target: DexKitBridge): Long {
        return tokenField.getLong(target)
    }

    private fun nativeGetMethodUsingStrings(token: Long, encodeId: Long): List<String> {
        @Suppress("UNCHECKED_CAST")
        return (nativeGetMethodUsingStringsMethod.invoke(null, token, encodeId) as Array<String>).toList()
    }

    private fun nativeGetMethodOpCodes(token: Long, encodeId: Long): List<Int> {
        return (nativeGetMethodOpCodesMethod.invoke(null, token, encodeId) as IntArray).toList()
    }

    @Test
    fun testGetDexNum() {
        assert(bridge.getDexNum() > 0)
    }

    @Test
    fun testPackages() {
        bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            excludePackages("org.luckypray.dexkit.demo.annotations")
        }.forEach {
            println(it.name)
        }
    }

    @Test
    fun testGetParameterNames() {
        bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
            }
        }.forEach {
            println(it.descriptor)
            println("paramNames: ${it.paramNames?.joinToString(",")}")
        }
    }

    @Test
    fun testAnnotationSearch() {
        val res = bridge.findClass {
            matcher {
                annotations {
                    add {
                        this.type("Router", StringMatchType.EndsWith)
                    }
                }
            }
        }
        println(res.map { it.name })
        assert(res.size == 2)
        res.forEach {
            assert(it.name.startsWith("org.luckypray.dexkit.demo"))
            assert(it.name.endsWith("Activity"))
            assert(it.annotations.size == 1)
            assert(it.annotations.first().typeName == "org.luckypray.dexkit.demo.annotations.Router")
        }
    }

    @Test
    fun testAnnotationForValue() {
        val res = bridge.findClass {
            matcher {
                addAnnotation {
                    type("Router", StringMatchType.EndsWith)
                    addElement {
                        name = "path"
                        value {
                            stringValue("/main", StringMatchType.Equals)
                        }
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val mainActivity = res.first()
        assert(mainActivity.name == "org.luckypray.dexkit.demo.MainActivity")
        assert(mainActivity.superClass!!.name == "androidx.appcompat.app.AppCompatActivity")
        assert(mainActivity.interfaceCount == 1)
    }

    @Test
    fun testGetClassAnnotationsOnColdBridge() {
        DexKitBridge.create(demoApkPath).use { coldBridge ->
            val res = coldBridge.getClassData("Lorg/luckypray/dexkit/demo/MainActivity;")
            assert(res != null)
            val annotations = res!!.annotations
            assert(annotations.size == 1)
            val router = annotations.first()
            assert(router.typeName == "org.luckypray.dexkit.demo.annotations.Router")
            assert(router.elements.size == 1)
            val pathElement = router.elements.first()
            assert(pathElement.name == "path")
            assert(pathElement.value.stringValue() == "/main")
        }
    }

    @Test
    fun testClassFieldsSearch() {
        val res = bridge.findClass {
            matcher {
                fields {
                    addForType("java.lang.String")
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    count(3)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val playActivity = res.first()
        assert(playActivity.fields.size == 3)
        assert(playActivity.name == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassUsingString() {
        val res = bridge.findClass {
            matcher {
                usingStrings("PlayActivity")
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassUsingStringArray() {
        val res = bridge.findClass {
            matcher {
                usingStrings("PlayActivity", "onClick")
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testClassMatcherAnyOf() {
        val res = bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                anyOf {
                    match { className = "org.luckypray.dexkit.demo.MainActivity" }
                    match { className = "org.luckypray.dexkit.demo.PlayActivity" }
                }
            }
        }
        println(res)
        assert(res.size == 2)
        assert(res.map { it.name }.toSet() == setOf(
            "org.luckypray.dexkit.demo.MainActivity",
            "org.luckypray.dexkit.demo.PlayActivity"
        ))
    }

    @Test
    fun testClassMatcherAnyOfUsingStrings() {
        val res = bridge.findClass {
            matcher {
                anyOf {
                    match { usingStrings(listOf("PlayActivity"), StringMatchType.Contains) }
                    match { usingStrings(listOf("You rolled a "), StringMatchType.Contains) }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testClassMatcherNotAndAnyOfUsingStrings() {
        val res = bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                anyOf {
                    match { usingStrings(listOf("MainActivity"), StringMatchType.Contains) }
                    match { usingStrings(listOf("PlayActivity"), StringMatchType.Contains) }
                }
                not {
                    usingStrings(listOf("MainActivity"), StringMatchType.Contains)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassSuper() {
        val res = bridge.findClass {
            matcher {
                superClass("androidx.appcompat.app.AppCompatActivity")
            }
        }
        println(res)
        assert(res.size == 2)
        res.map { it.name }.forEach {
            assert(it.startsWith("org.luckypray.dexkit.demo"))
            assert(it.endsWith("Activity"))
        }
    }

    @Test
    fun testFindClassImpl() {
        val res = bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                superClass("AppCompatActivity", StringMatchType.EndsWith)
                interfaces {
                    add("android.view.View\$OnClickListener")
                    count(1)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.MainActivity")
    }

    @Test
    fun testFindClassImplIgnoreCase() {
        val res = bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                superClass("appcompatactivity", StringMatchType.EndsWith, true)
                interfaces {
                    add("android.view.View\$OnClicklistener", StringMatchType.Equals, true)
                    count(1)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().name == "org.luckypray.dexkit.demo.MainActivity")
    }

    @Test
    fun testIntNumberSearch() {
        val res = bridge.findMethod {
            excludePackages("org.luckypray.dexkit.demo.hook")
            matcher {
                usingNumbers {
                    add {
                        intValue(114514)
                    }
                }
            }
        }
        println(res)
        assert(res.size == 2)
    }

    @Test
    fun testIntAndFloatNumberSearch() {
        val res = bridge.findMethod {
            excludePackages("org.luckypray.dexkit.demo.hook")
            matcher {
                usingNumbers {
                    add {
                        floatValue(0.987f)
                    }
                    add {
                        intValue(114514)
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
    }

    @Test
    fun testMethodUsingFieldsMatcher() {
        val res = bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                usingNumbers {
                    add {
                        intValue(114514)
                    }
                }
                usingFields {
                    add {
                        declaredClass = "org.luckypray.dexkit.demo.PlayActivity"
                        type = "android.os.Handler"
                        usingType = UsingType.Any
                    }
                }
            }
        }
        println(res)
        assert(res.isNotEmpty())
        assert(res.all { it.className == "org.luckypray.dexkit.demo.PlayActivity" })
    }

    @Test
    fun testFieldReadMethodsMatcher() {
        val res = bridge.findField {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                type("android.os.Handler")
                addReadMethod {
                    usingNumbers {
                        add {
                            intValue(114514)
                        }
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.PlayActivity")
        assert(res.first().typeName == "android.os.Handler")
    }

    @Test
    fun testFieldWriteMethodsMatcher() {
        val res = bridge.findField {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                type("android.widget.TextView")
                addWriteMethod {
                    name = "onCreate"
                    paramTypes("android.os.Bundle")
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.PlayActivity")
        assert(res.first().typeName == "android.widget.TextView")
    }

    @Test
    fun testFieldMatcherAnyOf() {
        val res = bridge.findField {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                anyOf {
                    match { type = "android.os.Handler" }
                    match { type = "android.widget.TextView" }
                }
            }
        }
        println(res)
        assert(res.size == 2)
        assert(res.map { it.typeName }.toSet() == setOf("android.os.Handler", "android.widget.TextView"))
    }

    @Test
    fun testMethodMatcherNotAndAnyOf() {
        val res = bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                anyOf {
                    match { name = "onCreate" }
                    match { usingStrings(listOf("rollDice: "), StringMatchType.Contains) }
                }
                not {
                    usingStrings(listOf("onCreate"), StringMatchType.Contains)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val methodData = bridge.getMethodData(res.first().descriptor)!!
        assert(methodData.usingStrings.contains("rollDice: "))
        assert(!methodData.usingStrings.contains("onCreate"))
    }

    @Test
    fun testMethodMatcherAnyOfUsingStrings() {
        val res = bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                anyOf {
                    match { usingStrings(listOf("rollDice: "), StringMatchType.Contains) }
                    match { usingStrings(listOf("You rolled a "), StringMatchType.Contains) }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val methodData = bridge.getMethodData(res.first().descriptor)!!
        assert(methodData.className == "org.luckypray.dexkit.demo.PlayActivity")
        assert(methodData.usingStrings.contains("rollDice: "))
        assert(methodData.usingStrings.contains("You rolled a "))
    }

    @Test
    fun testCompositeStringMatcherWithinUsingStrings() {
        val res = bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
                name = "onCreate"
                usingStrings {
                    add {
                        allOf {
                            match("Play", StringMatchType.StartsWith)
                            match("Activity", StringMatchType.EndsWith)
                        }
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().descriptor == "Lorg/luckypray/dexkit/demo/PlayActivity;->onCreate(Landroid/os/Bundle;)V")
    }

    @Test
    fun testGetClassData() {
        val res = bridge.getClassData("Lorg/luckypray/dexkit/demo/MainActivity;")
        assert(res != null)
        assert(res!!.name == "org.luckypray.dexkit.demo.MainActivity")
        res.methods.forEach {
            println(it.descriptor)
        }
    }

    @Test
    fun testGetConstructorData() {
        val res = bridge.getMethodData("Lorg/luckypray/dexkit/demo/MainActivity;-><init>()V")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.methodName == "<init>")
        assert(res.methodSign == "()V")
        assert(res.isConstructor)
    }

    @Test
    fun testGetMethodData() {
        val res = bridge.getMethodData("Lorg/luckypray/dexkit/demo/MainActivity;->onClick(Landroid/view/View;)V")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.methodName == "onClick")
        assert(res.methodSign == "(Landroid/view/View;)V")
        assert(res.isMethod)
    }

    @Test
    fun testGetFieldData() {
        val res = bridge.getFieldData("Lorg/luckypray/dexkit/demo/MainActivity;->TAG:Ljava/lang/String;")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.fieldName == "TAG")
        assert(res.typeName == "java.lang.String")
    }

    @Test
    fun testGetMethodUsingStrings() {
        val res = bridge.getMethodData("Lorg/luckypray/dexkit/demo/PlayActivity;->onCreate(Landroid/os/Bundle;)V")
        assert(res != null)
        val usingStrings = res!!.usingStrings
        assert(usingStrings.size == 2)
        usingStrings.containsAll(listOf("onCreate", "PlayActivity"))
    }

    @Test
    fun testMethodUsingNumbers() {
        val cls = bridge.findMethod {
            excludePackages("org.luckypray.dexkit.demo.hook")
            matcher {
                usingNumbers(0, -1, 0.01, 0.987, 114514)
            }
        }.single()
        assert(cls.className == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testConcurrentFindMethodOnSharedBridge() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = parallelBridge.findMethod {
                                excludePackages("org.luckypray.dexkit.demo.hook")
                                matcher {
                                    usingNumbers(114514)
                                }
                            }
                            assert(result.size == 2)
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

    @Test
    fun testConcurrentFindFirstMethodOnSharedBridge() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
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
    fun testConcurrentFindFirstMethodOnSharedBridgeWithSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
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
    fun testMixedFindFirstAndRegularQueriesOnSharedScheduler() {
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
        }
    }


    @Test
    fun testConcurrentBatchFindClassUsingStringsOnSharedBridge() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = parallelBridge.batchFindClassUsingStrings {
                                searchPackages("org.luckypray.dexkit.demo")
                                groups(
                                    mapOf(
                                        "main_activity" to listOf("onClick: playButton"),
                                        "play_activity" to listOf("onClick: rollButton")
                                    )
                                )
                            }
                            assert(result["main_activity"]?.size == 1)
                            assert(result["play_activity"]?.size == 1)
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

    @Test
    fun testConcurrentBatchFindMethodUsingStringsOnSharedBridge() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = parallelBridge.batchFindMethodUsingStrings {
                                searchPackages("org.luckypray.dexkit.demo")
                                groups(
                                    mapOf(
                                        "main_on_click" to listOf("onClick: playButton"),
                                        "play_on_click" to listOf("onClick: rollButton")
                                    )
                                )
                            }
                            assert(result["main_on_click"]?.size == 1)
                            assert(result["play_on_click"]?.size == 1)
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
    fun testConcurrentBatchFindMethodUsingStringsOnSharedScheduler() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            parallelBridge.setThreadNum(2)
            parallelBridge.setMaxConcurrentQueries(2)
            val workers = 4
            val iterationsPerWorker = 8
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = parallelBridge.batchFindMethodUsingStrings {
                                searchPackages("org.luckypray.dexkit.demo")
                                groups(
                                    mapOf(
                                        "main_on_click" to listOf("onClick: playButton"),
                                        "play_on_click" to listOf("onClick: rollButton")
                                    )
                                )
                            }
                            assert(result["main_on_click"]?.size == 1)
                            assert(result["play_on_click"]?.size == 1)
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

    @Test
    fun testConcurrentNativeGetMethodUsingStringsWithoutBridgeSynchronization() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            val method = parallelBridge.getMethodData("Lorg/luckypray/dexkit/demo/PlayActivity;->onCreate(Landroid/os/Bundle;)V")
            assert(method != null)
            val encodeId = method!!.getEncodeId()
            val token = getBridgeToken(parallelBridge)
            val workers = 6
            val iterationsPerWorker = 16
            val start = CountDownLatch(1)
            val baseline = AtomicReference<List<String>?>(null)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = nativeGetMethodUsingStrings(token, encodeId)
                            assert(result.size == 2)
                            assert(result.containsAll(listOf("onCreate", "PlayActivity")))
                            val current = baseline.get()
                            if (current == null) {
                                baseline.compareAndSet(null, result)
                            } else {
                                assert(result == current)
                            }
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

    @Test
    fun testConcurrentNativeGetMethodOpCodesWithoutBridgeSynchronization() {
        DexKitBridge.create(demoApkPath).use { parallelBridge ->
            val method = parallelBridge.getMethodData("Lorg/luckypray/dexkit/demo/MainActivity;->onClick(Landroid/view/View;)V")
            assert(method != null)
            val encodeId = method!!.getEncodeId()
            val token = getBridgeToken(parallelBridge)
            val workers = 6
            val iterationsPerWorker = 16
            val start = CountDownLatch(1)
            val baseline = AtomicReference<List<Int>?>(null)
            val executor = Executors.newFixedThreadPool(workers)
            try {
                val futures = (0 until workers).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(iterationsPerWorker) {
                            val result = nativeGetMethodOpCodes(token, encodeId)
                            assert(result.isNotEmpty())
                            val current = baseline.get()
                            if (current == null) {
                                baseline.compareAndSet(null, result)
                            } else {
                                assert(result == current)
                            }
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
}
