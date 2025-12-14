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

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class AdaptiveLoaderCache<K : Any, V : Any>(
    private val weakValue: Boolean = true
) {
    private val lock = ReentrantReadWriteLock()
    private sealed interface Ref<V : Any> { fun get(): V? }
    private class StrongRef<V : Any>(private val v: V) : Ref<V> { override fun get(): V = v }
    private class WeakRef<V : Any>(v: V) : Ref<V> {
        private val ref = WeakReference(v)
        override fun get(): V? = ref.get()
    }
    private fun wrap(v: V): Ref<V> = if (weakValue) WeakRef(v) else StrongRef(v)

    private var singleLoaderRef: WeakReference<ClassLoader>? = null
    private var singleMap: MutableMap<K, Ref<V>> = HashMap()

    private var multi: WeakHashMap<ClassLoader, MutableMap<K, Ref<V>>>? = null

    fun get(loader: ClassLoader, key: K, producer: () -> V): V {
        lock.readLock().withLock {
            val m = multi
            if (m == null) {
                val sl = singleLoaderRef?.get()
                if (sl === loader) {
                    val ref = singleMap[key] ?: return@withLock
                    val v = ref.get()
                    if (v != null) return v
                }
            } else {
                val map = m[loader]
                if (map != null) {
                    val ref = map[key] ?: return@withLock
                    val v = ref.get()
                    if (v != null) return v
                }
            }
        }

        lock.writeLock().withLock {
            var m = multi

            if (m == null) {
                val sl = singleLoaderRef?.get()
                when {
                    sl == null -> {
                        singleLoaderRef = WeakReference(loader)
                        singleMap.clear()
                    }
                    sl !== loader -> {
                        val newMulti = WeakHashMap<ClassLoader, MutableMap<K, Ref<V>>>()
                        newMulti[sl] = singleMap
                        newMulti[loader] = HashMap()
                        multi = newMulti
                        m = newMulti

                        singleLoaderRef = null
                        singleMap = HashMap()
                    }
                }
            }

            val targetMap = if (m != null) {
                m.getOrPut(loader) { HashMap() }
            } else {
                singleMap
            }

            targetMap[key]?.get()?.let { return it }

            val v = producer()
            targetMap[key] = wrap(v)
            return v
        }
    }

    fun clear() {
        lock.writeLock().withLock {
            multi?.clear()
            multi = null
            singleLoaderRef = null
            singleMap.clear()
        }
    }

    fun clear(loader: ClassLoader) {
        lock.writeLock().withLock {
            multi?.remove(loader)
            if (multi?.isEmpty() == true) {
                multi = null
            }
            val sl = singleLoaderRef?.get()
            if (sl === loader) {
                singleLoaderRef = null
                singleMap.clear()
            }
        }
    }
}
