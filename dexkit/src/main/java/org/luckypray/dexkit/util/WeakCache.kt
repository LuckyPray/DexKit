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

package org.luckypray.dexkit.util

import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Only applicable to singleton objects as keys, otherwise concurrent situations are not thread-safe
 */
internal class WeakCache<K : Any, V> : Iterable<Map.Entry<K, V>> {

    private val cache: MutableMap<K, V> = WeakHashMap()
    private val lock = ReentrantReadWriteLock()

    fun get(key: K): V? {
        lock.readLock().withLock {
            return cache[key]
        }
    }

    fun get(key: K, block: () -> V): V {
        return get(key) ?: run {
            val value: V
            synchronized(key) {
                value = get(key) ?: put(key, block())
            }
            return value
        }
    }

    fun put(key: K, value: V): V {
        lock.writeLock().withLock {
            cache[key] = value
        }
        return value
    }

    fun remove(key: K) {
        lock.writeLock().withLock {
            cache.remove(key)
        }
    }

    fun clear() {
        lock.writeLock().withLock {
            cache.clear()
        }
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return cache.iterator()
    }
}