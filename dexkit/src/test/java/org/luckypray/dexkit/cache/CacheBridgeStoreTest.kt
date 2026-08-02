package org.luckypray.dexkit.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexMethod
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

@OptIn(DexKitExperimentalApi::class)
class CacheBridgeStoreTest {
    private class MemoryCache : DexKitCacheBridge.Cache {
        private val values = LinkedHashMap<String, Any>()
        private val forcedListMisses = HashMap<String, Int>()
        var groupWritesBeforeFailure: Int? = null

        override fun getString(key: String, default: String?): String? =
            values[key] as? String ?: default

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringList(key: String, default: List<String>?): List<String>? {
            forcedListMisses[key]?.let { remaining ->
                if (remaining > 0) {
                    if (remaining == 1) {
                        forcedListMisses.remove(key)
                    } else {
                        forcedListMisses[key] = remaining - 1
                    }
                    return default
                }
            }
            return values[key] as? List<String> ?: default
        }

        override fun putStringList(key: String, value: List<String>) {
            if (":group:" in key) {
                groupWritesBeforeFailure?.let { remaining ->
                    if (remaining == 0) {
                        throw IllegalStateException("group write failed")
                    }
                    groupWritesBeforeFailure = remaining - 1
                }
            }
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun getAllKeys(): Collection<String> = values.keys

        override fun clearAll() {
            values.clear()
        }

        fun forceListMisses(key: String, count: Int) {
            forcedListMisses[key] = count
        }
    }

