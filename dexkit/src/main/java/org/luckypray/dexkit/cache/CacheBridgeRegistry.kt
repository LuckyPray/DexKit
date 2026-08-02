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
import java.util.HashMap

@OptIn(DexKitExperimentalApi::class)
internal object CacheBridgeRegistry {
    private val registryLock = Any()
    private val strongPool = HashMap<String, DexKitCacheBridge.RecyclableBridge>()
    private val weakPool = HashMap<String, KeyedWeakReference>()
    private val refQueue = ReferenceQueue<DexKitCacheBridge.RecyclableBridge>()

    private class KeyedWeakReference(
        val key: String,
        referent: DexKitCacheBridge.RecyclableBridge,
        q: ReferenceQueue<DexKitCacheBridge.RecyclableBridge>
    ) : WeakReference<DexKitCacheBridge.RecyclableBridge>(referent, q)

    private fun removeClearedWeakRefsLocked() {
        while (true) {
            val ref = refQueue.poll() ?: break
            val keyed = ref as? KeyedWeakReference ?: continue
            if (weakPool[keyed.key] === keyed) {
                weakPool.remove(keyed.key)
            }
        }
    }

    fun removeClearedWeakRefs() {
        synchronized(registryLock) {
            removeClearedWeakRefsLocked()
        }
    }

    fun obtainBridge(
        appTag: String,
        factory: () -> DexKitCacheBridge.RecyclableBridge
    ): DexKitCacheBridge.RecyclableBridge {
        return synchronized(registryLock) {
            removeClearedWeakRefsLocked()

            strongPool[appTag]?.let { bridge ->
                if (!bridge.isRetired()) return@synchronized bridge
                strongPool.remove(appTag)
            }

            weakPool[appTag]?.let { ref ->
                val bridge = ref.get()
                if (bridge != null && !bridge.isRetired()) {
                    return@synchronized bridge
                }
                weakPool.remove(appTag)
            }

            val newBridge = factory()
            weakPool[appTag] = KeyedWeakReference(appTag, newBridge, refQueue)
            newBridge
        }
    }

    fun promote(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge) {
        synchronized(registryLock) {
            removeClearedWeakRefsLocked()

            strongPool[appTag]?.let { current ->
                if (current !== bridge && !current.isRetired()) {
                    error("Another RecyclableBridge is active for appTag: $appTag")
                }
                if (current !== bridge) {
                    strongPool.remove(appTag)
                }
            }

            weakPool[appTag]?.let { ref ->
                val current = ref.get()
                if (current != null && current !== bridge && !current.isRetired()) {
                    error("Another RecyclableBridge is registered for appTag: $appTag")
                }
                weakPool.remove(appTag)
            }
            strongPool[appTag] = bridge
        }
    }

    fun demote(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge) {
        synchronized(registryLock) {
            removeClearedWeakRefsLocked()

            if (strongPool[appTag] === bridge) {
                strongPool.remove(appTag)
            }
            if (bridge.isRetired()) {
                unregisterWeakLocked(appTag, bridge)
                return
            }

            weakPool[appTag]?.let { ref ->
                val current = ref.get()
                if (current != null && current !== bridge && !current.isRetired()) {
                    error("Another RecyclableBridge is registered for appTag: $appTag")
                }
            }
            weakPool[appTag] = KeyedWeakReference(appTag, bridge, refQueue)
        }
    }

    fun unregister(appTag: String, bridge: DexKitCacheBridge.RecyclableBridge) {
        synchronized(registryLock) {
            if (strongPool[appTag] === bridge) {
                strongPool.remove(appTag)
            }
            unregisterWeakLocked(appTag, bridge)
        }
    }

    private fun unregisterWeakLocked(
        appTag: String,
        bridge: DexKitCacheBridge.RecyclableBridge,
    ) {
        val ref = weakPool[appTag] ?: return
        val current = ref.get()
        if (current == null || current === bridge) {
            weakPool.remove(appTag)
        }
    }
}
