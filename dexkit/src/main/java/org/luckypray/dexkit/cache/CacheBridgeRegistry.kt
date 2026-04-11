/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
package org.luckypray.dexkit.cache

import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@OptIn(DexKitExperimentalApi::class)
internal object CacheBridgeRegistry {
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
