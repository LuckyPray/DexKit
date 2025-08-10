@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")

package org.luckypray.dexkit

import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.result.BaseDataList
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
//        fun getList(key: String, default: List<String>?): List<String>?
//        fun putList(key: String, value: List<String>)
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
            private const val EMPTY_LIST = "[]"

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
            allowNull: Boolean,
            loader: (() -> T?)? = null,
        ): Result<T?> {
            fun <T> innerGet(key: String): Result<T?>? {
                cache.get(key, null)?.let {
                    val ret = if (it == CACHE_NULL) {
                        null
                    } else {
                        ISerializable.deserializeAs(it)
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
                        it?.let {
                            cache.put(key, it.serialize())
                        } ?: run {
                            if (allowNull) {
                                cache.put(key, CACHE_NULL)
                            }
                        }
                    }
                }
            }
        }

        private fun <T : ISerializable> getCachedList(
            key: String,
            loader: (() -> List<T>)? = null,
        ): Result<List<T>> {
            fun <T> innerGet(key: String): Result<List<T>>? {
                cache.get(key, null)?.let {
                    val ret: List<T> = if (it == EMPTY_LIST) {
                        emptyList()
                    } else {
                        require(it.first() == '[' && it.last() == ']') {
                            "not list cache: $it"
                        }
                        it.substring(1, it.length - 1)
                            .split("#")
                            .map { ISerializable.deserializeAs(it) }
                    }
                    return Result.success(ret)
                }
                return null
            }

            lock.read { innerGet<T>(key)?.let { return it } }
            loader ?: return Result.failure(NoSuchElementException("no found cache for key: $key"))

            return lock.write {
                innerGet<T>(key)?.let { return it }
                runCatching {
                    loader().also { list ->
                        val value = list.joinToString("#", "[", "]") {
                            it.serialize()
                        }
                        cache.put(key, value)
                    }
                }
            }
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternal(
            key: String?,
            allowNull: Boolean,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> BaseDataList<D>,
            noinline mapper: (D) -> R
        ): Result<R?> {
            val query = buildQuery?.invoke()
            val spKey = "$appTag:${key ?: query!!.hashKey()}"
            val loader: (() -> R?)? = query?.let {
                // TODO sure singleOrNull? maybe need check BaseDataList
                { executor(bridge, query).singleOrNull()?.let(mapper) }
            }
            return getCached(spKey, allowNull, loader)
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
            allowNull: Boolean,
            noinline executor: (DexKitBridge.() -> List<D>)?,
            noinline mapper: (D) -> R
        ): Result<R?> {
            val cacheKey = "$appTag:$key"
            val loader: (() -> R?)? = executor?.let {
                { bridge.let(executor).singleOrNull()?.let(mapper) }
            }
            return getCached(cacheKey, allowNull, loader)
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

        fun getClassOrNull(query: FindClassBuilder): DexClass? = getClassOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getClassOrNull(key: String, query: FindClassBuilder? = null): DexClass? = getClassOrNull(key) {
            query?.build(this)
        }

        fun getFieldOrNull(query: FindFieldBuilder): DexField? = getFieldOrNull {
            query.build(this)
        }

        @JvmOverloads
        fun getFieldOrNull(key: String, query: FindFieldBuilder? = null): DexField? = getFieldOrNull(key) {
            query?.build(this)
        }

        @JvmOverloads
        fun getMethodDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod = innerGetMethodDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getMethodsDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): List<DexMethod> = innerGetMethodsDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass = innerGetClassDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassesDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): List<DexClass> = innerGetClassesDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField = innerGetFieldDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldsDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): List<DexField> = innerGetFieldsDirect(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getMethodDirectOrNull(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod? = innerGetMethodDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getClassDirectOrNull(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass? = innerGetClassDirectOrNull(key, query?.toBridgeQuery())

        @JvmOverloads
        fun getFieldDirectOrNull(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField? = innerGetFieldDirectOrNull(key, query?.toBridgeQuery())

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

        fun getClassOrNull(finder: FindClass): DexClass? = getClassOrNull { finder }

        fun getFieldOrNull(finder: FindField): DexField? = getFieldOrNull { finder }

        fun getMethodOrNull(key: String, finder: FindMethod): DexMethod? = getMethodOrNull(key) { finder }

        fun getClassOrNull(key: String, finder: FindClass): DexClass? = getClassOrNull(key) { finder }

        fun getFieldOrNull(key: String, finder: FindField): DexField? = getFieldOrNull(key) { finder }

        // endregion

        // region DSL

        @JvmSynthetic
        fun getMethod(
            query: FindMethod.() -> Unit
        ): DexMethod = getInternal(
            key = null,
            allowNull = false,
            buildQuery = { FindMethod().apply(query) },
            // TODO 是否需要 .single()
            executor = { b, q -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getMethods(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = getInternalList(
            key = null,
            buildQuery = { FindMethod().apply(query) },
            executor = { b, q -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClass(
            query: FindClass.() -> Unit
        ): DexClass = getInternal(
            key = null,
            allowNull = false,
            buildQuery = { FindClass().apply(query) },
            executor = { b, q -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getClasses(
            query: FindClass.() -> Unit
        ): List<DexClass> = getInternalList(
            key = null,
            buildQuery = { FindClass().apply(query) },
            executor = { b, q -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getField(
            query: FindField.() -> Unit
        ): DexField = getInternal(
            key = null,
            allowNull = false,
            buildQuery = { FindField().apply(query) },
            executor = { b, q -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getFields(
            query: FindField.() -> Unit
        ): List<DexField> = getInternalList(
            key = null,
            buildQuery = { FindField().apply(query) },
            executor = { b, q -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethod(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod = innerGetMethod(key, query)

        private fun innerGetMethod(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): DexMethod = getInternal(
            key = key,
            allowNull = false,
            buildQuery = query?.let { { FindMethod().apply(query) } },
            executor = { b, q: FindMethod -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getMethods(
            key: String,
            query: (FindMethod.() -> Unit)
        ): List<DexMethod> = innerGetMethods(key, query)

        private fun innerGetMethods(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): List<DexMethod> = getInternalList(
            key = key,
            buildQuery = query?.let { { FindMethod().apply(query) } },
            executor = { b, q: FindMethod -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrThrow()

        @JvmSynthetic
        fun getClass(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass = innerGetClass(key, query)

        private fun innerGetClass(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): DexClass = getInternal(
            key = key,
            allowNull = false,
            buildQuery = query?.let { { FindClass().apply(query) } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getClasses(
            key: String,
            query: (FindClass.() -> Unit)
        ): List<DexClass> = innerGetClasses(key, query)

        private fun innerGetClasses(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): List<DexClass> = getInternalList(
            key = key,
            buildQuery = query?.let { { FindClass().apply(query) } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrThrow()

        @JvmSynthetic
        fun getField(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField = innerGetField(key, query)

        private fun innerGetField(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): DexField = getInternal(
            key = key,
            allowNull = false,
            buildQuery = query?.let { { FindField().apply(query) } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()!!

        @JvmSynthetic
        fun getFields(
            key: String,
            query: (FindField.() -> Unit)
        ): List<DexField> = innerGetFields(key, query)

        private fun innerGetFields(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): List<DexField> = getInternalList(
            key = key,
            buildQuery = query?.let { { FindField().apply(query) } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrThrow()

        @JvmSynthetic
        fun getMethodOrNull(
            query: FindMethod.() -> Unit
        ): DexMethod? = getInternal(
            key = null,
            allowNull = true,
            buildQuery = { FindMethod().apply(query) },
            executor = { b, q -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassOrNull(
            query: FindClass.() -> Unit
        ): DexClass? = getInternal(
            key = null,
            allowNull = true,
            buildQuery = { FindClass().apply(query) },
            executor = { b, q -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldOrNull(
            query: FindField.() -> Unit
        ): DexField? = getInternal(
            key = null,
            allowNull = true,
            buildQuery = { FindField().apply(query) },
            executor = { b, q -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodOrNull(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod? = innerGetMethodOrNull(key, query)

        private fun innerGetMethodOrNull(
            key: String,
            query: (FindMethod.() -> Unit)? = null
        ): DexMethod? = getInternal(
            key = key,
            allowNull = true,
            buildQuery = query?.let { { FindMethod().apply(query) } },
            executor = { b, q: FindMethod -> b.findMethod(q) },
            mapper = { it.toDexMethod() },
        ).getOrNull()

        @JvmSynthetic
        fun getClassOrNull(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass? = innerGetClassOrNull(key, query)

        private fun innerGetClassOrNull(
            key: String,
            query: (FindClass.() -> Unit)? = null
        ): DexClass? = getInternal(
            key = key,
            allowNull = true,
            buildQuery = query?.let { { FindClass().apply(query) } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() },
        ).getOrNull()

        @JvmSynthetic
        fun getFieldOrNull(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField? = innerGetFieldOrNull(key, query)

        private fun innerGetFieldOrNull(
            key: String,
            query: (FindField.() -> Unit)? = null
        ): DexField? = getInternal(
            key = key,
            allowNull = true,
            buildQuery = query?.let { { FindField().apply(query) } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() },
        ).getOrNull()

        @JvmSynthetic
        fun getMethodDirect(
            key: String,
            query: DexKitBridge.() -> MethodDataList
        ): DexMethod = innerGetMethodDirect(key, query)

        private fun innerGetMethodDirect(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): DexMethod = getDirectInternal(
            key = key,
            allowNull = false,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()!!

        @JvmSynthetic
        fun getMethodsDirect(
            key: String,
            query: DexKitBridge.() -> MethodDataList
        ): List<DexMethod> = innerGetMethodsDirect(key, query)

        private fun innerGetMethodsDirect(
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
            query: DexKitBridge.() -> ClassDataList
        ): DexClass = innerGetClassDirect(key, query)

        private fun innerGetClassDirect(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): DexClass = getDirectInternal(
            key = key,
            allowNull = false,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()!!

        @JvmSynthetic
        fun getClassesDirect(
            key: String,
            query: DexKitBridge.() -> ClassDataList
        ): List<DexClass> = innerGetClassesDirect(key, query)

        private fun innerGetClassesDirect(
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
            query: DexKitBridge.() -> FieldDataList
        ): DexField = innerGetFieldDirect(key, query)

        private fun innerGetFieldDirect(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): DexField = getDirectInternal(
            key = key,
            allowNull = false,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()!!

        @JvmSynthetic
        fun getFieldsDirect(
            key: String,
            query: DexKitBridge.() -> FieldDataList
        ): List<DexField> = innerGetFieldsDirect(key, query)

        private fun innerGetFieldsDirect(
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
            query: DexKitBridge.() -> MethodDataList
        ): DexMethod? = innerGetMethodDirectOrNull(key, query)

        private fun innerGetMethodDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> MethodDataList)? = null
        ): DexMethod? = getDirectInternal(
            key = key,
            allowNull = true,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrNull()

        @JvmSynthetic
        fun getClassDirectOrNull(
            key: String,
            query: DexKitBridge.() -> ClassDataList
        ): DexClass? = innerGetClassDirectOrNull(key, query)

        private fun innerGetClassDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> ClassDataList)? = null
        ): DexClass? = getDirectInternal(
            key = key,
            allowNull = true,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrNull()

        @JvmSynthetic
        fun getFieldDirectOrNull(
            key: String,
            query: DexKitBridge.() -> FieldDataList
        ): DexField? = innerGetFieldDirectOrNull(key, query)

        private fun innerGetFieldDirectOrNull(
            key: String,
            query: (DexKitBridge.() -> FieldDataList)? = null
        ): DexField? = getDirectInternal(
            key = key,
            allowNull = true,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrNull()

        // endregion
    }
}