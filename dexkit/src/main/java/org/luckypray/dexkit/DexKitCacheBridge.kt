@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")

package org.luckypray.dexkit

import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.result.ClassDataList
import org.luckypray.dexkit.result.FieldDataList
import org.luckypray.dexkit.result.MethodDataList
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
    }
    private val pool = ConcurrentHashMap<String, RecyclableBridge>()
    private val lock = ReentrantReadWriteLock()
    var timeout: Long = 5_000L

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
        lock.write { cache.clear(appTag) }
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
        fun clear(appTag: String)
        fun clearAll()
    }

    class RecyclableBridge private constructor(
        val appTag: String,
        val path: String? = null,
        val dexArray: Array<ByteArray>? = null,
        val classLoader: ClassLoader? = null,
    ) : Closeable {

        companion object {
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
                reaperFuture = scheduler.schedule(reaper, timeout, TimeUnit.MILLISECONDS)
            }
        }

        private inline fun <R> acquire(block: RecyclableBridge.() -> R): R {
            beginUse()
            return try {
                block()
            } finally {
                endUse()
            }
        }

        val bridge: DexKitBridge
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

        private fun BridgeMethodBuilder.toBridgeQuery(): (DexKitBridge) -> MethodDataList =
            this::build

        private fun BridgeClassBuilder.toBridgeQuery(): (DexKitBridge) -> ClassDataList =
            this::build

        private fun BridgeFieldBuilder.toBridgeQuery(): (DexKitBridge) -> FieldDataList =
            this::build

        private fun <T : ISerializable> getCached(
            key: String,
            loader: (() -> T)? = null
        ): Result<T> {
            fun <T> innerGet(key: String): T? {
                cache.get(key, null)?.let {
                    return ISerializable.deserializeAs(it)
                }
                return null
            }

            lock.read { innerGet<T>(key)?.let { return Result.success(it) } }
            loader ?: return Result.failure(NoSuchElementException("no found cache for key: $key"))

            return lock.write {
                innerGet<T>(key)?.let { return Result.success(it) }
                runCatching {
                    loader().also {
                        cache.put(key, it.serialize())
                    }
                }
            }
        }

        private fun <T : ISerializable> getCachedList(
            key: String,
            loader: (() -> List<T>)? = null,
        ): Result<List<T>> {
            fun <T> innerGet(key: String): List<T>? {
                cache.getList(key, null)?.let {
                    return it.map { ISerializable.deserializeAs(it) }
                }
                return null
            }

            lock.read { innerGet<T>(key)?.let { return Result.success(it) } }
            loader ?: return Result.failure(NoSuchElementException("no found cache for key: $key"))

            return lock.write {
                innerGet<T>(key)?.let { return Result.success(it) }
                runCatching {
                    loader().also {
                        cache.putList(key, it.map { it.serialize() })
                    }
                }
            }
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternal(
            key: String?,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> D,
            noinline mapper: (D) -> R
        ): Result<R> {
            val query = buildQuery?.invoke()
            val spKey = "$appTag:${key ?: query!!.hashKey()}"
            val loader: (() -> R)? = query?.let {
                { mapper(executor(bridge, query)) }
            }
            return getCached(spKey, loader)
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalList(
            key: String?,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            val query = buildQuery?.invoke()
            val spKey = "$appTag:${key ?: query!!.hashKey()}"
            val loader: (() -> List<R>)? = query?.let {
                { executor(bridge, query).map(mapper) }
            }
            return getCachedList(spKey, loader)
        }

        private inline fun <D, R : ISerializable> getDirectInternal(
            key: String,
            noinline executor: (DexKitBridge.() -> List<D>)?,
            noinline mapper: (D) -> R
        ): Result<R> {
            val cacheKey = "$appTag:$key"
            val loader: (() -> R)? = executor?.let {
                { mapper(bridge.let(executor).single()) }
            }
            return getCached(cacheKey, loader)
        }

        private inline fun <D, R : ISerializable> getDirectInternalList(
            key: String,
            noinline executor: (DexKitBridge.() -> List<D>)?,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            val cacheKey = "$appTag:$key"
            val loader: (() -> List<R>)? = executor?.let {
                { bridge.let(executor).map(mapper) }
            }
            return getCachedList(cacheKey, loader)
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

        fun interface BridgeMethodBuilder {
            fun build(b: DexKitBridge): MethodDataList
        }

        fun interface BridgeClassBuilder {
            fun build(b: DexKitBridge): ClassDataList
        }

        fun interface BridgeFieldBuilder {
            fun build(b: DexKitBridge): FieldDataList
        }

        // endregion

        // region java SAM

        fun getMethod(query: FindMethodBuilder): DexMethod = getMethod {
            query.build(this)
        }

        @JvmOverloads
        fun getMethod(key: String, query: FindMethodBuilder? = null): DexMethod = getMethod(key) {
            query?.build(this)
        }

        fun getMethods(query: FindMethodBuilder): List<DexMethod> = getMethods {
            query.build(this)
        }

        @JvmOverloads
        fun getMethods(key: String, query: FindMethodBuilder? = null): List<DexMethod> = getMethods(key) {
            query?.build(this)
        }

        fun getClass(query: FindClassBuilder): DexClass = getClass {
            query.build(this)
        }

        @JvmOverloads
        fun getClass(key: String, query: FindClassBuilder? = null): DexClass = getClass(key) {
            query?.build(this)
        }

        fun getClasses(query: FindClassBuilder): List<DexClass> = getClasses {
            query.build(this)
        }

        @JvmOverloads
        fun getClasses(key: String, query: FindClassBuilder? = null): List<DexClass> = getClasses(key) {
            query?.build(this)
        }

        fun getField(query: FindFieldBuilder): DexField = getField {
            query.build(this)
        }

        @JvmOverloads
        fun getField(key: String, query: FindFieldBuilder? = null): DexField = getField(key) {
            query?.build(this)
        }

        fun getFields(query: FindFieldBuilder): List<DexField> = getFields {
            query.build(this)
        }

        @JvmOverloads
        fun getFields(key: String, query: FindFieldBuilder? = null): List<DexField> = getFields(key) {
            query?.build(this)
        }

        fun getMethodOrNull(query: FindMethodBuilder): DexMethod? = getMethodOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getMethodOrNull(key: String, query: FindMethodBuilder? = null): DexMethod? = getMethodOrNull(key) {
            query?.build(this)
        }

        fun getMethodsOrNull(query: FindMethodBuilder): List<DexMethod>? = getMethodsOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getMethodsOrNull(key: String, query: FindMethodBuilder? = null): List<DexMethod>? = getMethodsOrNull(key) {
            query?.build(this)
        }

        fun getClassOrNull(query: FindClassBuilder): DexClass? = getClassOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getClassOrNull(key: String, query: FindClassBuilder? = null): DexClass? = getClassOrNull(key) {
            query?.build(this)
        }

        fun getClassesOrNull(query: FindClassBuilder): List<DexClass>? = getClassesOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getClassesOrNull(key: String, query: FindClassBuilder? = null): List<DexClass>? = getClassesOrNull(key) {
            query?.build(this)
        }

        fun getFieldOrNull(query: FindFieldBuilder): DexField? = getFieldOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getFieldOrNull(key: String, query: FindFieldBuilder? = null): DexField? = getFieldOrNull(key) {
            query?.build(this)
        }

        fun getFieldsOrNull(query: FindFieldBuilder): List<DexField>? = getFieldsOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getFieldsOrNull(key: String, query: FindFieldBuilder? = null): List<DexField>? = getFieldsOrNull(key) {
            query?.build(this)
        }

        @JvmOverloads
        fun getMethodDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod = getMethodDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getMethodsDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): List<DexMethod> = getMethodsDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass = getClassDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassesDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): List<DexClass> = getClassesDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField = getFieldDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldsDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): List<DexField> = getFieldsDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getMethodDirectOrNull(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod? = getMethodDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getMethodsDirectOrNull(
            key: String,
            query: BridgeMethodBuilder? = null
        ): List<DexMethod>? = getMethodsDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassDirectOrNull(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass? = getClassDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassesDirectOrNull(
            key: String,
            query: BridgeClassBuilder? = null
        ): List<DexClass>? = getClassesDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldDirectOrNull(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField? = getFieldDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldsDirectOrNull(
            key: String,
            query: BridgeFieldBuilder? = null
        ): List<DexField>? = getFieldsDirectOrNull(key, query?.toBridgeQuery())

        // endregion

        // region java overloads

        fun getMethod(finder: FindMethod): DexMethod = getMethod { finder }

        fun getMethods(finder: FindMethod): List<DexMethod> = getMethods { finder }

        fun getClass(finder: FindClass): DexClass = getClass { finder }

        fun getClasses(finder: FindClass): List<DexClass> = getClasses { finder }

        fun getField(finder: FindField): DexField = getField { finder }

        fun getFields(finder: FindField): List<DexField> = getFields { finder }

        fun getMethod(key: String, finder: FindMethod): DexMethod = getMethod(key) { finder }

        fun getMethods(key: String, finder: FindMethod): List<DexMethod> = getMethods(key) { finder }

        fun getClass(key: String, finder: FindClass): DexClass = getClass(key) { finder }

        fun getClasses(key: String, finder: FindClass): List<DexClass> = getClasses(key) { finder }

        fun getField(key: String, finder: FindField): DexField = getField(key) { finder }

        fun getFields(key: String, finder: FindField): List<DexField> = getFields(key) { finder }

        fun getMethodOrNull(finder: FindMethod): DexMethod? = getMethodOrNull { finder }

        fun getMethodsOrNull(finder: FindMethod): List<DexMethod>? = getMethodsOrNull { finder }

        fun getClassOrNull(finder: FindClass): DexClass? = getClassOrNull { finder }

        fun getClassesOrNull(finder: FindClass): List<DexClass>? = getClassesOrNull { finder }

        fun getFieldOrNull(finder: FindField): DexField? = getFieldOrNull { finder }

        fun getFieldsOrNull(finder: FindField): List<DexField>? = getFieldsOrNull { finder }

        fun getMethodOrNull(key: String, finder: FindMethod): DexMethod? = getMethodOrNull(key) { finder }

        fun getMethodsOrNull(key: String, finder: FindMethod): List<DexMethod>? = getMethodsOrNull(key) { finder }

        fun getClassOrNull(key: String, finder: FindClass): DexClass? = getClassOrNull(key) { finder }

        fun getClassesOrNull(key: String, finder: FindClass): List<DexClass>? = getClassesOrNull(key) { finder }

        fun getFieldOrNull(key: String, finder: FindField): DexField? = getFieldOrNull(key) { finder }

        fun getFieldsOrNull(key: String, finder: FindField): List<DexField>? = getFieldsOrNull(key) { finder }

        // endregion

        // region DSL

        @JvmSynthetic
        fun getMethod(
            query: FindMethod.() -> Unit
        ): DexMethod = getInternal(
            key = null,
            buildQuery = { FindMethod().apply(query) },
            executor =  { b, q -> b.findMethod(q).single() },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethods(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = getInternalList(
            key = null,
            buildQuery = { FindMethod().apply(query) },
            executor =  { b, q -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClass(
            query: FindClass.() -> Unit
        ): DexClass = getInternal(
            key = null,
            buildQuery = { FindClass().apply(query) },
            executor =  { b, q -> b.findClass(q).single() },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClasses(
            query: FindClass.() -> Unit
        ): List<DexClass> = getInternalList(
            key = null,
            buildQuery = { FindClass().apply(query) },
            executor =  { b, q -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getField(
            query: FindField.() -> Unit
        ): DexField = getInternal(
            key = null,
            buildQuery = { FindField().apply(query) },
            executor =  { b, q -> b.findField(q).single() },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getFields(
            query: FindField.() -> Unit
        ): List<DexField> = getInternalList(
            key = null,
            buildQuery = { FindField().apply(query) },
            executor =  { b, q -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethod(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): DexMethod = getInternal(
            key = key,
            buildQuery = query?.let{ { FindMethod().apply(query) } },
            executor =  { b, q: FindMethod -> b.findMethod(q).single() },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethods(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): List<DexMethod> = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindMethod().apply(query) } },
            executor =  { b, q: FindMethod -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClass(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): DexClass = getInternal(
            key = key,
            buildQuery = query?.let{ { FindClass().apply(query) } },
            executor =  { b, q: FindClass -> b.findClass(q).single() },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClasses(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): List<DexClass> = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindClass().apply(query) } },
            executor =  { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getField(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): DexField = getInternal(
            key = key,
            buildQuery = query?.let{ { FindField().apply(query) } },
            executor =  { b, q: FindField -> b.findField(q).single() },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getFields(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): List<DexField> = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindField().apply(query) } },
            executor =  { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethodOrNull(
            query: FindMethod.() -> Unit
        ): DexMethod? = getInternal(
            key = null,
            buildQuery = { FindMethod().apply(query) },
            executor =  { b, q -> b.findMethod(q).single() },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodsOrNull(
            query: FindMethod.() -> Unit
        ): List<DexMethod>? = getInternalList(
            key = null,
            buildQuery = { FindMethod().apply(query) },
            executor =  { b, q -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassOrNull(
            query: FindClass.() -> Unit
        ): DexClass? = getInternal(
            key = null,
            buildQuery = { FindClass().apply(query) },
            executor =  { b, q -> b.findClass(q).single() },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassesOrNull(
            query: FindClass.() -> Unit
        ): List<DexClass>? = getInternalList(
            key = null,
            buildQuery = { FindClass().apply(query) },
            executor =  { b, q -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldOrNull(
            query: FindField.() -> Unit
        ): DexField? = getInternal(
            key = null,
            buildQuery = { FindField().apply(query) },
            executor =  { b, q -> b.findField(q).single() },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldsOrNull(
            query: FindField.() -> Unit
        ): List<DexField>? = getInternalList(
            key = null,
            buildQuery = { FindField().apply(query) },
            executor =  { b, q -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodOrNull(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): DexMethod? = getInternal(
            key = key,
            buildQuery = query?.let{ { FindMethod().apply(query) } },
            executor =  { b, q: FindMethod -> b.findMethod(q).single() },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodsOrNull(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): List<DexMethod>? = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindMethod().apply(query) } },
            executor =  { b, q: FindMethod -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassOrNull(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): DexClass? = getInternal(
            key = key,
            buildQuery = query?.let{ { FindClass().apply(query) } },
            executor =  { b, q: FindClass -> b.findClass(q).single() },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassesOrNull(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): List<DexClass>? = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindClass().apply(query) } },
            executor =  { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldOrNull(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): DexField? = getInternal(
            key = key,
            buildQuery = query?.let{ { FindField().apply(query) } },
            executor =  { b, q: FindField -> b.findField(q).single() },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldsOrNull(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): List<DexField>? = getInternalList(
            key = key,
            buildQuery = query?.let{ { FindField().apply(query) } },
            executor =  { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodDirect(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): DexMethod = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        @JvmSynthetic
        fun getMethodsDirect(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): List<DexMethod> = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        @JvmSynthetic
        fun getClassDirect(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): DexClass = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        @JvmSynthetic
        fun getClassesDirect(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): List<DexClass> = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        @JvmSynthetic
        fun getFieldDirect(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): DexField = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()

        @JvmSynthetic
        fun getFieldsDirect(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): List<DexField> = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()

        @JvmSynthetic
        fun getMethodDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): DexMethod? = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrNull()

        @JvmSynthetic
        fun getMethodsDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): List<DexMethod>? = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrNull()

        @JvmSynthetic
        fun getClassDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): DexClass? = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrNull()

        @JvmSynthetic
        fun getClassesDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): List<DexClass>? = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrNull()

        @JvmSynthetic
        fun getFieldDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): DexField? = getDirectInternal(
            key = key,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrNull()

        @JvmSynthetic
        fun getFieldsDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): List<DexField>? = getDirectInternalList(
            key = key,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrNull()

        // endregion
    }
}