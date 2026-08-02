package org.luckypray.dexkit.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DexKitExperimentalApi::class)
class CacheBridgeRegistryTest {

    private fun newBridge(appTag: String): DexKitCacheBridge.RecyclableBridge {
        return DexKitCacheBridge.RecyclableBridge.create(appTag, "unused")
    }

    @Test
    fun unregisterOldHolderKeepsCurrentHolder() {
        val appTag = "registry-${UUID.randomUUID()}"
        val first = CacheBridgeRegistry.obtainBridge(appTag) { newBridge(appTag) }
        CacheBridgeRegistry.unregister(appTag, first)
        val second = CacheBridgeRegistry.obtainBridge(appTag) { newBridge(appTag) }

        try {
            CacheBridgeRegistry.unregister(appTag, first)
            assertSame(
                second,
                CacheBridgeRegistry.obtainBridge(appTag) {
                    throw AssertionError("current holder was removed")
                }
            )
        } finally {
            second.destroy()
        }
    }

    @Test
    fun demoteAndObtainKeepOneHolder() {
        val appTag = "registry-${UUID.randomUUID()}"
        val holder = CacheBridgeRegistry.obtainBridge(appTag) { newBridge(appTag) }
        val factoryCalls = AtomicInteger()
        val executor = Executors.newFixedThreadPool(2)

        try {
            repeat(500) {
                CacheBridgeRegistry.promote(appTag, holder)
                val start = CountDownLatch(1)
                val demote = executor.submit {
                    start.await()
                    CacheBridgeRegistry.demote(appTag, holder)
                }
                val obtain = executor.submit<DexKitCacheBridge.RecyclableBridge> {
                    start.await()
                    CacheBridgeRegistry.obtainBridge(appTag) {
                        factoryCalls.incrementAndGet()
                        newBridge(appTag)
                    }
                }
                start.countDown()

                demote.get(5, TimeUnit.SECONDS)
                assertSame(holder, obtain.get(5, TimeUnit.SECONDS))
            }
            assertSame(
                holder,
                CacheBridgeRegistry.obtainBridge(appTag) {
                    factoryCalls.incrementAndGet()
                    newBridge(appTag)
                }
            )
            assertEquals(0, factoryCalls.get())
        } finally {
            holder.destroy()
            executor.shutdownNow()
        }
    }
}
