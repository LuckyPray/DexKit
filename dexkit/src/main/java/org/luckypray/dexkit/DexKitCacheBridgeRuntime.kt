package org.luckypray.dexkit

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class DexKitCacheBridgeRuntime(
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
                val removed = DexKitCacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
                if (!removed || isDestroyed()) return@schedule
                val result = releaseBridgeLocked()
                DexKitCacheBridgeRegistry.putWeak(appTag, bridgeHolder)
                result
            }
            if (released) {
                notifyBridgeReleased()
            }
        }, idleTimeoutMillis(), TimeUnit.MILLISECONDS)
    }

    private fun moveToWeakPoolLocked() {
        if (!isDestroyed()) {
            DexKitCacheBridgeRegistry.moveToWeak(appTag, bridgeHolder)
        } else {
            DexKitCacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
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
            DexKitCacheBridgeRegistry.removeStrong(appTag, bridgeHolder)
            DexKitCacheBridgeRegistry.removeWeak(appTag)
            if (released) {
                notifyBridgeReleased()
            }
            notifyBridgeDestroyed()
        }
    }
}
