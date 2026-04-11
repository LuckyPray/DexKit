package org.luckypray.dexkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.exceptions.NoResultException
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(DexKitExperimentalApi::class)
class DexKitCacheBridgeTest {
    private class MemoryCache : DexKitCacheBridge.Cache {
        private val values = ConcurrentHashMap<String, String>()
        private val lists = ConcurrentHashMap<String, List<String>>()

        override fun getString(key: String, default: String?): String? = values[key] ?: default

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun getStringList(key: String, default: List<String>?): List<String>? = lists[key] ?: default

        override fun putStringList(key: String, value: List<String>) {
            lists[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
            lists.remove(key)
        }

        override fun getAllKeys(): Collection<String> = values.keys + lists.keys

        override fun clearAll() {
            values.clear()
            lists.clear()
        }

        fun storedValues(): Collection<String> = values.values
    }

    companion object {
        private const val CACHE_NO_RESULT = "CACHE_NO_RESULT"
        private val sharedCache = MemoryCache()

        init {
            loadLibrary("dexkit")
            DexKitCacheBridge.init(sharedCache)
        }
    }

    init {
        sharedCache.clearAll()
        DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy()
        DexKitCacheBridge.idleTimeoutMillis = 5_000L
        DexKitCacheBridge.clearListeners()
    }

    private fun newAppTag(): String = "cache-test-${UUID.randomUUID()}"

    private fun demoApkPath(): String {
        val apkDir = System.getProperty("apk.path") ?: "dexkit/apk"
        return File(apkDir, "demo.apk").absolutePath
    }

    @Test
    fun useAllowsLaterUncachedQuery() {
        val bridge = DexKitCacheBridge.create(newAppTag(), demoApkPath())

        bridge.use {
            assertEquals(
                "org.luckypray.dexkit.demo.PlayActivity",
                it.getClass("play") {
                    matcher {
                        className("org.luckypray.dexkit.demo.PlayActivity")
                    }
                }.typeName
            )
        }

        try {
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                bridge.getClass("main") {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
        } finally {
            bridge.destroy()
        }
    }

    @Test
    fun createAfterCloseReusesSameWrapper() {
        val appTag = newAppTag()
        val first = DexKitCacheBridge.create(appTag, demoApkPath())

        assertEquals(
            "org.luckypray.dexkit.demo.PlayActivity",
            first.getClass("play") {
                matcher {
                    className("org.luckypray.dexkit.demo.PlayActivity")
                }
            }.typeName
        )

        first.close()

        val second = DexKitCacheBridge.create(appTag, demoApkPath())
        try {
            assertSame(first, second)
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                second.getClass("main") {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
        } finally {
            second.destroy()
        }
    }

    @Test
    fun destroyMakesWrapperUnavailableAndCreateReturnsNewInstance() {
        val appTag = newAppTag()
        val first = DexKitCacheBridge.create(appTag, demoApkPath())

        first.destroy()

        try {
            first.getClass("main") {
                matcher {
                    className("org.luckypray.dexkit.demo.MainActivity")
                }
            }
            throw AssertionError("Destroyed bridge should reject further access")
        } catch (e: IllegalStateException) {
            assertNotEquals(-1, e.message?.indexOf("destroyed") ?: -1)
        }

        val second = DexKitCacheBridge.create(appTag, demoApkPath())
        try {
            assertNotSame(first, second)
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                second.getClass("main") {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
        } finally {
            second.destroy()
        }
    }

    @Test
    fun queryOnlyCachesStructuredAutoKeyNoResultFailure() {
        val cache = sharedCache
        DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
            failurePolicy = DexKitCacheBridge.CacheFailurePolicy.QUERY_ONLY
        )
        val bridge = DexKitCacheBridge.create(newAppTag(), demoApkPath())

        try {
            try {
                bridge.getClass {
                    matcher {
                        className("org.luckypray.dexkit.demo.DoesNotExist")
                    }
                }
                throw AssertionError("Expected no result")
            } catch (_: NoResultException) {
            }
            assertEquals(1, cache.getAllKeys().size)
            assertTrue(cache.storedValues().contains(CACHE_NO_RESULT))
        } finally {
            bridge.destroy()
            DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy()
        }
    }

    @Test
    fun queryOnlyDoesNotCacheStructuredExplicitKeyFailure() {
        val cache = sharedCache
        DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
            failurePolicy = DexKitCacheBridge.CacheFailurePolicy.QUERY_ONLY
        )
        val bridge = DexKitCacheBridge.create(newAppTag(), demoApkPath())

        try {
            try {
                bridge.getClass("missing") {
                    matcher {
                        className("org.luckypray.dexkit.demo.DoesNotExist")
                    }
                }
                throw AssertionError("Expected no result")
            } catch (_: NoResultException) {
            }
            assertTrue(cache.getAllKeys().isEmpty())
        } finally {
            bridge.destroy()
            DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy()
        }
    }

    @Test
    fun allCachesDirectNoResultFailure() {
        val cache = sharedCache
        DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
            failurePolicy = DexKitCacheBridge.CacheFailurePolicy.ALL
        )
        val bridge = DexKitCacheBridge.create(newAppTag(), demoApkPath())
        var calls = 0

        try {
            repeat(2) {
                try {
                    bridge.getClassDirect("missing-direct") {
                        calls++
                        throw NoResultException("missing direct")
                    }
                    throw AssertionError("Expected no result")
                } catch (_: NoResultException) {
                }
            }
            assertEquals(1, calls)
            assertEquals(1, cache.getAllKeys().size)
            assertTrue(cache.storedValues().contains(CACHE_NO_RESULT))
        } finally {
            bridge.destroy()
            DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy()
        }
    }