    @Test
    fun concurrentMissesShareOneLoad() {
        val cache = MemoryCache()
        val lock = ReentrantReadWriteLock()
        val namespace = "dkcb:single-flight"
        val loaderCalls = AtomicInteger()
        val threadCount = 8
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            val futures = (0 until threadCount).map {
                executor.submit<List<DexClass>> {
                    ready.countDown()
                    start.await()
                    CacheBridgeStore.getCachedList(
                        cache = cache,
                        lock = lock,
                        cachePolicy = DexKitCacheBridge.CachePolicy(cacheSuccess = false),
                        namespace = namespace,
                        cacheKey = "$namespace:c:l:user:key",
                        allowEmpty = false,
                        ensureUsable = {},
                        loader = {
                            loaderCalls.incrementAndGet()
                            Thread.sleep(100)
                            listOf(DexClass("Lfoo/Bar;"))
                        }
                    ).result.getOrThrow()
                }
            }
            ready.await()
            start.countDown()

            futures.forEach {
                assertEquals("foo.Bar", it.get(5, TimeUnit.SECONDS).single().typeName)
            }
            assertEquals(1, loaderCalls.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun clearPreventsInFlightLoadFromWritingBack() {
        val cache = MemoryCache()
        val lock = ReentrantReadWriteLock()
        val namespace = "dkcb:clear-race"
        val cacheKey = "$namespace:c:s:user:key"
        val loaderStarted = CountDownLatch(1)
        val releaseLoader = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val future = executor.submit<DexClass?> {
                CacheBridgeStore.getCachedSingle(
                    cache = cache,
                    lock = lock,
                    cachePolicy = DexKitCacheBridge.CachePolicy(),
                    namespace = namespace,
                    cacheKey = cacheKey,
                    mode = CacheBridgeStore.SingleResolveMode.REQUIRED,
                    canCacheFailure = false,
                    ensureUsable = {},
                    loader = {
                        loaderStarted.countDown()
                        releaseLoader.await()
                        CacheBridgeStore.SingleOutcome.Value(DexClass("Lfoo/Bar;"))
                    }
                ).result.getOrThrow()
            }

            loaderStarted.await()
            lock.write {
                CacheBridgeStore.invalidate(namespace)
                cache.remove(cacheKey)
            }
            releaseLoader.countDown()

            assertEquals("foo.Bar", future.get(5, TimeUnit.SECONDS)?.typeName)
            assertNull(cache.getString(cacheKey, null))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun corruptSingleFallsBackToLoader() {
        val cache = MemoryCache()
        val lock = ReentrantReadWriteLock()
        val namespace = "dkcb:corrupt-single"
        val cacheKey = "$namespace:m:s:user:key"
        val loaderCalls = AtomicInteger()
        cache.putString(cacheKey, "Lfoo/Bar;->broken")

        val result = CacheBridgeStore.getCachedSingle(
            cache = cache,
            lock = lock,
            cachePolicy = DexKitCacheBridge.CachePolicy(),
            namespace = namespace,
            cacheKey = cacheKey,
            mode = CacheBridgeStore.SingleResolveMode.REQUIRED,
            canCacheFailure = false,
            ensureUsable = {},
            loader = {
                loaderCalls.incrementAndGet()
                CacheBridgeStore.SingleOutcome.Value(DexMethod("Lfoo/Bar;->ok()V"))
            }
        ).result.getOrThrow()

        assertEquals("ok", result?.name)
        assertEquals(1, loaderCalls.get())
        assertEquals("Lfoo/Bar;->ok()V", cache.getString(cacheKey, null))
    }

    @Test
    fun corruptListFallsBackToLoader() {
        val cache = MemoryCache()
        val lock = ReentrantReadWriteLock()
        val namespace = "dkcb:corrupt-list"
        val cacheKey = "$namespace:m:l:user:key"
        val loaderCalls = AtomicInteger()
        cache.putStringList(cacheKey, listOf("Lfoo/Bar;->broken"))

        val result = CacheBridgeStore.getCachedList(
            cache = cache,
            lock = lock,
            cachePolicy = DexKitCacheBridge.CachePolicy(),
            namespace = namespace,
            cacheKey = cacheKey,
            allowEmpty = false,
            ensureUsable = {},
            loader = {
                loaderCalls.incrementAndGet()
                listOf(DexMethod("Lfoo/Bar;->ok()V"))
            }
        ).result.getOrThrow()

        assertEquals("ok", result.single().name)
        assertEquals(1, loaderCalls.get())
        assertEquals(listOf("Lfoo/Bar;->ok()V"), cache.getStringList(cacheKey, null))
    }

    @Test
    fun partialBatchWriteKeepsPreviousSnapshot() {
        val cache = MemoryCache()
        val lock = ReentrantReadWriteLock()
        val namespace = "dkcb:batch-snapshot"
        val cacheKey = "$namespace:c:b:user:key"
        val policy = DexKitCacheBridge.CachePolicy()
        val oldMap = linkedMapOf(
            "a" to listOf(DexClass("Lfoo/OldA;")),
            "b" to listOf(DexClass("Lfoo/OldB;")),
        )
        val newMap = linkedMapOf(
            "a" to listOf(DexClass("Lfoo/NewA;")),
            "c" to listOf(DexClass("Lfoo/NewC;")),
        )

        CacheBridgeStore.getCachedMap(
            cache = cache,
            lock = lock,
            cachePolicy = policy,
            namespace = namespace,
            cacheKey = cacheKey,
            ensureUsable = {},
            loader = { oldMap }
        ).result.getOrThrow()

        cache.forceListMisses(CacheBridgeKeys.mapGroupsKey(cacheKey), 2)
        cache.groupWritesBeforeFailure = 1
        val failed = runCatching {
            CacheBridgeStore.getCachedMap(
                cache = cache,
                lock = lock,
                cachePolicy = policy,
                namespace = namespace,
                cacheKey = cacheKey,
                ensureUsable = {},
                loader = { newMap }
            ).result.getOrThrow()
        }
        cache.groupWritesBeforeFailure = null
        assertEquals("group write failed", failed.exceptionOrNull()?.message)

        val cached = CacheBridgeStore.getCachedMap<DexClass>(
            cache = cache,
            lock = lock,
            cachePolicy = policy,
            namespace = namespace,
            cacheKey = cacheKey,
            ensureUsable = {},
            loader = null
        ).result.getOrThrow()

        assertEquals(oldMap, cached)
    }
}
