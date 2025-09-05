@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")

package org.luckypray.dexkit

import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.BatchFindClassUsingStrings
import org.luckypray.dexkit.query.BatchFindMethodUsingStrings
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import org.luckypray.dexkit.wrap.ISerializable
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@DexKitExperimentalApi
object DexKitCacheBridge {
    private lateinit var cache: Cache
    private val scheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1) { r ->
        Thread(r, "DexKit-Reaper").apply { isDaemon = true }
    }.apply {
        removeOnCancelPolicy = true
    }
    private val pool = ConcurrentHashMap<String, RecyclableBridge>()
    private val lock = ReentrantReadWriteLock()
    @JvmStatic
    var idleTimeoutMillis: Long = 5_000L

    @JvmStatic
    fun init(cache: Cache) {
        this.cache = cache
    }

    @JvmStatic
    fun create(appTag: String, path: String): RecyclableBridge {
        require(::cache.isInitialized) { "Wrapper must be init(cache) first" }
        pool[appTag]?.let { return it }
        val newBridge = RecyclableBridge.create(appTag, path)
        val prev = pool.putIfAbsent(appTag, newBridge)
        return prev ?: newBridge
    }

    @JvmStatic
    fun create(appTag: String, dexArray: Array<ByteArray>): RecyclableBridge {
        require(::cache.isInitialized) { "Wrapper must be init(cache) first" }
        pool[appTag]?.let { return it }
        val newBridge = RecyclableBridge.create(appTag, dexArray)
        val prev = pool.putIfAbsent(appTag, newBridge)
        return prev ?: newBridge
    }

    @JvmStatic
    fun create(appTag: String, classLoader: ClassLoader): RecyclableBridge {
        require(::cache.isInitialized) { "Wrapper must be init(cache) first" }
        pool[appTag]?.let { return it }
        val newBridge = RecyclableBridge.create(appTag, classLoader)
        val prev = pool.putIfAbsent(appTag, newBridge)
        return prev ?: newBridge
    }

    @JvmStatic
    fun clearCache(appTag: String) {
        lock.write {
            val prefix = "$appTag:"
            cache.getAllKeys().forEach {
                if (it.startsWith(prefix)) {
                    cache.remove(it)
                }
            }
        }
    }

    @JvmStatic
    fun clearAllCache() {
        lock.write { cache.clearAll() }
    }

    interface Cache {
        fun get(key: String, default: String?): String?
        fun put(key: String, value: String)
        fun getList(key: String, default: List<String>?): List<String>?
        fun putList(key: String, value: List<String>)
        fun remove(key: String)
        fun getAllKeys(): Collection<String>
        fun clearAll()
    }

    class RecyclableBridge private constructor(
        private val appTag: String,
        private val path: String? = null,
        private val dexArray: Array<ByteArray>? = null,
        private val classLoader: ClassLoader? = null,
    ) : Closeable {

        companion object {
            private const val CACHE_NULL = "CACHE_NULL"

            @JvmSynthetic
            internal fun create(
                appTag: String,
                path: String
            ): RecyclableBridge = RecyclableBridge(appTag, path, null, null)

            @JvmSynthetic
            internal fun create(
                appTag: String,
                dexArray: Array<ByteArray>
            ): RecyclableBridge = RecyclableBridge(appTag, null, dexArray, null)

            @JvmSynthetic
            internal fun create(
                appTag: String,
                classLoader: ClassLoader
            ): RecyclableBridge = RecyclableBridge(appTag, null, null, classLoader)
        }

        private val activeCalls = AtomicInteger(0)
        private var reaperFuture: ScheduledFuture<*>? = null
        @Volatile
        private var _bridge: DexKitBridge? = null

        private fun createBridge(): DexKitBridge {
            return when {
                path != null -> DexKitBridge.create(path)
                dexArray != null -> DexKitBridge.create(dexArray)
                classLoader != null -> DexKitBridge.create(classLoader, true)
                else -> error("init fail")
            }
        }

        private fun beginUse() {
            reaperFuture?.cancel(false)
            activeCalls.incrementAndGet()
        }

        private fun endUse() {
            if (activeCalls.decrementAndGet() == 0) {
                reaperFuture = scheduler.schedule(reaper, idleTimeoutMillis, TimeUnit.MILLISECONDS)
            }
        }

        fun interface BridgeFunction {
            fun apply(bridge: DexKitBridge)
        }

        fun withBridge(action: BridgeFunction) {
            acquireBridge { b -> action.apply(b) }
        }

        @JvmSynthetic
        fun withBridge(action: (DexKitBridge) -> Unit) {
            acquireBridge { b -> action(b) }
        }

        private inline fun <R> acquireBridge(block: (DexKitBridge) -> R): R = acquire {
            var b = _bridge
            if (b == null) synchronized(RecyclableBridge::class.java) {
                b = _bridge ?: createBridge().also { _bridge = it }
            }
            block(b!!)
        }

        private inline fun <R> acquire(block: RecyclableBridge.() -> R): R {
            beginUse()
            return try {
                block()
            } finally {
                endUse()
            }
        }

        private val bridge: DexKitBridge
            get() = acquire {
                _bridge ?: createBridge().also { _bridge = it }
            }

        override fun close() {
            reaperFuture?.cancel(false)
            activeCalls.set(0)
            _bridge?.close()
            _bridge = null
        }

        private val reaper = Runnable {
            close()
        }

        // region fun interface

        fun interface FindMethodBuilder {
            fun build(f: FindMethod)
        }

        fun interface FindClassBuilder {
            fun build(f: FindClass)
        }

        fun interface FindFieldBuilder {
            fun build(f: FindField)
        }

        fun interface BatchFindMethodUsingStringsBuilder {
            fun build(f: BatchFindMethodUsingStrings)
        }

        fun interface BatchFindClassUsingStringsBuilder {
            fun build(f: BatchFindClassUsingStrings)
        }

        fun interface BridgeMethodBuilder {
            fun build(b: DexKitBridge): MethodData
        }

        fun interface BridgeClassBuilder {
            fun build(b: DexKitBridge): ClassData
        }

        fun interface BridgeFieldBuilder {
            fun build(b: DexKitBridge): FieldData
        }

        fun interface BridgeMethodsBuilder {
            fun build(b: DexKitBridge): List<MethodData>
        }

        fun interface BridgeClassesBuilder {
            fun build(b: DexKitBridge): List<ClassData>
        }

        fun interface BridgeFieldsBuilder {
            fun build(b: DexKitBridge): List<FieldData>
        }

        // endregion

        // region java SAM auto gen key

        fun getMethod(
            query: FindMethodBuilder
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getClass(
            query: FindClassBuilder
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getField(
            query: FindFieldBuilder
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getMethods(
            query: FindMethodBuilder
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getClasses(
            query: FindClassBuilder
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getFields(
            query: FindFieldBuilder
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getBatchUsingStringsMethods(
            query: BatchFindMethodUsingStringsBuilder
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = query.toQuery()
        )

        fun getBatchUsingStringsClasses(
            query: BatchFindClassUsingStringsBuilder
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = query.toQuery()
        )

        fun getMethodOrNull(
            query: FindMethodBuilder
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getClassOrNull(
            query: FindClassBuilder
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getFieldOrNull(
            query: FindFieldBuilder
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getMethodsOrEmpty(
            query: FindMethodBuilder? = null
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getClassesOrEmpty(
            query: FindClassBuilder? = null
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getFieldsOrEmpty(
            query: FindFieldBuilder? = null
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = query?.toQuery()
        )

        // endregion

        // region java SAM need key

        @JvmOverloads
        fun getMethod(
            key: String,
            query: FindMethodBuilder? = null
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getClass(
            key: String,
            query: FindClassBuilder? = null
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getField(
            key: String,
            query: FindFieldBuilder? = null
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getMethods(
            key: String,
            query: FindMethodBuilder? = null
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getClasses(
            key: String,
            query: FindClassBuilder? = null
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getFields(
            key: String,
            query: FindFieldBuilder? = null
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getBatchUsingStringsMethods(
            key: String,
            query: BatchFindMethodUsingStringsBuilder? = null
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getBatchUsingStringsClasses(
            key: String,
            query: BatchFindClassUsingStringsBuilder? = null
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getMethodOrNull(
            key: String,
            query: FindMethodBuilder? = null
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getClassOrNull(
            key: String,
            query: FindClassBuilder? = null
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getFieldOrNull(
            key: String,
            query: FindFieldBuilder? = null
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        fun getMethodsOrEmpty(
            key: String,
            query: FindMethodBuilder? = null
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getClassesOrEmpty(
            key: String,
            query: FindClassBuilder? = null
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getFieldsOrEmpty(
            key: String,
            query: FindFieldBuilder? = null
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        // endregion

        // region SAM direct

        @JvmOverloads
        fun getMethodDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod = innerGetMethodDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getClassDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass = innerGetClassDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getFieldDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField = innerGetFieldDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getMethodsDirect(
            key: String,
            query: BridgeMethodsBuilder? = null
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassesDirect(
            key: String,
            query: BridgeClassesBuilder? = null
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldsDirect(
            key: String,
            query: BridgeFieldsBuilder? = null
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getMethodDirectOrNull(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod? = innerGetMethodDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassDirectOrNull(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass? = innerGetClassDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldDirectOrNull(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField? = innerGetFieldDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getMethodsDirectOrEmpty(
            key: String,
            query: BridgeMethodsBuilder? = null
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassesDirectOrEmpty(
            key: String,
            query: BridgeClassesBuilder? = null
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldsDirectOrEmpty(
            key: String,
            query: BridgeFieldsBuilder? = null
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        // endregion

        // region java overloads auto gen key

        fun getMethod(
            finder: FindMethod
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getClass(
            finder: FindClass
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getField(
            finder: FindField
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getMethods(
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getClasses(
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getFields(
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getBatchUsingStringsMethods(
            finder: BatchFindMethodUsingStrings
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = finder
        )

        fun getBatchUsingStringsClasses(
            finder: BatchFindClassUsingStrings
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = finder
        )

        fun getMethodOrNull(
            finder: FindMethod
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = finder
        )

        fun getClassOrNull(
            finder: FindClass
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = finder,
        )

        fun getFieldOrNull(
            finder: FindField
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = finder,
        )

        fun getMethodsOrEmpty(
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = finder
        )

        fun getClassesOrEmpty(
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = finder
        )

        fun getFieldsOrEmpty(
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = finder
        )

        // endregion

        // region java overloads need key

        fun getMethod(
            key: String,
            finder: FindMethod
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getClass(
            key: String,
            finder: FindClass
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getField(
            key: String,
            finder: FindField
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getMethods(
            key: String,
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getClasses(
            key: String,
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getFields(
            key: String,
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getBatchUsingStringsMethods(
            key: String,
            finder: BatchFindMethodUsingStrings
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = finder
        )

        fun getBatchUsingStringsClasses(
            key: String,
            finder: BatchFindClassUsingStrings
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = finder
        )

        fun getMethodOrNull(
            key: String,
            finder: FindMethod
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getClassOrNull(
            key: String,
            finder: FindClass
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getFieldOrNull(
            key: String,
            finder: FindField
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getMethodsOrEmpty(
            key: String,
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = finder
        )

        fun getClassesOrEmpty(
            key: String,
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = finder
        )

        fun getFieldsOrEmpty(
            key: String,
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = finder
        )

        // endregion

        // region DSL auto gen key

        @JvmSynthetic
        fun getMethod(
            query: FindMethod.() -> Unit
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = FindMethod().apply(query)
        )!!

        @JvmSynthetic
        fun getClass(
            query: FindClass.() -> Unit
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = FindClass().apply(query)
        )!!

        @JvmSynthetic
        fun getField(
            query: FindField.() -> Unit
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = FindField().apply(query)
        )!!

        @JvmSynthetic
        fun getMethods(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClasses(
            query: FindClass.() -> Unit
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFields(
            query: FindField.() -> Unit
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsMethods(
            query: BatchFindMethodUsingStrings.() -> Unit
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = BatchFindMethodUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsClasses(
            query: BatchFindClassUsingStrings.() -> Unit
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = BatchFindClassUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getMethodOrNull(
            query: FindMethod.() -> Unit
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassOrNull(
            query: FindClass.() -> Unit
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldOrNull(
            query: FindField.() -> Unit
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getMethodsOrEmpty(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassesOrEmpty(
            query: FindClass.() -> Unit
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldsOrEmpty(
            query: FindField.() -> Unit
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = FindField().apply(query)
        )

        // endregion

        // region DSL need key

        @JvmSynthetic
        fun getMethod(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = FindMethod().apply(query)
        )!!

        @JvmSynthetic
        fun getClass(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = FindClass().apply(query)
        )!!

        @JvmSynthetic
        fun getField(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = FindField().apply(query)
        )!!

        @JvmSynthetic
        fun getMethods(
            key: String,
            query: (FindMethod.() -> Unit)
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClasses(
            key: String,
            query: (FindClass.() -> Unit)
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFields(
            key: String,
            query: (FindField.() -> Unit)
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsMethods(
            key: String,
            query: BatchFindMethodUsingStrings.() -> Unit
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = BatchFindMethodUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsClasses(
            key: String,
            query: BatchFindClassUsingStrings.() -> Unit
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = BatchFindClassUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getMethodOrNull(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassOrNull(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldOrNull(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getMethodsOrEmpty(
            key: String,
            query: (FindMethod.() -> Unit)
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassesOrEmpty(
            key: String,
            query: (FindClass.() -> Unit)
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldsOrEmpty(
            key: String,
            query: (FindField.() -> Unit)
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = FindField().apply(query)
        )

        // endregion

        // region DSL direct

        @JvmSynthetic
        fun getMethodDirect(
            key: String,
            query: DexKitBridge.() -> MethodData
        ): DexMethod = innerGetMethodDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getClassDirect(
            key: String,
            query: DexKitBridge.() -> ClassData
        ): DexClass = innerGetClassDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getFieldDirect(
            key: String,
            query: DexKitBridge.() -> FieldData
        ): DexField = innerGetFieldDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getMethodsDirect(
            key: String,
            query: DexKitBridge.() -> List<MethodData>
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getClassesDirect(
            key: String,
            query: DexKitBridge.() -> List<ClassData>
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getFieldsDirect(
            key: String,
            query: DexKitBridge.() -> List<FieldData>
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getMethodDirectOrNull(
            key: String,
            query: DexKitBridge.() -> MethodData?
        ): DexMethod? = innerGetMethodDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getClassDirectOrNull(
            key: String,
            query: DexKitBridge.() -> ClassData?
        ): DexClass? = innerGetClassDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getFieldDirectOrNull(
            key: String,
            query: DexKitBridge.() -> FieldData?
        ): DexField? = innerGetFieldDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getMethodsDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<MethodData>
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        @JvmSynthetic
        fun getClassesDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<ClassData>
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        @JvmSynthetic
        fun getFieldsDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<FieldData>
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        // endregion

        // region inner methods

        private fun innerGetMethod(
            key: String? = null,
            allowNull: Boolean,
            query: FindMethod? = null
        ): DexMethod? {
            val buildQuery = query?.let { { query } }

            return getInternal(
                key = key,
                allowNull = allowNull,
                buildQuery = buildQuery,
                executor = { b, q -> b.findMethod(q) },
                mapper   = { it.toDexMethod() }
            ).getOrThrow()
        }

        private fun innerGetMethods(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindMethod? = null
        ): List<DexMethod> {
            val buildQuery = query?.let { { query } }

            return getInternalList(
                key = key,
                allowEmpty = allowEmpty,
                buildQuery = buildQuery,
                executor = { b, q -> b.findMethod(q) },
                mapper   = { it.toDexMethod() }
            ).getOrThrow()
        }

        private fun innerGetClass(
            key: String? = null,
            allowNull: Boolean,
            query: FindClass? = null
        ): DexClass? = getInternal(
            key = key,
            allowNull = allowNull,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetClasses(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindClass? = null
        ): List<DexClass> = getInternalList(
            key = key,
            allowEmpty = allowEmpty,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetField(
            key: String? = null,
            allowNull: Boolean,
            query: FindField? = null
        ): DexField? = getInternal(
            key = key,
            allowNull = allowNull,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetFields(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindField? = null
        ): List<DexField> = getInternalList(
            key = key,
            allowEmpty = allowEmpty,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetBatchUsingStringsClasses(
            key: String?,
            query: BatchFindClassUsingStrings? = null
        ): Map<String, List<DexClass>> = getInternalMap(
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindClassUsingStrings -> b.batchFindClassUsingStrings(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetBatchUsingStringsMethods(
            key: String?,
            query: BatchFindMethodUsingStrings? = null
        ): Map<String, List<DexMethod>> = getInternalMap(
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindMethodUsingStrings -> b.batchFindMethodUsingStrings(q) },
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetMethodDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> MethodData?)? = null
        ): DexMethod? = getDirectInternal(
            key = key,
            allowNull = allowNull,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetClassDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> ClassData?)? = null
        ): DexClass? = getDirectInternal(
            key = key,
            allowNull = allowNull,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetFieldDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> FieldData?)? = null
        ): DexField? = getDirectInternal(
            key = key,
            allowNull = allowNull,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetMethodsDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<MethodData>)? = null
        ): List<DexMethod> = getDirectInternalList(
            key = key,
            allowEmpty = allowEmpty,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetClassesDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<ClassData>)? = null
        ): List<DexClass> = getDirectInternalList(
            key = key,
            executor = query,
            allowEmpty = allowEmpty,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetFieldsDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<FieldData>)? = null
        ): List<DexField> = getDirectInternalList(
            key = key,
            executor = query,
            allowEmpty = allowEmpty,
            mapper = { it.toDexField() }
        ).getOrThrow()

        // endregion

        // region no public

        private fun FindMethodBuilder.toQuery(): FindMethod =
            FindMethod().apply { this@toQuery.build(this) }

        private fun FindClassBuilder.toQuery(): FindClass =
            FindClass().apply { this@toQuery.build(this) }

        private fun FindFieldBuilder.toQuery(): FindField =
            FindField().apply { this@toQuery.build(this) }

        private fun BatchFindMethodUsingStringsBuilder.toQuery(): BatchFindMethodUsingStrings =
            BatchFindMethodUsingStrings().apply { this@toQuery.build(this) }

        private fun BatchFindClassUsingStringsBuilder.toQuery(): BatchFindClassUsingStrings =
            BatchFindClassUsingStrings().apply { this@toQuery.build(this) }

        private fun BridgeMethodBuilder.toBridgeQuery(): (DexKitBridge) -> MethodData =
            this::build

        private fun BridgeClassBuilder.toBridgeQuery(): (DexKitBridge) -> ClassData =
            this::build

        private fun BridgeFieldBuilder.toBridgeQuery(): (DexKitBridge) -> FieldData =
            this::build

        private fun BridgeMethodsBuilder.toBridgeQuery(): (DexKitBridge) -> List<MethodData> =
            this::build

        private fun BridgeClassesBuilder.toBridgeQuery(): (DexKitBridge) -> List<ClassData> =
            this::build

        private fun BridgeFieldsBuilder.toBridgeQuery(): (DexKitBridge) -> List<FieldData> =
            this::build

        private fun <T : ISerializable> getCached(
            key: String,
            allowNull: Boolean,
            loader: (() -> T?)? = null,
        ): Result<T?> {
            fun <T : ISerializable> innerGet(key: String): Result<T?>? {
                cache.get(key, null)?.let {
                    val ret = if (it == CACHE_NULL) {
                        null
                    } else {
                        ISerializable.deserializeAs<T>(it)
                    }
                    return Result.success(ret)
                }
                return null
            }

            lock.read { innerGet<T>(key)?.let { return it } }

            loader ?: return Result.failure(
                NoSuchElementException("no found cache for key: $key")
            )

            return lock.write {
                innerGet<T>(key)?.let { return it }

                runCatching {
                    loader().also {
                        if (it == null && !allowNull) {
                            return Result.failure(
                                IllegalStateException(
                                    "query returned null for key: $key but null not allowed"
                                )
                            )
                        }
                        cache.put(key, it?.serialize() ?: CACHE_NULL)
                    }
                }
            }
        }

        private fun <T : ISerializable> getCachedList(
            key: String,
            allowEmpty: Boolean,
            loader: (() -> List<T>)? = null,
        ): Result<List<T>> {
            fun <T : ISerializable> innerGet(key: String): List<T>? {
                cache.getList(key, null)?.let {
                    return it.map { ISerializable.deserializeAs(it) }
                }
                return null
            }

            lock.read { innerGet<T>(key)?.let { return Result.success(it) } }
            loader ?: return Result.failure(
                NoSuchElementException("no found cache for key: $key")
            )

            return lock.write {
                innerGet<T>(key)?.let { return Result.success(it) }
                runCatching {
                    loader().also {
                        if (it.isEmpty() && !allowEmpty) {
                            return Result.failure(
                                IllegalStateException(
                                    "query returned empty for key: $key but empty not allowed"
                                )
                            )
                        }
                        cache.putList(key, it.map(ISerializable::serialize))
                    }
                }
            }
        }

        private fun <T : ISerializable> getCachedMap(
            key: String,
            loader: (() -> Map<String, List<T>>)? = null,
        ): Result<Map<String, List<T>>> {
            fun <T : ISerializable> innerGetMap(key: String): Map<String, List<T>>? {
                cache.getList("$key:keys", null)?.let { keys ->
                    val map = mutableMapOf<String, List<T>>()
                    keys.forEach { groupKey ->
                        val dataList = cache.getList("$key:$groupKey", null)
                            ?.map { ISerializable.deserializeAs<T>(it) }
                            ?: emptyList()
                        map.put(groupKey, dataList)
                    }
                    return map
                }
                return null
            }

            lock.read { innerGetMap<T>(key)?.let { return Result.success(it) } }
            loader ?: return Result.failure(NoSuchElementException("no found cache for key: $key"))

            return lock.write {
                innerGetMap<T>(key)?.let { return Result.success(it) }
                runCatching {
                    val oldKeys = cache.getList("$key:keys", null) ?: emptyList()
                    loader().also { map ->
                        val keys = mutableListOf<String>()
                        map.entries.forEach { (groupKey, value) ->
                            keys.add(groupKey)
                            cache.putList("$key:$groupKey", value.map { it.serialize() })
                        }
                        (oldKeys - keys.toSet()).forEach { cache.remove("$key:$it") }
                        cache.putList("$key:keys", keys)
                    }
                }
            }
        }

        private fun spKeyOf(kind: String, key: String?, query: BaseFinder? = null): String {
            if (key != null) return "$appTag:$kind:$key"
            requireNotNull(query) {
                "Either key or query must be provided for auto-generated cache key."
            }
            return "$appTag:$kind:${query.hashKey()}"
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternal(
            key: String?,
            allowNull: Boolean,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<R?> {
            val query = buildQuery?.invoke()
            // :s: -> single
            val spKey = spKeyOf("s", key, query)
            val loader: (() -> R?)? = query?.let {
                {
                    acquireBridge {
                        val list = executor(it, query)
                        var ret: D? = list.firstOrNull()
                        for (i in 1 until list.size) {
                            if (ret != list[i]) {
                                ret = null
                                break
                            }
                        }
                        ret?.let(mapper)
                    }
                }
            }
            return getCached(spKey, allowNull, loader)
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalList(
            key: String?,
            allowEmpty: Boolean,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            val query = buildQuery?.invoke()
            // :l: -> list
            val spKey = spKeyOf("l", key, query)
            val loader: (() -> List<R>)? = query?.let {
                { acquireBridge { executor(it, query).map(mapper) } }
            }
            return getCachedList(spKey, allowEmpty, loader)
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalMap(
            key: String?,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> Map<String, List<D>>,
            noinline mapper: (D) -> R
        ): Result<Map<String, List<R>>> {
            val query = buildQuery?.invoke()
            // :b: -> batch find
            val spKey = spKeyOf("b", key, query)
            val loader: (() -> Map<String, List<R>>)? = query?.let {
                { acquireBridge { executor(it, query).mapValues { it.value.map(mapper) } } }
            }
            return getCachedMap(spKey, loader)
        }

        private inline fun <D, R : ISerializable> getDirectInternal(
            key: String,
            allowNull: Boolean,
            noinline executor: (DexKitBridge.() -> D?)?,
            noinline mapper: (D) -> R
        ): Result<R?> {
            // :s: -> single
            val spKey = spKeyOf("s", key)
            val loader: (() -> R?)? = executor?.let {
                { acquireBridge { it.let(executor)?.let(mapper) } }
            }
            return getCached(spKey, allowNull, loader)
        }

        private inline fun <D, R : ISerializable> getDirectInternalList(
            key: String,
            allowEmpty: Boolean,
            noinline executor: (DexKitBridge.() -> List<D>)?,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            // :l: -> list
            val spKey = spKeyOf("l", key)
            val loader: (() -> List<R>)? = executor?.let {
                { acquireBridge { it.let(executor).map(mapper) } }
            }
            return getCachedList(spKey, allowEmpty, loader)
        }

        // endregion
    }
}