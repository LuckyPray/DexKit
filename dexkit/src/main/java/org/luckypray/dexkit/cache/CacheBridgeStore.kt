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
import org.luckypray.dexkit.exceptions.NoResultException
import org.luckypray.dexkit.exceptions.NonUniqueResultException
import org.luckypray.dexkit.wrap.ISerializable
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@OptIn(DexKitExperimentalApi::class)
internal object CacheBridgeStore {
    private const val CACHE_NO_RESULT = "CACHE_NO_RESULT"
    private const val CACHE_NON_UNIQUE = "CACHE_NON_UNIQUE"

    enum class SingleResolveMode {
        REQUIRED,
        NULLABLE,
    }

    sealed interface SingleOutcome<out T : ISerializable> {
        data class Value<T : ISerializable>(val value: T) : SingleOutcome<T>
        data class NoResult(
            val exception: NoResultException = NoResultException("No result found for query")
        ) : SingleOutcome<Nothing>

        data class NonUnique(
            val exception: NonUniqueResultException = NonUniqueResultException(
                "query did not return a unique result"
            )
        ) : SingleOutcome<Nothing>
    }

    data class LoadResult<T>(
        val source: DexKitCacheBridge.ResultSource,
        val result: Result<T>,
    )

    fun shouldCacheFailure(
        cachePolicy: DexKitCacheBridge.CachePolicy,
        stableQueryIdentity: Boolean
    ): Boolean {
        return when (cachePolicy.failurePolicy) {
            DexKitCacheBridge.CacheFailurePolicy.NONE -> false
            DexKitCacheBridge.CacheFailurePolicy.QUERY_ONLY -> stableQueryIdentity
            DexKitCacheBridge.CacheFailurePolicy.ALL -> true
        }
    }

