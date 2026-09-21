package org.luckypray.dexkit

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.UsingType
import org.luckypray.dexkit.query.matchers.MethodMatcher
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
    fun testBatchFindClassUsingStringsSearchIn() {
        val groupName = "SearchGroup"
        val playActivity = bridge.getClassData("org.luckypray.dexkit.demo.PlayActivity")!!
        val mainActivity = bridge.getClassData("org.luckypray.dexkit.demo.MainActivity")!!

        val baseline = bridge.batchFindClassUsingStrings {
            addSearchGroup(groupName, listOf("PlayActivity"), StringMatchType.Contains, true)
        }
        assert(baseline[groupName]!!.map { it.descriptor } == listOf(playActivity.descriptor))

        val inSelf = bridge.batchFindClassUsingStrings {
            searchIn(listOf(playActivity))
            addSearchGroup(groupName, listOf("PlayActivity"), StringMatchType.Contains, true)
        }
        assert(inSelf[groupName]!!.map { it.descriptor } == listOf(playActivity.descriptor))

        val inOther = bridge.batchFindClassUsingStrings {
            searchIn(listOf(mainActivity))
            addSearchGroup(groupName, listOf("PlayActivity"), StringMatchType.Contains, true)
        }
        assert(inOther[groupName]!!.isEmpty())
    }

    @Test
    fun testBatchFindMethodUsingStringsSearchInScope() {
        val groupName = "SearchGroup"
        val groups = mapOf(groupName to listOf("getRandomDice: "))
        val randomUtil = bridge.getClassData("org.luckypray.dexkit.demo.RandomUtil")!!
        val mainActivity = bridge.getClassData("org.luckypray.dexkit.demo.MainActivity")!!
        // demo release apk is minified: method names are obfuscated, locate by used string
        val hit = bridge.findMethod {
            excludePackages("org.luckypray.dexkit.demo.hook")
            matcher {
                declaredClass("org.luckypray.dexkit.demo.RandomUtil")
                usingStrings("getRandomDice: ")
            }
        }.single()
        val unrelated = mainActivity.methods.single { it.name == "onCreate" }

        // no scope, the hook package also uses this literal, so exclude it
        val baseline = bridge.batchFindMethodUsingStrings {
            excludePackages("org.luckypray.dexkit.demo.hook")
            groups(groups)
        }
        assert(baseline[groupName]!!.map { it.descriptor } == listOf(hit.descriptor))

        // in_classes
        val inDeclaringClass = bridge.batchFindMethodUsingStrings {
            searchInClasses(listOf(randomUtil))
            groups(groups)
        }
        assert(inDeclaringClass[groupName]!!.map { it.descriptor } == listOf(hit.descriptor))

        val inOtherClass = bridge.batchFindMethodUsingStrings {
            searchInClasses(listOf(mainActivity))
            groups(groups)
        }
        assert(inOtherClass[groupName]!!.isEmpty())

        // in_methods
        val inHitMethod = bridge.batchFindMethodUsingStrings {
            searchInMethods(listOf(hit))
            groups(groups)
        }
        assert(inHitMethod[groupName]!!.map { it.descriptor } == listOf(hit.descriptor))

        val inUnrelatedMethod = bridge.batchFindMethodUsingStrings {
            searchInMethods(listOf(unrelated))
            groups(groups)
        }
        assert(inUnrelatedMethod[groupName]!!.isEmpty())
    }

    @Test
    fun testBatchStringsMatchOrdinaryQueriesAcrossModesAndEmptyStrings() {
        DexKitBridge.create(demoApkPath).use { target ->
            val groups = mapOf(
                "missing" to listOf("dexkit-no-such-literal-9187"),
                "hit" to listOf("PlayActivity"),
                "empty" to listOf(""),
                "case" to listOf("playactivity"),
                "and" to listOf("PlayActivity", "onCreate")
            )
            for (type in StringMatchType.values()) for (ignoreCase in listOf(false, true)) {
                val batch = target.batchFindMethodUsingStrings { groups(groups, type, ignoreCase) }
                assert(batch.keys == groups.keys)
                groups.forEach { (key, words) ->
                    val ordinary = target.findMethod {
                        matcher { usingStrings(words, type, ignoreCase) }
                    }
                    assert(batch[key]!!.map { it.descriptor }.sorted() == ordinary.map { it.descriptor }.sorted())
                }
            }
        }
    }

    @Test
    fun testAnchoredStringsMatchExplicitModes() {
        val cases = listOf(
            Triple("^PlayActivity$", "PlayActivity", StringMatchType.Equals),
            Triple("^Play", "Play", StringMatchType.StartsWith),
            Triple("Activity$", "Activity", StringMatchType.EndsWith),
            Triple("Play", "Play", StringMatchType.Contains),
            Triple("^playactivity$", "playactivity", StringMatchType.Equals),
            Triple("^play", "play", StringMatchType.StartsWith),
            Triple("^dexkit-no-such-literal-9187$", "dexkit-no-such-literal-9187", StringMatchType.Equals),
            Triple("^dexkit-no-such-prefix-9187", "dexkit-no-such-prefix-9187", StringMatchType.StartsWith),
            Triple("^\u4e2d$", "\u4e2d", StringMatchType.Equals),
            Triple("^$", "", StringMatchType.Equals),
            Triple("^", "", StringMatchType.StartsWith),
            Triple("$", "", StringMatchType.EndsWith)
        )
        for (fullCache in listOf(false, true)) {
            DexKitBridge.create(demoApkPath).use { target ->
                if (fullCache) target.initFullCache()
                for ((pattern, value, type) in cases) for (ignoreCase in listOf(false, true)) {
                    val label = "$pattern, ignoreCase=$ignoreCase, fullCache=$fullCache"
                    val methods = target.findMethod {
                        matcher { usingStrings(listOf(pattern), StringMatchType.SimilarRegex, ignoreCase) }
                    }.map { it.descriptor }.sorted()
                    val expectedMethods = target.findMethod {
                        matcher { usingStrings(listOf(value), type, ignoreCase) }
                    }.map { it.descriptor }.sorted()
                    assertEquals(label, expectedMethods, methods)
                    if (pattern == "^PlayActivity$" || pattern == "^Play") assertTrue(label, methods.isNotEmpty())

                    val classes = target.findClass {
                        matcher { usingStrings(listOf(pattern), StringMatchType.SimilarRegex, ignoreCase) }
                    }.map { it.name }.sorted()
                    val expectedClasses = target.findClass {
                        matcher { usingStrings(listOf(value), type, ignoreCase) }
                    }.map { it.name }.sorted()
                    assertEquals(label, expectedClasses, classes)
                }
            }
        }
    }

    @Test
    fun testRootRangeSeedChecksRemainingStringsAndLogicalConditions() {
        val cases: List<MethodMatcher.() -> Unit> = listOf(
            { usingEqStrings("PlayActivity", "onCreate") },
            { usingEqStrings("PlayActivity", "MainActivity") },
            { addEqString("PlayActivity"); addUsingString("onC", StringMatchType.StartsWith) },
            { addEqString("PlayActivity"); addUsingString("rollDice:") },
            { addEqString("PlayActivity"); addUsingString("dexkit-absent-condition-9246") },
            {
                addUsingString("^PlayActivity$", StringMatchType.SimilarRegex)
                addUsingString("^onC", StringMatchType.SimilarRegex)
                anyOf {
                    match { name("onCreate") }
                    match { name("dexkit-absent-method-9246") }
                }
            }
        )
        for (fullCache in listOf(false, true)) {
            DexKitBridge.create(demoApkPath).use { target ->
                if (fullCache) target.initFullCache()
                cases.forEachIndexed { index, configure ->
                    val actual = target.findMethod {
                        matcher { configure(); returnType("void") }
                    }.map { it.descriptor }.sorted()
                    // The nested predicate uses the established full matcher,
                    // without supplying a root string candidate seed.
                    val expected = target.findMethod {
                        matcher { returnType("void"); allOf { match { configure() } } }
                    }.map { it.descriptor }.sorted()
                    assertEquals("case=$index, fullCache=$fullCache", expected, actual)
                    if (index == 0) assertTrue(actual.isNotEmpty())
                    if (index == 1 || index == 4) assertTrue(actual.isEmpty())
                }
            }
        }
    }

    @Test
    fun testRootRangeSeedsPreserveDuplicateTextAndCaseSemantics() {
        val cases: List<MethodMatcher.() -> Unit> = listOf(
            { addEqString("Activity"); addUsingString("Activity") },
            { addUsingString("Activity"); addEqString("Activity") },
            { addEqString("playactivity"); addUsingString("playactivity", StringMatchType.Equals, true) },
            { addUsingString("playactivity", StringMatchType.Equals, true); addEqString("PlayActivity") },
            { addEqString("PlayActivity"); addEqString("PlayActivity") },
            { addUsingString("^Activity$", StringMatchType.SimilarRegex); addUsingString("Activity") }
        )
        // Preserve the existing flat keyword matcher's duplicate-text behavior.
        // Logical-group prefilters have separate atom semantics and are not an
        // equivalent oracle for these ambiguous duplicate specifications.
        val equivalents: List<MethodMatcher.() -> Unit> = listOf(
            { addUsingString("Activity") },
            { addEqString("Activity") },
            { addUsingString("playactivity", StringMatchType.Equals, true) },
            { addEqString("PlayActivity") },
            { addEqString("PlayActivity") },
            { addUsingString("Activity") }
        )
        DexKitBridge.create(demoApkPath).use { target ->
            cases.forEachIndexed { index, configure ->
                val expected = target.findMethod {
                    matcher { equivalents[index]() }
                }.map { it.descriptor }.sorted()
                val actual = target.findMethod {
                    matcher { configure() }
                }.map { it.descriptor }.sorted()
                assertEquals("duplicate/case=$index", expected, actual)
            }
        }
    }

    @Test
    fun testClassRangeSeedsAllowStringsInDifferentMethods() {
        DexKitBridge.create(demoApkPath).use { target ->
            val play = target.getClassData("org.luckypray.dexkit.demo.PlayActivity")!!
            val usedStrings = play.methods.map { it.usingStrings }
            val roll = usedStrings.flatten().first { it.startsWith("rollDice:") }
            assertTrue(usedStrings.none { it.contains("onCreate") && it.contains(roll) })
            val actual = target.findClass {
                matcher { usingEqStrings("onCreate", roll) }
            }.map { it.name }.sorted()
            val expected = target.findClass {
                matcher { allOf { match { usingEqStrings("onCreate", roll) } } }
            }.map { it.name }.sorted()
            assertEquals(expected, actual)
            assertTrue(actual.contains(play.name))
        }
    }

    @Test
    fun testRootStringCandidatesIntersectExplicitAndPackageScopes() {
        val cases: List<MethodMatcher.() -> Unit> = listOf(
            { addEqString("PlayActivity"); addUsingString("onC", StringMatchType.StartsWith) },
            { usingStrings("PlayActivity", "onCreate") },
            { addUsingString("Activity", StringMatchType.EndsWith) }
        )
        DexKitBridge.create(demoApkPath).use { target ->
            val play = target.getClassData("org.luckypray.dexkit.demo.PlayActivity")!!
            val selected = play.methods.filter { it.name == "onCreate" }
            assertTrue(selected.isNotEmpty())
            cases.forEachIndexed { index, configure ->
                val all = target.findMethod { matcher { configure() } }
                val expected = all.filter { value -> selected.any { it.descriptor == value.descriptor } }
                    .map { it.descriptor }.sorted()
                val actual = target.findMethod {
                    searchPackages("org.luckypray.dexkit.demo")
                    excludePackages("org.luckypray.dexkit.demo.hook")
                    searchInClass(listOf(play))
                    searchInMethod(selected)
                    matcher { configure() }
                }.map { it.descriptor }.sorted()
                assertTrue("scope=$index should have a witness", expected.isNotEmpty())
                assertEquals("scope=$index", expected, actual)
                assertTrue("excluded method scope=$index", target.findMethod {
                    searchPackages("org.luckypray.dexkit.demo")
                    excludePackages("org.luckypray.dexkit.demo")
                    searchInClass(listOf(play))
                    searchInMethod(selected)
                    matcher { configure() }
                }.isEmpty())
                assertTrue(target.findMethod {
                    searchInMethod(emptyList())
                    matcher { configure() }
                }.isEmpty())
            }
            for (matchType in listOf(StringMatchType.Equals, StringMatchType.Contains)) {
                val scoped = target.findClass {
                    searchPackages("org.luckypray.dexkit.demo")
                    searchIn(listOf(play))
                    matcher { addUsingString("PlayActivity", matchType) }
                }.map { it.name }
                assertEquals("class scope=$matchType", listOf(play.name), scoped)
                assertTrue("excluded class scope=$matchType", target.findClass {
                    searchPackages("org.luckypray.dexkit.demo")
                    excludePackages("org.luckypray.dexkit.demo")
                    searchIn(listOf(play))
                    matcher { addUsingString("PlayActivity", matchType) }
                }.isEmpty())
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

    @Test
    fun testOpCodesEndsWith() {
        // A method whose last opcode also occurs earlier in the sequence is
        // the regression case: EndsWith must anchor at the tail, not at the
        // first occurrence found when scanning from the start.
        val method = bridge.findMethod {
            searchPackages("org.luckypray.dexkit.demo")
        }.first {
            val ops = it.opCodes
            ops.size >= 2 && ops.indexOf(ops.last()) != ops.size - 1
        }
        val opCodes = method.opCodes
        for (len in 1..opCodes.size) {
            val res = bridge.findMethod {
                matcher {
                    opCodes(opCodes.takeLast(len), OpCodeMatchType.EndsWith)
                }
            }
            assert(res.any { it.getEncodeId() == method.getEncodeId() })
        }
    }

    @Test
    fun testLazyMetadataMatchesFullCacheIncludingEmptyMethods() {
        DexKitBridge.create(demoApkPath).use { target ->
            val token = getBridgeToken(target)
            val methods = target.findMethod {
                searchPackages("org.luckypray.dexkit.demo")
            }
            assert(methods.isNotEmpty())
            val before = methods.associate { method ->
                val id = method.getEncodeId()
                id to Pair(nativeGetMethodUsingStrings(token, id), nativeGetMethodOpCodes(token, id))
            }
            // Router's abstract annotation members have no code; empty is a value.
            assert(before.values.any { it.second.isEmpty() })
            assert(before.values.any { it.first.isNotEmpty() && it.second.isNotEmpty() })
            target.initFullCache()
            before.forEach { (id, expected) ->
                // Bypass MethodData's Kotlin lazy properties to read native state again.
                assert(nativeGetMethodUsingStrings(token, id) == expected.first)
                assert(nativeGetMethodOpCodes(token, id) == expected.second)
            }
        }
    }

    @Test
    fun testCompactRowsSurvivePartialWarmupInEitherOrder() {
        for (stringsFirst in listOf(true, false)) {
            DexKitBridge.create(demoApkPath).use { target ->
                val methods = target.findMethod {
                    searchPackages("org.luckypray.dexkit.demo")
                }
                assertTrue(methods.isNotEmpty())
                // Include preceding IDs to exercise gaps outside declared method lists.
                val ids = methods.flatMap {
                    val id = it.getEncodeId()
                    if ((id and 0xffffffffL) == 0L) listOf(id) else listOf(id - 1, id)
                }.distinct().sorted()
                val token = getBridgeToken(target)
                fun strings() = ids.associateWith { nativeGetMethodUsingStrings(token, it) }
                fun opcodes() = ids.associateWith { nativeGetMethodOpCodes(token, it) }
                fun invokes() = ids.associateWith { id ->
                    target.getInvokeMethods(id).map { it.getEncodeId() }
                }
                fun callers() = ids.associateWith { id ->
                    target.getCallMethods(id).map { it.getEncodeId() }
                }
                fun warmStrings() {
                    target.findMethod { matcher { usingStrings("PlayActivity") } }
                }

                val expectedStrings = strings()
                val expectedOpcodes = opcodes()
                assertTrue(expectedOpcodes.values.any { it.isEmpty() })
                assertTrue(expectedStrings.values.any { it.isNotEmpty() })
                if (stringsFirst) warmStrings()
                val expectedInvokes = invokes()
                val expectedCallers = callers()
                if (!stringsFirst) warmStrings()

                // Fresh native reads bypass MethodData's lazy properties. List
                // equality checks order and duplicate entries, not only membership.
                assertEquals(expectedStrings, strings())
                assertEquals(expectedInvokes, invokes())
                assertEquals(expectedCallers, callers())
                target.initFullCache()
                assertEquals(expectedStrings, strings())
                assertEquals(expectedOpcodes, opcodes())
                assertEquals(expectedInvokes, invokes())
                assertEquals(expectedCallers, callers())
            }
        }
    }

    @Test
    fun testFieldReverseRowsSurviveWarmupAndConcurrentReads() {
        val fieldIds: List<Long>
        val methodIds: List<Long>
        fun rows(target: DexKitBridge, ids: List<Long>) = ids.associateWith { id ->
            // Read through the bridge each time, bypassing FieldData's lazy values.
            target.readFieldMethods(id).map { it.getEncodeId() } to
                    target.writeFieldMethods(id).map { it.getEncodeId() }
        }
        val expected: Map<Long, Pair<List<Long>, List<Long>>>
        DexKitBridge.create(demoApkPath).use { reference ->
            fieldIds = reference.findField {
                searchPackages("org.luckypray.dexkit.demo")
            }.map { it.getEncodeId() }
            methodIds = reference.findMethod {
                searchPackages("org.luckypray.dexkit.demo")
            }.map { it.getEncodeId() }
            reference.initFullCache()
            expected = rows(reference, fieldIds)
        }
        assertTrue(fieldIds.isNotEmpty() && methodIds.isNotEmpty())
        assertTrue(expected.values.any { it.first.isNotEmpty() })
        assertTrue(expected.values.any { it.second.isNotEmpty() })
        assertTrue(expected.values.any { it.first.isEmpty() || it.second.isEmpty() })
        for (schedule in 0..3) {
            DexKitBridge.create(demoApkPath).use { target ->
                if (schedule == 1) methodIds.forEach { target.getMethodUsingFields(it) }
                if (schedule == 2) methodIds.forEach { target.getCallMethods(it) }
                if (schedule == 3) {
                    val start = CountDownLatch(1)
                    val executor = Executors.newFixedThreadPool(3)
                    try {
                        val readers = executor.submit<Unit> {
                            start.await(10, TimeUnit.SECONDS)
                            assertEquals(expected, rows(target, fieldIds))
                        }
                        val callers = executor.submit<Unit> {
                            start.await(10, TimeUnit.SECONDS)
                            methodIds.forEach { target.getCallMethods(it) }
                        }
                        val warmup = executor.submit<Unit> {
                            start.await(10, TimeUnit.SECONDS)
                            target.initFullCache()
                        }
                        start.countDown()
                        listOf(readers, callers, warmup).forEach { it.get(60, TimeUnit.SECONDS) }
                    } finally {
                        executor.shutdownNow()
                    }
                }
                // List equality preserves occurrence order and repeated uses.
                assertEquals(expected, rows(target, fieldIds))
                target.initFullCache()
                assertEquals(expected, rows(target, fieldIds))
            }
        }
    }

    @Test
    fun testColdMetadataReadersRaceFullWarmup() {
        val descriptor = "Lorg/luckypray/dexkit/demo/PlayActivity;->onCreate(Landroid/os/Bundle;)V"
        val expectedStrings: List<String>
        val expectedOpcodes: List<Int>
        DexKitBridge.create(demoApkPath).use { reference ->
            val id = reference.getMethodData(descriptor)!!.getEncodeId()
            expectedStrings = nativeGetMethodUsingStrings(getBridgeToken(reference), id)
            expectedOpcodes = nativeGetMethodOpCodes(getBridgeToken(reference), id)
        }
        DexKitBridge.create(demoApkPath).use { target ->
            val id = target.getMethodData(descriptor)!!.getEncodeId()
            val token = getBridgeToken(target)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(7)
            try {
                val readers = (0 until 6).map {
                    executor.submit<Unit> {
                        start.await(10, TimeUnit.SECONDS)
                        repeat(16) {
                            assert(nativeGetMethodUsingStrings(token, id) == expectedStrings)
                            assert(nativeGetMethodOpCodes(token, id) == expectedOpcodes)
                        }
                    }
                }
                val warmup = executor.submit<Unit> {
                    start.await(10, TimeUnit.SECONDS)
                    target.initFullCache()
                }
                start.countDown()
                (readers + warmup).forEach { it.get(60, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
