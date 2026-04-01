package org.luckypray.dexkit

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

internal object DexKitCacheBridgeRegistry {
    private val strongPool = ConcurrentHashMap<String, DexKitCacheBridge.RecyclableBridge>()
    private val weakPool = ConcurrentHashMap<String, KeyedWeakReference>()
    private val refQueue = ReferenceQueue<DexKitCacheBridge.RecyclableBridge>()

    private class KeyedWeakReference(
        val key: String,
        referent: DexKitCacheBridge.RecyclableBridge,
        q: ReferenceQueue<DexKitCacheBridge.RecyclableBridge>
    ) : WeakReference<DexKitCacheBridge.RecyclableBridge>(referent, q)

    fun removeClearedWeakRefs() {
        while (true) {
            val ref = refQueue.poll() ?: break
            val keyed = ref as? KeyedWeakReference ?: continue
            weakPool.remove(keyed.key, keyed)
        }
    }

    private fun tryPromoteFromWeakPool(appTag: String): DexKitCacheBridge.RecyclableBridge? {
        removeClearedWeakRefs()

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
            ?: return candidate
        if (!prev.isRetired()) return prev
        strongPool.remove(appTag, prev)
        return null
    }

    fun obtainBridge(
        appTag: String,
        factory: () -> DexKitCacheBridge.RecyclableBridge
    ): DexKitCacheBridge.RecyclableBridge {
        while (true) {
            strongPool[appTag]?.let { bridge ->
                if (!bridge.isRetired()) return bridge
                strongPool.remove(appTag, bridge)
            }
            tryPromoteFromWeakPool(appTag)?.let { return it }

            val newBridge = factory()
            val prev = strongPool.putIfAbsent(appTag, newBridge)
                ?: return newBridge
            if (!prev.isRetired()) return prev
            strongPool.remove(appTag, prev)
        }
    }

    fun removeStrong(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge): Boolean {
        return strongPool.remove(appTag, bridge)
    }

    fun moveToWeak(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge) {
        strongPool.remove(appTag, bridge)
        putWeak(appTag, bridge)
    }

    fun putWeak(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge) {
        removeClearedWeakRefs()
        weakPool[appTag] = KeyedWeakReference(appTag, bridge, refQueue)
    }

    fun removeWeak(appTag: String) {
        weakPool.remove(appTag)
    }
}