    private fun <T : ISerializable> resolveSingleOutcome(
        outcome: SingleOutcome<T>,
        mode: SingleResolveMode
    ): Result<T?> {
        return when (outcome) {
            is SingleOutcome.Value -> Result.success(outcome.value)
            is SingleOutcome.NoResult -> when (mode) {
                SingleResolveMode.NULLABLE -> Result.success(null)
                SingleResolveMode.REQUIRED -> Result.failure(outcome.exception)
            }

            is SingleOutcome.NonUnique -> when (mode) {
                SingleResolveMode.NULLABLE -> Result.success(null)
                SingleResolveMode.REQUIRED -> Result.failure(outcome.exception)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ISerializable> parseSingleOutcome(raw: String): SingleOutcome<T> {
        return when (raw) {
            CACHE_NO_RESULT -> SingleOutcome.NoResult()
            CACHE_NON_UNIQUE -> SingleOutcome.NonUnique()
            else -> SingleOutcome.Value(ISerializable.deserializeAs(raw) as T)
        }
    }

    fun <T : ISerializable> getCachedSingle(
        cache: DexKitCacheBridge.Cache,
        lock: ReentrantReadWriteLock,
        cachePolicy: DexKitCacheBridge.CachePolicy,
        cacheKey: String,
        mode: SingleResolveMode,
        canCacheFailure: Boolean,
        ensureUsable: () -> Unit,
        loader: (() -> SingleOutcome<T>)? = null,
    ): LoadResult<T?> {
        ensureUsable()

        fun <U : ISerializable> innerGet(cacheKey: String): SingleOutcome<U>? {
            cache.getString(cacheKey, null)?.let { raw ->
                return parseSingleOutcome(raw)
            }
            return null
        }

        lock.read {
            innerGet<T>(cacheKey)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = resolveSingleOutcome(it, mode)
                )
            }
        }

        loader ?: return LoadResult(
            source = DexKitCacheBridge.ResultSource.CACHE,
            result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey"))
        )

        val loaded = runCatching { loader() }

        return lock.write {
            innerGet<T>(cacheKey)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = resolveSingleOutcome(it, mode)
                )
            }
            LoadResult(
                source = DexKitCacheBridge.ResultSource.QUERY,
                result = loaded.fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is SingleOutcome.Value -> {
                                if (cachePolicy.cacheSuccess) {
                                    cache.putString(cacheKey, outcome.value.serialize())
                                }
                            }

                            is SingleOutcome.NoResult -> {
                                if (canCacheFailure) {
                                    cache.putString(cacheKey, CACHE_NO_RESULT)
                                }
                            }

                            is SingleOutcome.NonUnique -> {
                                if (canCacheFailure) {
                                    cache.putString(cacheKey, CACHE_NON_UNIQUE)
                                }
                            }
                        }
                        resolveSingleOutcome(outcome, mode)
                    },
                    onFailure = { Result.failure(it) }
                )
            )
        }
    }

    fun <T : ISerializable> getCachedList(
        cache: DexKitCacheBridge.Cache,
        lock: ReentrantReadWriteLock,
        cachePolicy: DexKitCacheBridge.CachePolicy,
        cacheKey: String,
        allowEmpty: Boolean,
        ensureUsable: () -> Unit,
        loader: (() -> List<T>)? = null,
    ): LoadResult<List<T>> {
        ensureUsable()

        fun <U : ISerializable> innerGet(cacheKey: String, allowEmpty: Boolean): Result<List<U>>? {
            cache.getStringList(cacheKey, null)?.let { rawList ->
                val list = rawList.map { ISerializable.deserializeAs<U>(it) }
                if (list.isEmpty() && !allowEmpty) {
                    return Result.failure(
                        IllegalStateException(
                            "cached empty for key: $cacheKey but empty not allowed"
                        )
                    )
                }
                return Result.success(list)
            }
            return null
        }

        lock.read {
            innerGet<T>(cacheKey, allowEmpty)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = it
                )
            }
        }

        loader ?: return LoadResult(
            source = DexKitCacheBridge.ResultSource.CACHE,
            result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey"))
        )

        val loaded = runCatching { loader() }

        return lock.write {
            innerGet<T>(cacheKey, allowEmpty)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = it
                )
            }
            LoadResult(
                source = DexKitCacheBridge.ResultSource.QUERY,
                result = loaded.fold(
                    onSuccess = { list ->
                        if (list.isEmpty() && !allowEmpty) {
                            Result.failure(
                                IllegalStateException(
                                    "query returned empty for key: $cacheKey but empty not allowed"
                                )
                            )
                        } else {
                            if (cachePolicy.cacheSuccess) {
                                cache.putStringList(cacheKey, list.map(ISerializable::serialize))
                            }
                            Result.success(list)
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            )
        }
    }

    fun <T : ISerializable> getCachedMap(
        cache: DexKitCacheBridge.Cache,
        lock: ReentrantReadWriteLock,
        cachePolicy: DexKitCacheBridge.CachePolicy,
        cacheKey: String,
        ensureUsable: () -> Unit,
        loader: (() -> Map<String, List<T>>)? = null,
    ): LoadResult<Map<String, List<T>>> {
        ensureUsable()

        fun <U : ISerializable> innerGetMap(cacheKey: String): Map<String, List<U>>? {
            val keys = cache.getStringList(CacheBridgeKeys.mapGroupsKey(cacheKey), null)
                ?: return null

            val uniqueKeys = LinkedHashSet<String>(keys.size)
            val map = LinkedHashMap<String, List<U>>(keys.size)
            keys.forEach { groupKey ->
                if (!uniqueKeys.add(groupKey)) {
                    return null
                }
                val rawList = cache.getStringList(
                    CacheBridgeKeys.mapGroupKey(cacheKey, groupKey),
                    null
                ) ?: return null
                val dataList = runCatching {
                    rawList.map { ISerializable.deserializeAs<U>(it) }
                }.getOrElse {
                    return null
                }
                map[groupKey] = dataList
            }
            return map
        }

        lock.read {
            innerGetMap<T>(cacheKey)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = Result.success(it)
                )
            }
        }

        loader ?: return LoadResult(
            source = DexKitCacheBridge.ResultSource.CACHE,
            result = Result.failure(NoSuchElementException("no found cache for key: $cacheKey"))
        )

        val loaded = runCatching { loader() }

        return lock.write {
            innerGetMap<T>(cacheKey)?.let {
                return LoadResult(
                    source = DexKitCacheBridge.ResultSource.CACHE,
                    result = Result.success(it)
                )
            }
            LoadResult(
                source = DexKitCacheBridge.ResultSource.QUERY,
                result = loaded.fold(
                    onSuccess = { map ->
                        val oldKeys = cache.getStringList(
                            CacheBridgeKeys.mapGroupsKey(cacheKey),
                            null
                        ) ?: emptyList()
                        val keys = mutableListOf<String>()
                        if (cachePolicy.cacheSuccess) {
                            map.entries.forEach { (groupKey, value) ->
                                keys.add(groupKey)
                                cache.putStringList(
                                    CacheBridgeKeys.mapGroupKey(cacheKey, groupKey),
                                    value.map { it.serialize() }
                                )
                            }
                            (oldKeys - keys.toSet()).forEach {
                                cache.remove(CacheBridgeKeys.mapGroupKey(cacheKey, it))
                            }
                            cache.putStringList(CacheBridgeKeys.mapGroupsKey(cacheKey), keys)
                        }
                        Result.success(map)
                    },
                    onFailure = { Result.failure(it) }
                )
            )
        }
    }
}
