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

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.DexKitCacheBridge
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class CacheBridgeRuntime(
    private val appTag: String,
    private val bridgeHolder: DexKitCacheBridge.RecyclableBridge,
    private val scheduler: ScheduledThreadPoolExecutor,
    private val idleTimeoutMillis: () -> Long,
    private val createBridge: () -> DexKitBridge,
    private val notifyBridgeCreated: () -> Unit,
    private val notifyBridgeReleased: () -> Unit,
    private val notifyBridgeDestroyed: () -> Unit,
) {
    private val destroyed = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private var activeCalls = 0
    private var generation = 0L
    private var releaseRequested = false
    private var reaperFuture: ScheduledFuture<*>? = null

    @Volatile
    private var bridge: DexKitBridge? = null

    fun isDestroyed(): Boolean = destroyed.get()

    fun ensureUsable() {
        check(!isDestroyed()) { "RecyclableBridge is destroyed" }
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
            if (isDestroyed()) {
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
            notifyBridgeReleased()
        }
    }

    inline fun <R> acquireBridge(block: (DexKitBridge) -> R): R {
        beginUse()
        return try {
            var created = false
            val current = synchronized(lifecycleLock) {
                bridge ?: createBridge().also {
                    bridge = it
                    created = true
                }
            }
            if (created) {
                notifyBridgeCreated()
            }
            block(current)
        } finally {
            endUse()
        }
    }

    private fun releaseBridgeLocked(): Boolean {
        val current = bridge ?: return false
        current.close()
        bridge = null
        return true
    }

    private fun scheduleRetireLocked() {
        val token = generation
        reaperFuture = scheduler.schedule({
            val released = synchronized(lifecycleLock) {
                if (isDestroyed()) return@schedule
                if (activeCalls != 0) return@schedule
                if (generation != token) return@schedule
                reaperFuture = null
                if (releaseRequested) return@schedule
                val removed = CacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
                if (!removed || isDestroyed()) return@schedule
                val result = releaseBridgeLocked()
                CacheBridgeRegistry.putWeak(appTag, bridgeHolder)
                result
            }
            if (released) {
                notifyBridgeReleased()
            }
        }, idleTimeoutMillis(), TimeUnit.MILLISECONDS)
    }

    private fun moveToWeakPoolLocked() {
        if (!isDestroyed()) {
            CacheBridgeRegistry.moveToWeak(appTag, bridgeHolder)
        } else {
            CacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
        }
    }

    fun close() {
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
            notifyBridgeReleased()
        }
    }

    fun destroy() {
        if (destroyed.compareAndSet(false, true)) {
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
            CacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
            CacheBridgeRegistry.removeWeak(appTag)
            if (released) {
                notifyBridgeReleased()
            }
            notifyBridgeDestroyed()
        }
    }
}
