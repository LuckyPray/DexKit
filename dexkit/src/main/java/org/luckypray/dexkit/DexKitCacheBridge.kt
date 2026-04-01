@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")

package org.luckypray.dexkit

import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.exceptions.NoResultException
import org.luckypray.dexkit.exceptions.NonUniqueResultException
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
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object DexKitCacheBridge {
    data class CachePolicy(
        val cacheSuccess: Boolean = true,
        val failurePolicy: CacheFailurePolicy = CacheFailurePolicy.NONE,
    )

    enum class CacheFailurePolicy {
        NONE,
        QUERY_ONLY,
        ALL,
    }

    enum class QueryKind {
        METHOD_SINGLE,
        CLASS_SINGLE,
        FIELD_SINGLE,
        METHOD_LIST,
        CLASS_LIST,
        FIELD_LIST,
        METHOD_BATCH,
        CLASS_BATCH,
    }

    enum class ResultSource {
        CACHE,
        QUERY,
    }

    data class QuerySuccessInfo(
        val appTag: String,
        val queryKind: QueryKind,
        val key: String?,
        val source: ResultSource,
        val resultCount: Int,
    )

    data class QueryFailureInfo(
        val appTag: String,
        val queryKind: QueryKind,
        val key: String?,
        val source: ResultSource,
        val error: Throwable,
    )

    open class Listener {
        open fun onQuerySuccess(info: QuerySuccessInfo) {}
        open fun onQueryFailure(info: QueryFailureInfo) {}
        open fun onBridgeCreated(appTag: String) {}
        open fun onBridgeReleased(appTag: String) {}
        open fun onBridgeDestroyed(appTag: String) {}
    }

    private lateinit var cache: Cache
    private val scheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1) { r ->
        Thread(r, "DexKit-Reaper").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }
    private val lock = ReentrantReadWriteLock()
    private val strongPool = ConcurrentHashMap<String, RecyclableBridge>()
    private val weakPool = ConcurrentHashMap<String, KeyedWeakReference>()
    private val refQueue = ReferenceQueue<RecyclableBridge>()
    private val listeners = CopyOnWriteArraySet<Listener>()

    private class KeyedWeakReference(
        val key: String,
        referent: RecyclableBridge,
        q: ReferenceQueue<RecyclableBridge>
    ) : WeakReference<RecyclableBridge>(referent, q)

    private fun reapCleared() {
        while (true) {
            val ref = refQueue.poll() ?: break
            val keyed = ref as? KeyedWeakReference ?: continue
            weakPool.remove(keyed.key, keyed)
        }
    }

    private fun tryPromoteFromWeakPool(appTag: String): RecyclableBridge? {
        reapCleared()

        val ref = weakPool[appTag] ?: return null
        val candidate = ref.get()
        if (candidate == null) {
            weakPool.remove(appTag, ref)
            return null
        }

        if (candidate.isRetired()) {
            weakPool.remove(appTag, ref)
            return null
        }

        val prev = strongPool.putIfAbsent(appTag, candidate)
        if (prev == null) return candidate
        if (!prev.isRetired()) return prev
        strongPool.remove(appTag, prev)
        return null
    }

    private fun obtainBridge(
        appTag: String,
        factory: () -> RecyclableBridge
    ): RecyclableBridge {
        require(::cache.isInitialized) { "Wrapper must be init(cache) first" }
        while (true) {
            strongPool[appTag]?.let { bridge ->
                if (!bridge.isRetired()) return bridge
                strongPool.remove(appTag, bridge)
            }
            tryPromoteFromWeakPool(appTag)?.let { return it }

            val newBridge = factory()
            val prev = strongPool.putIfAbsent(appTag, newBridge)
            if (prev == null) return newBridge
            if (!prev.isRetired()) return prev
            strongPool.remove(appTag, prev)
        }
    }

    @JvmStatic
    var idleTimeoutMillis: Long = 5_000L

    @JvmStatic
    var cachePolicy: CachePolicy = CachePolicy()

    @JvmStatic
    fun init(cache: Cache) {
        this.cache = cache
    }

    @JvmStatic
    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun clearListeners() {
        listeners.clear()
    }

    private inline fun notifyListeners(block: Listener.() -> Unit) {
        listeners.forEach { listener ->
            runCatching { listener.block() }
        }
    }

    @JvmStatic
    fun create(appTag: String, path: String): RecyclableBridge {
        return obtainBridge(appTag) {
            RecyclableBridge.create(appTag, path)
        }
    }

    @JvmStatic
    fun create(appTag: String, dexArray: Array<ByteArray>): RecyclableBridge {
        return obtainBridge(appTag) {
            RecyclableBridge.create(appTag, dexArray)
        }
    }

    @JvmStatic
    fun create(appTag: String, classLoader: ClassLoader): RecyclableBridge {
        return obtainBridge(appTag) {
            RecyclableBridge.create(appTag, classLoader)
        }
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
            private const val CACHE_NO_RESULT = "CACHE_NO_RESULT"
            private const val CACHE_NON_UNIQUE = "CACHE_NON_UNIQUE"

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

        private val retired = AtomicBoolean(false)
        fun isRetired(): Boolean = retired.get()

        private val lifecycleLock = Any()
        private var activeCalls = 0
        private var generation = 0L
        private var releaseRequested = false
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

        private fun ensureUsable() {
            check(!isRetired()) { "RecyclableBridge is destroyed" }
        }

        private inline fun <T> observeResult(
            queryKind: QueryKind,
            key: String?,
            source: ResultSource,
            result: Result<T>,
            countOf: (T) -> Int
        ): Result<T> {
            result.fold(
                onSuccess = { value ->
                    notifyListeners {
                        onQuerySuccess(
                            QuerySuccessInfo(
                                appTag = appTag,
                                queryKind = queryKind,
                                key = key,
                                source = source,
                                resultCount = countOf(value),
                            )
                        )
                    }
                },
                onFailure = { error ->
                    notifyListeners {
                        onQueryFailure(
                            QueryFailureInfo(
                                appTag = appTag,
                                queryKind = queryKind,
                                key = key,
                                source = source,
                                error = error,
                            )
                        )
                    }
                }
            )
            return result
        }

        private fun beginUse() {
            synchronized(lifecycleLock) {
                ensureUsable()
                generation++
                reaperFuture?.cancel(false)
                reaperFuture = null
                activeCalls++
            }
        }

        private fun endUse() {
            var released = false
            synchronized(lifecycleLock) {
                check(activeCalls > 0) { "activeCalls underflow" }
                activeCalls--
                if (activeCalls != 0) return
                if (isRetired()) {
                    released = releaseBridgeLocked()
                } else if (releaseRequested) {
                    releaseRequested = false
                    released = releaseBridgeLocked()
                    moveToWeakPoolLocked()
                } else {
                    scheduleRetireLocked()
                }
            }
            if (released) {
                notifyListeners { onBridgeReleased(appTag) }
            }
        }

        private inline fun <R> acquireBridge(block: (DexKitBridge) -> R): R = acquire {
            var created = false
            val b = synchronized(lifecycleLock) {
                _bridge ?: createBridge().also {
                    _bridge = it
                    created = true
                }
            }
            if (created) {
                notifyListeners { onBridgeCreated(appTag) }
            }
            block(b)
        }

        private inline fun <R> acquire(block: RecyclableBridge.() -> R): R {
            beginUse()
            return try {
                this.block()
            } finally {
                endUse()
            }
        }

        private fun releaseBridgeLocked(): Boolean {
            val bridge = _bridge ?: return false
            bridge.close()
            _bridge = null
            return true
        }

        private fun scheduleRetireLocked() {
            val token = generation
            val bridge = this
            reaperFuture = scheduler.schedule({
                val released = synchronized(lifecycleLock) {
                    if (isRetired()) return@schedule
                    if (activeCalls != 0) return@schedule
                    if (generation != token) return@schedule
                    reaperFuture = null
                    if (releaseRequested) return@schedule
                    val removed = strongPool.remove(appTag, bridge)
                    if (!removed || isRetired()) return@schedule
                    val result = releaseBridgeLocked()
                    reapCleared()
                    weakPool[appTag] = KeyedWeakReference(appTag, bridge, refQueue)
                    result
                }
                if (released) {
                    notifyListeners { onBridgeReleased(appTag) }
                }
            }, idleTimeoutMillis, TimeUnit.MILLISECONDS)
        }

        private fun moveToWeakPoolLocked() {
            strongPool.remove(appTag, this)
            reapCleared()
            if (!isRetired()) {
                weakPool[appTag] = KeyedWeakReference(appTag, this, refQueue)
            }
        }

        override fun close() {
            var released = false
            synchronized(lifecycleLock) {
                ensureUsable()
                generation++
                reaperFuture?.cancel(false)
                reaperFuture = null
                if (activeCalls == 0) {
                    releaseRequested = false
                    released = releaseBridgeLocked()
                    moveToWeakPoolLocked()
                } else {
                    releaseRequested = true
                }
            }
            if (released) {
                notifyListeners { onBridgeReleased(appTag) }
            }
        }

        fun destroy() {
            if (retired.compareAndSet(false, true)) {
                var released = false
                synchronized(lifecycleLock) {
                    generation++
                    releaseRequested = false
                    reaperFuture?.cancel(false)
                    reaperFuture = null
                    if (activeCalls == 0) {
                        released = releaseBridgeLocked()
                    }
                }
                strongPool.remove(appTag, this)
                weakPool.remove(appTag)
                if (released) {
                    notifyListeners { onBridgeReleased(appTag) }
                }
                notifyListeners { onBridgeDestroyed(appTag) }
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
            query: FindMethodBuilder
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
        )

        fun getClassesOrEmpty(
            query: FindClassBuilder
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
        )

        fun getFieldsOrEmpty(
            query: FindFieldBuilder
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
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

            return getInternalSingle(
                queryKind = QueryKind.METHOD_SINGLE,
                key = key,
                mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
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
                queryKind = QueryKind.METHOD_LIST,
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
        ): DexClass? = getInternalSingle(
            queryKind = QueryKind.CLASS_SINGLE,
            key = key,
            mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetClasses(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindClass? = null
        ): List<DexClass> = getInternalList(
            queryKind = QueryKind.CLASS_LIST,
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
        ): DexField? = getInternalSingle(
            queryKind = QueryKind.FIELD_SINGLE,
            key = key,
            mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetFields(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindField? = null
        ): List<DexField> = getInternalList(
            queryKind = QueryKind.FIELD_LIST,
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
            queryKind = QueryKind.CLASS_BATCH,
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindClassUsingStrings -> b.batchFindClassUsingStrings(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetBatchUsingStringsMethods(
            key: String?,
            query: BatchFindMethodUsingStrings? = null
        ): Map<String, List<DexMethod>> = getInternalMap(
            queryKind = QueryKind.METHOD_BATCH,
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindMethodUsingStrings -> b.batchFindMethodUsingStrings(q) },
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetMethodDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> MethodData?)? = null
        ): DexMethod? = getDirectInternalSingle(
            queryKind = QueryKind.METHOD_SINGLE,
            key = key,
            mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetClassDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> ClassData?)? = null
        ): DexClass? = getDirectInternalSingle(
            queryKind = QueryKind.CLASS_SINGLE,
            key = key,
            mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetFieldDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> FieldData?)? = null
        ): DexField? = getDirectInternalSingle(
            queryKind = QueryKind.FIELD_SINGLE,
            key = key,
            mode = if (allowNull) SingleResolveMode.NULLABLE else SingleResolveMode.REQUIRED,
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetMethodsDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<MethodData>)? = null
        ): List<DexMethod> = getDirectInternalList(
            queryKind = QueryKind.METHOD_LIST,
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
            queryKind = QueryKind.CLASS_LIST,
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
            queryKind = QueryKind.FIELD_LIST,
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

        private enum class SingleResolveMode {
            REQUIRED,
            NULLABLE,
        }

        private sealed interface SingleOutcome<out T : ISerializable> {
            data class Value<T : ISerializable>(val value: T) : SingleOutcome<T>
            data class NoResult(
                val exception: NoResultException =
                    NoResultException("No result found for query")
            ) : SingleOutcome<Nothing>
            data class NonUnique(
                val exception: NonUniqueResultException =
                    NonUniqueResultException("query did not return a unique result")
            ) : SingleOutcome<Nothing>
        }

        private fun <T : ISerializable> resolveSingleOutcome(
            outcome: SingleOutcome<T>,
            mode: SingleResolveMode
        ): Result<T?> {
            return when (outcome) {
                is SingleOutcome.Value -> Result.success(outcome.value)
                is SingleOutcome.NoResult -> when (mode) {
                    SingleResolveMode.NULLABLE -> Result.success(null)
                    SingleResolveMode.REQUIRED -> Result.failure(outcome.exception)
                }

                is SingleOutcome.NonUnique -> when (mode) {
                    SingleResolveMode.NULLABLE -> Result.success(null)
                    SingleResolveMode.REQUIRED -> Result.failure(outcome.exception)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : ISerializable> parseSingleOutcome(raw: String): SingleOutcome<T> {
            return when (raw) {
                CACHE_NO_RESULT -> SingleOutcome.NoResult()
                CACHE_NON_UNIQUE -> SingleOutcome.NonUnique()
                else -> SingleOutcome.Value(ISerializable.deserializeAs(raw) as T)
            }
        }

        private fun shouldCacheFailure(stableQueryIdentity: Boolean): Boolean {
            return when (cachePolicy.failurePolicy) {
                CacheFailurePolicy.NONE -> false
                CacheFailurePolicy.QUERY_ONLY -> stableQueryIdentity
                CacheFailurePolicy.ALL -> true
            }
        }

        private fun <T : ISerializable> getCachedSingle(
            queryKind: QueryKind,
            key: String?,
            cacheKey: String,
            mode: SingleResolveMode,
            canCacheFailure: Boolean,
            loader: (() -> SingleOutcome<T>)? = null,
        ): Result<T?> {
            ensureUsable()

            fun <U : ISerializable> innerGet(cacheKey: String): SingleOutcome<U>? {
                cache.get(cacheKey, null)?.let { raw ->
                    return parseSingleOutcome(raw)
                }
                return null
            }

            lock.read {
                innerGet<T>(cacheKey)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = resolveSingleOutcome(it, mode),
                        countOf = { value -> if (value == null) 0 else 1 }
                    )
                }
            }

            loader ?: return observeResult(
                queryKind = queryKind,
                key = key,
                source = ResultSource.CACHE,
                result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey")),
                countOf = { value: T? -> if (value == null) 0 else 1 }
            )

            val loaded = runCatching { loader() }

            return lock.write {
                innerGet<T>(cacheKey)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = resolveSingleOutcome(it, mode),
                        countOf = { value -> if (value == null) 0 else 1 }
                    )
                }
                observeResult(
                    queryKind = queryKind,
                    key = key,
                    source = ResultSource.QUERY,
                    result = loaded.fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is SingleOutcome.Value -> {
                                    if (cachePolicy.cacheSuccess) {
                                        cache.put(cacheKey, outcome.value.serialize())
                                    }
                                }

                                is SingleOutcome.NoResult -> {
                                    if (canCacheFailure) {
                                        cache.put(cacheKey, CACHE_NO_RESULT)
                                    }
                                }

                                is SingleOutcome.NonUnique -> {
                                    if (canCacheFailure) {
                                        cache.put(cacheKey, CACHE_NON_UNIQUE)
                                    }
                                }
                            }
                            resolveSingleOutcome(outcome, mode)
                        },
                        onFailure = { Result.failure(it) }
                    ),
                    countOf = { value -> if (value == null) 0 else 1 }
                )
            }
        }

        private fun <T : ISerializable> getCachedList(
            queryKind: QueryKind,
            key: String?,
            cacheKey: String,
            allowEmpty: Boolean,
            loader: (() -> List<T>)? = null,
        ): Result<List<T>> {
            ensureUsable()

            fun <U : ISerializable> innerGet(cacheKey: String, allowEmpty: Boolean): Result<List<U>>? {
                cache.getList(cacheKey, null)?.let { rawList ->
                    val list = rawList.map { ISerializable.deserializeAs<U>(it) }
                    if (list.isEmpty() && !allowEmpty) {
                        return Result.failure(IllegalStateException(
                            "cached empty for key: $cacheKey but empty not allowed"
                        ))
                    }
                    return Result.success(list)
                }
                return null
            }

            lock.read {
                innerGet<T>(cacheKey, allowEmpty)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = it,
                        countOf = { list -> list.size }
                    )
                }
            }
            loader ?: return observeResult(
                queryKind = queryKind,
                key = key,
                source = ResultSource.CACHE,
                result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey")),
                countOf = { list: List<T> -> list.size }
            )

            val loaded = runCatching { loader() }

            return lock.write {
                innerGet<T>(cacheKey, allowEmpty)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = it,
                        countOf = { list -> list.size }
                    )
                }
                observeResult(
                    queryKind = queryKind,
                    key = key,
                    source = ResultSource.QUERY,
                    result = loaded.fold(
                        onSuccess = { list ->
                            if (list.isEmpty() && !allowEmpty) {
                                Result.failure(
                                    IllegalStateException(
                                        "query returned empty for key: $cacheKey but empty not allowed"
                                    )
                                )
                            } else {
                                if (cachePolicy.cacheSuccess) {
                                    cache.putList(cacheKey, list.map(ISerializable::serialize))
                                }
                                Result.success(list)
                            }
                        },
                        onFailure = { Result.failure(it) }
                    ),
                    countOf = { list -> list.size }
                )
            }
        }

        private fun <T : ISerializable> getCachedMap(
            queryKind: QueryKind,
            key: String?,
            cacheKey: String,
            loader: (() -> Map<String, List<T>>)? = null,
        ): Result<Map<String, List<T>>> {
            ensureUsable()

            fun <U : ISerializable> innerGetMap(cacheKey: String): Map<String, List<U>>? {
                cache.getList("$cacheKey:keys", null)?.let { keys ->
                    val map = mutableMapOf<String, List<U>>()
                    keys.forEach { groupKey ->
                        val dataList = cache.getList("$cacheKey:$groupKey", null)
                            ?.map { ISerializable.deserializeAs<U>(it) }
                            ?: emptyList()
                        map.put(groupKey, dataList)
                    }
                    return map
                }
                return null
            }

            lock.read {
                innerGetMap<T>(cacheKey)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = Result.success(it),
                        countOf = { map -> map.values.sumOf { it.size } }
                    )
                }
            }
            loader ?: return observeResult(
                queryKind = queryKind,
                key = key,
                source = ResultSource.CACHE,
                result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey")),
                countOf = { map: Map<String, List<T>> -> map.values.sumOf { it.size } }
            )

            val loaded = runCatching { loader() }

            return lock.write {
                innerGetMap<T>(cacheKey)?.let {
                    return observeResult(
                        queryKind = queryKind,
                        key = key,
                        source = ResultSource.CACHE,
                        result = Result.success(it),
                        countOf = { map -> map.values.sumOf { it.size } }
                    )
                }
                observeResult(
                    queryKind = queryKind,
                    key = key,
                    source = ResultSource.QUERY,
                    result = loaded.fold(
                        onSuccess = { map ->
                            val oldKeys = cache.getList("$cacheKey:keys", null) ?: emptyList()
                            val keys = mutableListOf<String>()
                            if (cachePolicy.cacheSuccess) {
                                map.entries.forEach { (groupKey, value) ->
                                    keys.add(groupKey)
                                    cache.putList("$cacheKey:$groupKey", value.map { it.serialize() })
                                }
                                (oldKeys - keys.toSet()).forEach { cache.remove("$cacheKey:$it") }
                                cache.putList("$cacheKey:keys", keys)
                            }
                            Result.success(map)
                        },
                        onFailure = { Result.failure(it) }
                    ),
                    countOf = { map -> map.values.sumOf { it.size } }
                )
            }
        }

        private fun spKeyOf(kind: String, key: String?, query: BaseFinder? = null): String {
            if (key != null) return "$appTag:$kind:$key"
            requireNotNull(query) {
                "Either key or query must be provided for auto-generated cache key."
            }
            return "$appTag:$kind:${query.hashKey()}"
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalSingle(
            queryKind: QueryKind,
            key: String?,
            mode: SingleResolveMode,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<R?> {
            val query = buildQuery?.invoke()
            // :s: -> single
            val spKey = spKeyOf("s", key, query)
            val loader: (() -> SingleOutcome<R>)? = query?.let {
                {
                    acquireBridge {
                        val list = executor(it, query)
                        val ret = list.firstOrNull()
                            ?: return@acquireBridge SingleOutcome.NoResult()
                        for (i in 1 until list.size) {
                            if (ret != list[i]) {
                                return@acquireBridge SingleOutcome.NonUnique(
                                    NonUniqueResultException(list.size)
                                )
                            }
                        }
                        SingleOutcome.Value(mapper(ret))
                    }
                }
            }
            val stableQueryIdentity = key == null && query != null
            return getCachedSingle(
                queryKind = queryKind,
                key = key,
                cacheKey = spKey,
                mode = mode,
                canCacheFailure = shouldCacheFailure(stableQueryIdentity),
                loader = loader
            )
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalList(
            queryKind: QueryKind,
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
            return getCachedList(
                queryKind = queryKind,
                key = key,
                cacheKey = spKey,
                allowEmpty = allowEmpty,
                loader = loader
            )
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalMap(
            queryKind: QueryKind,
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
            return getCachedMap(
                queryKind = queryKind,
                key = key,
                cacheKey = spKey,
                loader = loader
            )
        }

        private inline fun <D, R : ISerializable> getDirectInternalSingle(
            queryKind: QueryKind,
            key: String,
            mode: SingleResolveMode,
            noinline executor: (DexKitBridge.() -> D?)?,
            noinline mapper: (D) -> R
        ): Result<R?> {
            // :s: -> single
            val spKey = spKeyOf("s", key)
            val loader: (() -> SingleOutcome<R>)? = executor?.let {
                {
                    try {
                        acquireBridge {
                            val value = it.let(executor)
                                ?: return@acquireBridge SingleOutcome.NoResult()
                            SingleOutcome.Value(mapper(value))
                        }
                    } catch (e: NoResultException) {
                        SingleOutcome.NoResult(e)
                    } catch (e: NonUniqueResultException) {
                        SingleOutcome.NonUnique(e)
                    }
                }
            }
            return getCachedSingle(
                queryKind = queryKind,
                key = key,
                cacheKey = spKey,
                mode = mode,
                canCacheFailure = shouldCacheFailure(stableQueryIdentity = false),
                loader = loader
            )
        }

        private inline fun <D, R : ISerializable> getDirectInternalList(
            queryKind: QueryKind,
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
            return getCachedList(
                queryKind = queryKind,
                key = key,
                cacheKey = spKey,
                allowEmpty = allowEmpty,
                loader = loader
            )
        }

        // endregion
    }
}