    @Test
    fun listenerReceivesCacheSuccessEvent() {
        val appTag = newAppTag()
        val key = "main"
        val successes = CopyOnWriteArrayList<DexKitCacheBridge.QuerySuccessEvent>()
        val failures = CopyOnWriteArrayList<DexKitCacheBridge.QueryFailureEvent>()
        val listener = object : DexKitCacheBridge.CacheBridgeListener() {
            override fun onQuerySuccess(info: DexKitCacheBridge.QuerySuccessEvent) {
                successes += info
            }

            override fun onQueryFailure(info: DexKitCacheBridge.QueryFailureEvent) {
                failures += info
            }
        }
        DexKitCacheBridge.addListener(listener)
        val bridge = DexKitCacheBridge.create(appTag, demoApkPath())

        try {
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                bridge.getClass(key) {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
            successes.clear()
            failures.clear()

            assertEquals("org.luckypray.dexkit.demo.MainActivity", bridge.getClass(key).typeName)
            assertEquals(1, successes.size)
            assertTrue(failures.isEmpty())
            val event = successes.single()
            assertEquals(appTag, event.appTag)
            assertEquals(DexKitCacheBridge.QueryKind.CLASS_SINGLE, event.queryKind)
            assertEquals(key, event.requestKey)
            assertEquals(DexKitCacheBridge.ResultSource.CACHE, event.source)
            assertEquals(1, event.matchCount)
        } finally {
            bridge.destroy()
            DexKitCacheBridge.removeListener(listener)
        }
    }

    @Test
    fun encodedCacheKeysHandleColonInAppTagAndKey() {
        val cache = sharedCache
        val appTag = "cache:test:${UUID.randomUUID()}"
        val key = "main:activity"
        val bridge = DexKitCacheBridge.create(appTag, demoApkPath())

        try {
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                bridge.getClass(key) {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
            assertTrue(cache.getAllKeys().isNotEmpty())
            assertTrue(cache.getAllKeys().none { it.contains(appTag) })
            assertTrue(cache.getAllKeys().none { it.contains(key) })
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                bridge.getClass(key).typeName
            )

            DexKitCacheBridge.clearCache(appTag)
            assertTrue(cache.getAllKeys().isEmpty())
        } finally {
            bridge.destroy()
        }
    }

    @Test
    fun listenerReceivesQueryAndLifecycleEvents() {
        val successes = CopyOnWriteArrayList<DexKitCacheBridge.QuerySuccessEvent>()
        val failures = CopyOnWriteArrayList<DexKitCacheBridge.QueryFailureEvent>()
        val created = CopyOnWriteArrayList<String>()
        val released = CopyOnWriteArrayList<String>()
        val destroyed = CopyOnWriteArrayList<String>()
        val listener = object : DexKitCacheBridge.CacheBridgeListener() {
            override fun onQuerySuccess(info: DexKitCacheBridge.QuerySuccessEvent) {
                successes += info
            }

            override fun onQueryFailure(info: DexKitCacheBridge.QueryFailureEvent) {
                failures += info
            }

            override fun onBridgeCreated(appTag: String) {
                created += appTag
            }

            override fun onBridgeReleased(appTag: String) {
                released += appTag
            }

            override fun onBridgeDestroyed(appTag: String) {
                destroyed += appTag
            }
        }
        DexKitCacheBridge.addListener(listener)
        val appTag = newAppTag()
        val bridge = DexKitCacheBridge.create(appTag, demoApkPath())

        try {
            assertEquals(
                "org.luckypray.dexkit.demo.MainActivity",
                bridge.getClass("main") {
                    matcher {
                        className("org.luckypray.dexkit.demo.MainActivity")
                    }
                }.typeName
            )
            bridge.close()
            bridge.destroy()

            assertTrue(created.contains(appTag))
            assertTrue(
                successes.any {
                    it.appTag == appTag &&
                        it.queryKind == DexKitCacheBridge.QueryKind.CLASS_SINGLE &&
                        it.source == DexKitCacheBridge.ResultSource.QUERY &&
                        it.matchCount == 1
                }
            )
            assertTrue(released.contains(appTag))
            assertTrue(destroyed.contains(appTag))
            assertFalse(failures.any { it.appTag == appTag })
        } finally {
            DexKitCacheBridge.removeListener(listener)
        }
    }
}
