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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "NOTHING_TO_INLINE")

package org.luckypray.dexkit

import org.luckypray.dexkit.cache.CacheBridgeKeys
import org.luckypray.dexkit.cache.CacheBridgeRegistry
import org.luckypray.dexkit.cache.CacheBridgeRuntime
import org.luckypray.dexkit.cache.CacheBridgeStore
import org.luckypray.dexkit.exceptions.NoResultException
import org.luckypray.dexkit.exceptions.NonUniqueResultException
import org.luckypray.dexkit.query.BatchFindClassUsingStrings
import org.luckypray.dexkit.query.BatchFindMethodUsingStrings
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import org.luckypray.dexkit.wrap.ISerializable
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

object DexKitCacheBridge {
    data class CachePolicy(
        val cacheSuccess: Boolean = true,
        val failurePolicy: CacheFailurePolicy = CacheFailurePolicy.NONE,
    )

    enum class CacheFailurePolicy {
        NONE,
        QUERY_ONLY,
        ALL,
    }

    enum class QueryKind {
        METHOD_SINGLE,
        CLASS_SINGLE,
        FIELD_SINGLE,
        METHOD_LIST,
        CLASS_LIST,
        FIELD_LIST,
        METHOD_BATCH,
        CLASS_BATCH,
    }

    enum class ResultSource {
        CACHE,
        QUERY,
    }

    data class QuerySuccessEvent(
        val appTag: String,
        val queryKind: QueryKind,
        val requestKey: String?,
        val source: ResultSource,
        val matchCount: Int,
    )

    data class QueryFailureEvent(
        val appTag: String,
        val queryKind: QueryKind,
        val requestKey: String?,
        val source: ResultSource,
        val error: Throwable,
    )

    open class CacheBridgeListener {
        open fun onQuerySuccess(info: QuerySuccessEvent) {}
        open fun onQueryFailure(info: QueryFailureEvent) {}
        open fun onBridgeCreated(appTag: String) {}
        open fun onBridgeReleased(appTag: String) {}
        open fun onBridgeDestroyed(appTag: String) {}
    }

    private val cacheRef = AtomicReference<Cache?>(null)
    private val cache: Cache
        get() = cacheRef.get() ?: error("Wrapper must be init(cache) first")

    private val reaperScheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1) { r ->
        Thread(r, "DexKit-Reaper").apply { isDaemon = true }
    }.apply { removeOnCancelPolicy = true }
    private val cacheLock = ReentrantReadWriteLock()
    private val listeners = CopyOnWriteArraySet<CacheBridgeListener>()

    @JvmStatic
    var idleTimeoutMillis: Long = 5_000L

    @JvmStatic
    var cachePolicy: CachePolicy = CachePolicy()

    @JvmStatic
    fun init(cache: Cache) {
        check(cacheRef.compareAndSet(null, cache)) {
            "DexKitCacheBridge.init(cache) can only be called once"
        }
    }

    @JvmStatic
    fun addListener(listener: CacheBridgeListener) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: CacheBridgeListener) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun clearListeners() {
        listeners.clear()
    }

    private inline fun notifyListeners(block: CacheBridgeListener.() -> Unit) {
        listeners.forEach { listener ->
            runCatching { listener.block() }
        }
    }

    @JvmStatic
    fun create(appTag: String, path: String): RecyclableBridge {
        cache
        return CacheBridgeRegistry.obtainBridge(appTag) {
            RecyclableBridge.create(appTag, path)
        }
    }

    @JvmStatic
    fun create(appTag: String, dexArray: Array<ByteArray>): RecyclableBridge {
        cache
        return CacheBridgeRegistry.obtainBridge(appTag) {
            RecyclableBridge.create(appTag, dexArray)
        }
    }

    @JvmStatic
    fun create(appTag: String, classLoader: ClassLoader): RecyclableBridge {
        cache
        return CacheBridgeRegistry.obtainBridge(appTag) {
            RecyclableBridge.create(appTag, classLoader)
        }
    }

    @JvmStatic
    fun clearCache(appTag: String) {
        cacheLock.write {
            val prefix = "${CacheBridgeKeys.cachePrefixOf(appTag)}:"
            cache.getAllKeys().forEach {
                if (it.startsWith(prefix)) {
                    cache.remove(it)
                }
            }
        }
    }

    @JvmStatic
    fun clearAllCache() {
        cacheLock.write { cache.clearAll() }
    }

    interface Cache {
        fun getString(key: String, default: String?): String?
        fun putString(key: String, value: String)
        fun getStringList(key: String, default: List<String>?): List<String>?
        fun putStringList(key: String, value: List<String>)
        fun remove(key: String)
        fun getAllKeys(): Collection<String>
        fun clearAll()
    }

    class RecyclableBridge private constructor(
        private val appTag: String,
        private val path: String? = null,
        private val dexArray: Array<ByteArray>? = null,
        private val classLoader: ClassLoader? = null,
    ) : Closeable {

        companion object {
            @JvmSynthetic
            internal fun create(
                appTag: String,
                path: String
            ): RecyclableBridge = RecyclableBridge(appTag, path, null, null)

            @JvmSynthetic
            internal fun create(
                appTag: String,
                dexArray: Array<ByteArray>
            ): RecyclableBridge = RecyclableBridge(appTag, null, dexArray, null)

            @JvmSynthetic
            internal fun create(
                appTag: String,
                classLoader: ClassLoader
            ): RecyclableBridge = RecyclableBridge(appTag, null, null, classLoader)
        }

        private val runtime by lazy(LazyThreadSafetyMode.NONE) {
            CacheBridgeRuntime(
                appTag = appTag,
                bridgeHolder = this,
                scheduler = reaperScheduler,
                idleTimeoutMillis = { idleTimeoutMillis },
                createBridge = ::createBridge,
                notifyBridgeCreated = { notifyListeners { onBridgeCreated(appTag) } },
                notifyBridgeReleased = { notifyListeners { onBridgeReleased(appTag) } },
                notifyBridgeDestroyed = { notifyListeners { onBridgeDestroyed(appTag) } },
            )
        }

        fun isRetired(): Boolean = runtime.isDestroyed()

        private fun createBridge(): DexKitBridge {
            return when {
                path != null -> DexKitBridge.create(path)
                dexArray != null -> DexKitBridge.create(dexArray)
                classLoader != null -> DexKitBridge.create(classLoader, true)
                else -> error("init fail")
            }
        }

        private fun ensureUsable() {
            runtime.ensureUsable()
        }

        private inline fun <T> notifyQueryResult(
            queryKind: QueryKind,
            key: String?,
            source: ResultSource,
            result: Result<T>,
            countOf: (T) -> Int
        ): Result<T> {
            result.fold(
                onSuccess = { value ->
                    notifyListeners {
                        onQuerySuccess(
                            QuerySuccessEvent(
                                appTag = appTag,
                                queryKind = queryKind,
                                requestKey = key,
                                source = source,
                                matchCount = countOf(value),
                            )
                        )
                    }
                },
                onFailure = { error ->
                    notifyListeners {
                        onQueryFailure(
                            QueryFailureEvent(
                                appTag = appTag,
                                queryKind = queryKind,
                                requestKey = key,
                                source = source,
                                error = error,
                            )
                        )
                    }
                }
            )
            return result
        }

        private inline fun <R> acquireBridge(block: (DexKitBridge) -> R): R =
            runtime.acquireBridge(block)

        override fun close() {
            runtime.close()
        }

        fun destroy() {
            runtime.destroy()
        }

        fun interface BridgeFunction {
            fun apply(bridge: DexKitBridge)
        }

        fun withBridge(action: BridgeFunction) {
            acquireBridge { b -> action.apply(b) }
        }

        @JvmSynthetic
        fun withBridge(action: (DexKitBridge) -> Unit) {
            acquireBridge { b -> action(b) }
        }

        // region fun interface

        fun interface FindMethodBuilder {
            fun build(f: FindMethod)
        }

        fun interface FindClassBuilder {
            fun build(f: FindClass)
        }

        fun interface FindFieldBuilder {
            fun build(f: FindField)
        }

        fun interface BatchFindMethodUsingStringsBuilder {
            fun build(f: BatchFindMethodUsingStrings)
        }

        fun interface BatchFindClassUsingStringsBuilder {
            fun build(f: BatchFindClassUsingStrings)
        }

        fun interface BridgeMethodBuilder {
            fun build(b: DexKitBridge): MethodData
        }

        fun interface BridgeClassBuilder {
            fun build(b: DexKitBridge): ClassData
        }

        fun interface BridgeFieldBuilder {
            fun build(b: DexKitBridge): FieldData
        }

        fun interface BridgeMethodsBuilder {
            fun build(b: DexKitBridge): List<MethodData>
        }

        fun interface BridgeClassesBuilder {
            fun build(b: DexKitBridge): List<ClassData>
        }

        fun interface BridgeFieldsBuilder {
            fun build(b: DexKitBridge): List<FieldData>
        }

        // endregion

        // region java SAM auto gen key

        fun getMethod(
            query: FindMethodBuilder
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getClass(
            query: FindClassBuilder
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getField(
            query: FindFieldBuilder
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = query.toQuery()
        )!!

        fun getMethods(
            query: FindMethodBuilder
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getClasses(
            query: FindClassBuilder
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getFields(
            query: FindFieldBuilder
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = query.toQuery()
        )

        fun getBatchUsingStringsMethods(
            query: BatchFindMethodUsingStringsBuilder
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = query.toQuery()
        )

        fun getBatchUsingStringsClasses(
            query: BatchFindClassUsingStringsBuilder
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = query.toQuery()
        )

        fun getMethodOrNull(
            query: FindMethodBuilder
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getClassOrNull(
            query: FindClassBuilder
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getFieldOrNull(
            query: FindFieldBuilder
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = query.toQuery()
        )

        fun getMethodsOrEmpty(
            query: FindMethodBuilder
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
        )

        fun getClassesOrEmpty(
            query: FindClassBuilder
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
        )

        fun getFieldsOrEmpty(
            query: FindFieldBuilder
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = query.toQuery()
        )

        // endregion

        // region java SAM need key

        @JvmOverloads
        fun getMethod(
            key: String,
            query: FindMethodBuilder? = null
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getClass(
            key: String,
            query: FindClassBuilder? = null
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getField(
            key: String,
            query: FindFieldBuilder? = null
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = query?.toQuery()
        )!!

        @JvmOverloads
        fun getMethods(
            key: String,
            query: FindMethodBuilder? = null
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getClasses(
            key: String,
            query: FindClassBuilder? = null
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getFields(
            key: String,
            query: FindFieldBuilder? = null
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getBatchUsingStringsMethods(
            key: String,
            query: BatchFindMethodUsingStringsBuilder? = null
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getBatchUsingStringsClasses(
            key: String,
            query: BatchFindClassUsingStringsBuilder? = null
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getMethodOrNull(
            key: String,
            query: FindMethodBuilder? = null
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getClassOrNull(
            key: String,
            query: FindClassBuilder? = null
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        @JvmOverloads
        fun getFieldOrNull(
            key: String,
            query: FindFieldBuilder? = null
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = query?.toQuery()
        )

        fun getMethodsOrEmpty(
            key: String,
            query: FindMethodBuilder? = null
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getClassesOrEmpty(
            key: String,
            query: FindClassBuilder? = null
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        fun getFieldsOrEmpty(
            key: String,
            query: FindFieldBuilder? = null
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = query?.toQuery()
        )

        // endregion

        // region SAM direct

        @JvmOverloads
        fun getMethodDirect(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod = innerGetMethodDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getClassDirect(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass = innerGetClassDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getFieldDirect(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField = innerGetFieldDirect(
            key = key,
            allowNull = false,
            query = query?.toBridgeQuery()
        )!!

        @JvmOverloads
        fun getMethodsDirect(
            key: String,
            query: BridgeMethodsBuilder? = null
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassesDirect(
            key: String,
            query: BridgeClassesBuilder? = null
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldsDirect(
            key: String,
            query: BridgeFieldsBuilder? = null
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = false,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getMethodDirectOrNull(
            key: String,
            query: BridgeMethodBuilder? = null
        ): DexMethod? = innerGetMethodDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassDirectOrNull(
            key: String,
            query: BridgeClassBuilder? = null
        ): DexClass? = innerGetClassDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldDirectOrNull(
            key: String,
            query: BridgeFieldBuilder? = null
        ): DexField? = innerGetFieldDirect(
            key = key,
            allowNull = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getMethodsDirectOrEmpty(
            key: String,
            query: BridgeMethodsBuilder? = null
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getClassesDirectOrEmpty(
            key: String,
            query: BridgeClassesBuilder? = null
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        @JvmOverloads
        fun getFieldsDirectOrEmpty(
            key: String,
            query: BridgeFieldsBuilder? = null
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = true,
            query = query?.toBridgeQuery()
        )

        // endregion

        // region java overloads auto gen key

        fun getMethod(
            finder: FindMethod
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getClass(
            finder: FindClass
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getField(
            finder: FindField
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = finder
        )!!

        fun getMethods(
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getClasses(
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getFields(
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = finder
        )

        fun getBatchUsingStringsMethods(
            finder: BatchFindMethodUsingStrings
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = finder
        )

        fun getBatchUsingStringsClasses(
            finder: BatchFindClassUsingStrings
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = finder
        )

        fun getMethodOrNull(
            finder: FindMethod
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = finder
        )

        fun getClassOrNull(
            finder: FindClass
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = finder,
        )

        fun getFieldOrNull(
            finder: FindField
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = finder,
        )

        fun getMethodsOrEmpty(
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = finder
        )

        fun getClassesOrEmpty(
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = finder
        )

        fun getFieldsOrEmpty(
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = finder
        )

        // endregion

        // region java overloads need key

        fun getMethod(
            key: String,
            finder: FindMethod
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getClass(
            key: String,
            finder: FindClass
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getField(
            key: String,
            finder: FindField
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = finder
        )!!

        fun getMethods(
            key: String,
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getClasses(
            key: String,
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getFields(
            key: String,
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = finder
        )

        fun getBatchUsingStringsMethods(
            key: String,
            finder: BatchFindMethodUsingStrings
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = finder
        )

        fun getBatchUsingStringsClasses(
            key: String,
            finder: BatchFindClassUsingStrings
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = finder
        )

        fun getMethodOrNull(
            key: String,
            finder: FindMethod
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getClassOrNull(
            key: String,
            finder: FindClass
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getFieldOrNull(
            key: String,
            finder: FindField
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = finder
        )

        fun getMethodsOrEmpty(
            key: String,
            finder: FindMethod
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = finder
        )

        fun getClassesOrEmpty(
            key: String,
            finder: FindClass
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = finder
        )

        fun getFieldsOrEmpty(
            key: String,
            finder: FindField
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = finder
        )

        // endregion

        // region DSL auto gen key

        @JvmSynthetic
        fun getMethod(
            query: FindMethod.() -> Unit
        ): DexMethod = innerGetMethod(
            key = null,
            allowNull = false,
            query = FindMethod().apply(query)
        )!!

        @JvmSynthetic
        fun getClass(
            query: FindClass.() -> Unit
        ): DexClass = innerGetClass(
            key = null,
            allowNull = false,
            query = FindClass().apply(query)
        )!!

        @JvmSynthetic
        fun getField(
            query: FindField.() -> Unit
        ): DexField = innerGetField(
            key = null,
            allowNull = false,
            query = FindField().apply(query)
        )!!

        @JvmSynthetic
        fun getMethods(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = false,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClasses(
            query: FindClass.() -> Unit
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = false,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFields(
            query: FindField.() -> Unit
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = false,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsMethods(
            query: BatchFindMethodUsingStrings.() -> Unit
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = null,
            query = BatchFindMethodUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsClasses(
            query: BatchFindClassUsingStrings.() -> Unit
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = null,
            query = BatchFindClassUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getMethodOrNull(
            query: FindMethod.() -> Unit
        ): DexMethod? = innerGetMethod(
            key = null,
            allowNull = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassOrNull(
            query: FindClass.() -> Unit
        ): DexClass? = innerGetClass(
            key = null,
            allowNull = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldOrNull(
            query: FindField.() -> Unit
        ): DexField? = innerGetField(
            key = null,
            allowNull = true,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getMethodsOrEmpty(
            query: FindMethod.() -> Unit
        ): List<DexMethod> = innerGetMethods(
            key = null,
            allowEmpty = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassesOrEmpty(
            query: FindClass.() -> Unit
        ): List<DexClass> = innerGetClasses(
            key = null,
            allowEmpty = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldsOrEmpty(
            query: FindField.() -> Unit
        ): List<DexField> = innerGetFields(
            key = null,
            allowEmpty = true,
            query = FindField().apply(query)
        )

        // endregion

        // region DSL need key

        @JvmSynthetic
        fun getMethod(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod = innerGetMethod(
            key = key,
            allowNull = false,
            query = FindMethod().apply(query)
        )!!

        @JvmSynthetic
        fun getClass(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass = innerGetClass(
            key = key,
            allowNull = false,
            query = FindClass().apply(query)
        )!!

        @JvmSynthetic
        fun getField(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField = innerGetField(
            key = key,
            allowNull = false,
            query = FindField().apply(query)
        )!!

        @JvmSynthetic
        fun getMethods(
            key: String,
            query: (FindMethod.() -> Unit)
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = false,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClasses(
            key: String,
            query: (FindClass.() -> Unit)
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = false,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFields(
            key: String,
            query: (FindField.() -> Unit)
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = false,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsMethods(
            key: String,
            query: BatchFindMethodUsingStrings.() -> Unit
        ): Map<String, List<DexMethod>> = innerGetBatchUsingStringsMethods(
            key = key,
            query = BatchFindMethodUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getBatchUsingStringsClasses(
            key: String,
            query: BatchFindClassUsingStrings.() -> Unit
        ): Map<String, List<DexClass>> = innerGetBatchUsingStringsClasses(
            key = key,
            query = BatchFindClassUsingStrings().apply(query)
        )

        @JvmSynthetic
        fun getMethodOrNull(
            key: String,
            query: (FindMethod.() -> Unit)
        ): DexMethod? = innerGetMethod(
            key = key,
            allowNull = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassOrNull(
            key: String,
            query: (FindClass.() -> Unit)
        ): DexClass? = innerGetClass(
            key = key,
            allowNull = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldOrNull(
            key: String,
            query: (FindField.() -> Unit)
        ): DexField? = innerGetField(
            key = key,
            allowNull = true,
            query = FindField().apply(query)
        )

        @JvmSynthetic
        fun getMethodsOrEmpty(
            key: String,
            query: (FindMethod.() -> Unit)
        ): List<DexMethod> = innerGetMethods(
            key = key,
            allowEmpty = true,
            query = FindMethod().apply(query)
        )

        @JvmSynthetic
        fun getClassesOrEmpty(
            key: String,
            query: (FindClass.() -> Unit)
        ): List<DexClass> = innerGetClasses(
            key = key,
            allowEmpty = true,
            query = FindClass().apply(query)
        )

        @JvmSynthetic
        fun getFieldsOrEmpty(
            key: String,
            query: (FindField.() -> Unit)
        ): List<DexField> = innerGetFields(
            key = key,
            allowEmpty = true,
            query = FindField().apply(query)
        )

        // endregion

        // region DSL direct

        @JvmSynthetic
        fun getMethodDirect(
            key: String,
            query: DexKitBridge.() -> MethodData
        ): DexMethod = innerGetMethodDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getClassDirect(
            key: String,
            query: DexKitBridge.() -> ClassData
        ): DexClass = innerGetClassDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getFieldDirect(
            key: String,
            query: DexKitBridge.() -> FieldData
        ): DexField = innerGetFieldDirect(
            key = key,
            allowNull = false,
            query = query
        )!!

        @JvmSynthetic
        fun getMethodsDirect(
            key: String,
            query: DexKitBridge.() -> List<MethodData>
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getClassesDirect(
            key: String,
            query: DexKitBridge.() -> List<ClassData>
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getFieldsDirect(
            key: String,
            query: DexKitBridge.() -> List<FieldData>
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = false,
            query = query
        )

        @JvmSynthetic
        fun getMethodDirectOrNull(
            key: String,
            query: DexKitBridge.() -> MethodData?
        ): DexMethod? = innerGetMethodDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getClassDirectOrNull(
            key: String,
            query: DexKitBridge.() -> ClassData?
        ): DexClass? = innerGetClassDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getFieldDirectOrNull(
            key: String,
            query: DexKitBridge.() -> FieldData?
        ): DexField? = innerGetFieldDirect(
            key = key,
            allowNull = true,
            query = query
        )

        @JvmSynthetic
        fun getMethodsDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<MethodData>
        ): List<DexMethod> = innerGetMethodsDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        @JvmSynthetic
        fun getClassesDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<ClassData>
        ): List<DexClass> = innerGetClassesDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        @JvmSynthetic
        fun getFieldsDirectOrEmpty(
            key: String,
            query: DexKitBridge.() -> List<FieldData>
        ): List<DexField> = innerGetFieldsDirect(
            key = key,
            allowEmpty = true,
            query = query
        )

        // endregion

        // region inner methods

        private fun innerGetMethod(
            key: String? = null,
            allowNull: Boolean,
            query: FindMethod? = null
        ): DexMethod? {
            val buildQuery = query?.let { { query } }

            return getInternalSingle(
                queryKind = QueryKind.METHOD_SINGLE,
                key = key,
                mode = if (allowNull) {
                    CacheBridgeStore.SingleResolveMode.NULLABLE
                } else {
                    CacheBridgeStore.SingleResolveMode.REQUIRED
                },
                buildQuery = buildQuery,
                executor = { b, q -> b.findMethod(q) },
                mapper   = { it.toDexMethod() }
            ).getOrThrow()
        }

        private fun innerGetMethods(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindMethod? = null
        ): List<DexMethod> {
            val buildQuery = query?.let { { query } }

            return getInternalList(
                queryKind = QueryKind.METHOD_LIST,
                key = key,
                allowEmpty = allowEmpty,
                buildQuery = buildQuery,
                executor = { b, q -> b.findMethod(q) },
                mapper   = { it.toDexMethod() }
            ).getOrThrow()
        }

        private fun innerGetClass(
            key: String? = null,
            allowNull: Boolean,
            query: FindClass? = null
        ): DexClass? = getInternalSingle(
            queryKind = QueryKind.CLASS_SINGLE,
            key = key,
            mode = if (allowNull) {
                CacheBridgeStore.SingleResolveMode.NULLABLE
            } else {
                CacheBridgeStore.SingleResolveMode.REQUIRED
            },
            buildQuery = query?.let { { query } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetClasses(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindClass? = null
        ): List<DexClass> = getInternalList(
            queryKind = QueryKind.CLASS_LIST,
            key = key,
            allowEmpty = allowEmpty,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindClass -> b.findClass(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetField(
            key: String? = null,
            allowNull: Boolean,
            query: FindField? = null
        ): DexField? = getInternalSingle(
            queryKind = QueryKind.FIELD_SINGLE,
            key = key,
            mode = if (allowNull) {
                CacheBridgeStore.SingleResolveMode.NULLABLE
            } else {
                CacheBridgeStore.SingleResolveMode.REQUIRED
            },
            buildQuery = query?.let { { query } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetFields(
            key: String? = null,
            allowEmpty: Boolean,
            query: FindField? = null
        ): List<DexField> = getInternalList(
            queryKind = QueryKind.FIELD_LIST,
            key = key,
            allowEmpty = allowEmpty,
            buildQuery = query?.let { { query } },
            executor = { b, q: FindField -> b.findField(q) },
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetBatchUsingStringsClasses(
            key: String?,
            query: BatchFindClassUsingStrings? = null
        ): Map<String, List<DexClass>> = getInternalMap(
            queryKind = QueryKind.CLASS_BATCH,
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindClassUsingStrings -> b.batchFindClassUsingStrings(q) },
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetBatchUsingStringsMethods(
            key: String?,
            query: BatchFindMethodUsingStrings? = null
        ): Map<String, List<DexMethod>> = getInternalMap(
            queryKind = QueryKind.METHOD_BATCH,
            key = key,
            buildQuery = query?.let { { query } },
            executor = { b, q: BatchFindMethodUsingStrings -> b.batchFindMethodUsingStrings(q) },
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetMethodDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> MethodData?)? = null
        ): DexMethod? = getDirectInternalSingle(
            queryKind = QueryKind.METHOD_SINGLE,
            key = key,
            mode = if (allowNull) {
                CacheBridgeStore.SingleResolveMode.NULLABLE
            } else {
                CacheBridgeStore.SingleResolveMode.REQUIRED
            },
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetClassDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> ClassData?)? = null
        ): DexClass? = getDirectInternalSingle(
            queryKind = QueryKind.CLASS_SINGLE,
            key = key,
            mode = if (allowNull) {
                CacheBridgeStore.SingleResolveMode.NULLABLE
            } else {
                CacheBridgeStore.SingleResolveMode.REQUIRED
            },
            executor = query,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetFieldDirect(
            key: String,
            allowNull: Boolean,
            query: (DexKitBridge.() -> FieldData?)? = null
        ): DexField? = getDirectInternalSingle(
            queryKind = QueryKind.FIELD_SINGLE,
            key = key,
            mode = if (allowNull) {
                CacheBridgeStore.SingleResolveMode.NULLABLE
            } else {
                CacheBridgeStore.SingleResolveMode.REQUIRED
            },
            executor = query,
            mapper = { it.toDexField() }
        ).getOrThrow()

        private fun innerGetMethodsDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<MethodData>)? = null
        ): List<DexMethod> = getDirectInternalList(
            queryKind = QueryKind.METHOD_LIST,
            key = key,
            allowEmpty = allowEmpty,
            executor = query,
            mapper = { it.toDexMethod() }
        ).getOrThrow()

        private fun innerGetClassesDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<ClassData>)? = null
        ): List<DexClass> = getDirectInternalList(
            queryKind = QueryKind.CLASS_LIST,
            key = key,
            executor = query,
            allowEmpty = allowEmpty,
            mapper = { it.toDexClass() }
        ).getOrThrow()

        private fun innerGetFieldsDirect(
            key: String,
            allowEmpty: Boolean,
            query: (DexKitBridge.() -> List<FieldData>)? = null
        ): List<DexField> = getDirectInternalList(
            queryKind = QueryKind.FIELD_LIST,
            key = key,
            executor = query,
            allowEmpty = allowEmpty,
            mapper = { it.toDexField() }
        ).getOrThrow()

        // endregion

        // region no public

        private fun FindMethodBuilder.toQuery(): FindMethod =
            FindMethod().apply { this@toQuery.build(this) }

        private fun FindClassBuilder.toQuery(): FindClass =
            FindClass().apply { this@toQuery.build(this) }

        private fun FindFieldBuilder.toQuery(): FindField =
            FindField().apply { this@toQuery.build(this) }

        private fun BatchFindMethodUsingStringsBuilder.toQuery(): BatchFindMethodUsingStrings =
            BatchFindMethodUsingStrings().apply { this@toQuery.build(this) }

        private fun BatchFindClassUsingStringsBuilder.toQuery(): BatchFindClassUsingStrings =
            BatchFindClassUsingStrings().apply { this@toQuery.build(this) }

        private fun BridgeMethodBuilder.toBridgeQuery(): (DexKitBridge) -> MethodData =
            this::build

        private fun BridgeClassBuilder.toBridgeQuery(): (DexKitBridge) -> ClassData =
            this::build

        private fun BridgeFieldBuilder.toBridgeQuery(): (DexKitBridge) -> FieldData =
            this::build

        private fun BridgeMethodsBuilder.toBridgeQuery(): (DexKitBridge) -> List<MethodData> =
            this::build

        private fun BridgeClassesBuilder.toBridgeQuery(): (DexKitBridge) -> List<ClassData> =
            this::build

        private fun BridgeFieldsBuilder.toBridgeQuery(): (DexKitBridge) -> List<FieldData> =
            this::build

        private inline fun <T> observeLoad(
            queryKind: QueryKind,
            key: String?,
            loadResult: CacheBridgeStore.LoadResult<T>,
            countOf: (T) -> Int
        ): Result<T> {
            return notifyQueryResult(
                queryKind = queryKind,
                key = key,
                source = loadResult.source,
                result = loadResult.result,
                countOf = countOf
            )
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalSingle(
            queryKind: QueryKind,
            key: String?,
            mode: CacheBridgeStore.SingleResolveMode,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<R?> {
            val query = buildQuery?.invoke()
            // :s: -> single
            val spKey = CacheBridgeKeys.cacheKeyOf(appTag, "s", key, query)
            val loader: (() -> CacheBridgeStore.SingleOutcome<R>)? = query?.let {
                {
                    acquireBridge {
                        val list = executor(it, query)
                        val ret = list.firstOrNull()
                            ?: return@acquireBridge CacheBridgeStore.SingleOutcome.NoResult()
                        for (i in 1 until list.size) {
                            if (ret != list[i]) {
                                return@acquireBridge CacheBridgeStore.SingleOutcome.NonUnique(
                                    NonUniqueResultException(list.size)
                                )
                            }
                        }
                        CacheBridgeStore.SingleOutcome.Value(mapper(ret))
                    }
                }
            }
            val stableQueryIdentity = key == null && query != null
            return observeLoad(
                queryKind = queryKind,
                key = key,
                loadResult = CacheBridgeStore.getCachedSingle(
                    cache = cache,
                    lock = cacheLock,
                    cachePolicy = cachePolicy,
                    cacheKey = spKey,
                    mode = mode,
                    canCacheFailure = CacheBridgeStore.shouldCacheFailure(
                        cachePolicy = cachePolicy,
                        stableQueryIdentity = stableQueryIdentity
                    ),
                    ensureUsable = ::ensureUsable,
                    loader = loader
                ),
                countOf = { value -> if (value == null) 0 else 1 }
            )
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalList(
            queryKind: QueryKind,
            key: String?,
            allowEmpty: Boolean,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> List<D>,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            val query = buildQuery?.invoke()
            // :l: -> list
            val spKey = CacheBridgeKeys.cacheKeyOf(appTag, "l", key, query)
            val loader: (() -> List<R>)? = query?.let {
                { acquireBridge { executor(it, query).map(mapper) } }
            }
            return observeLoad(
                queryKind = queryKind,
                key = key,
                loadResult = CacheBridgeStore.getCachedList(
                    cache = cache,
                    lock = cacheLock,
                    cachePolicy = cachePolicy,
                    cacheKey = spKey,
                    allowEmpty = allowEmpty,
                    ensureUsable = ::ensureUsable,
                    loader = loader
                ),
                countOf = { list -> list.size }
            )
        }

        private inline fun <Q : BaseFinder, D, R : ISerializable> getInternalMap(
            queryKind: QueryKind,
            key: String?,
            noinline buildQuery: (() -> Q)?,
            noinline executor: (DexKitBridge, Q) -> Map<String, List<D>>,
            noinline mapper: (D) -> R
        ): Result<Map<String, List<R>>> {
            val query = buildQuery?.invoke()
            // :b: -> batch find
            val spKey = CacheBridgeKeys.cacheKeyOf(appTag, "b", key, query)
            val loader: (() -> Map<String, List<R>>)? = query?.let {
                { acquireBridge { executor(it, query).mapValues { it.value.map(mapper) } } }
            }
            return observeLoad(
                queryKind = queryKind,
                key = key,
                loadResult = CacheBridgeStore.getCachedMap(
                    cache = cache,
                    lock = cacheLock,
                    cachePolicy = cachePolicy,
                    cacheKey = spKey,
                    ensureUsable = ::ensureUsable,
                    loader = loader
                ),
                countOf = { map -> map.values.sumOf { it.size } }
            )
        }

        private inline fun <D, R : ISerializable> getDirectInternalSingle(
            queryKind: QueryKind,
            key: String,
            mode: CacheBridgeStore.SingleResolveMode,
            noinline executor: (DexKitBridge.() -> D?)?,
            noinline mapper: (D) -> R
        ): Result<R?> {
            // :s: -> single
            val spKey = CacheBridgeKeys.cacheKeyOf(appTag, "s", key)
            val loader: (() -> CacheBridgeStore.SingleOutcome<R>)? = executor?.let {
                {
                    try {
                        acquireBridge {
                            val value = it.let(executor)
                                ?: return@acquireBridge CacheBridgeStore.SingleOutcome.NoResult()
                            CacheBridgeStore.SingleOutcome.Value(mapper(value))
                        }
                    } catch (e: NoResultException) {
                        CacheBridgeStore.SingleOutcome.NoResult(e)
                    } catch (e: NonUniqueResultException) {
                        CacheBridgeStore.SingleOutcome.NonUnique(e)
                    }
                }
            }
            return observeLoad(
                queryKind = queryKind,
                key = key,
                loadResult = CacheBridgeStore.getCachedSingle(
                    cache = cache,
                    lock = cacheLock,
                    cachePolicy = cachePolicy,
                    cacheKey = spKey,
                    mode = mode,
                    canCacheFailure = CacheBridgeStore.shouldCacheFailure(
                        cachePolicy = cachePolicy,
                        stableQueryIdentity = false
                    ),
                    ensureUsable = ::ensureUsable,
                    loader = loader
                ),
                countOf = { value -> if (value == null) 0 else 1 }
            )
        }

        private inline fun <D, R : ISerializable> getDirectInternalList(
            queryKind: QueryKind,
            key: String,
            allowEmpty: Boolean,
            noinline executor: (DexKitBridge.() -> List<D>)?,
            noinline mapper: (D) -> R
        ): Result<List<R>> {
            // :l: -> list
            val spKey = CacheBridgeKeys.cacheKeyOf(appTag, "l", key)
            val loader: (() -> List<R>)? = executor?.let {
                { acquireBridge { it.let(executor).map(mapper) } }
            }
            return observeLoad(
                queryKind = queryKind,
                key = key,
                loadResult = CacheBridgeStore.getCachedList(
                    cache = cache,
                    lock = cacheLock,
                    cachePolicy = cachePolicy,
                    cacheKey = spKey,
                    allowEmpty = allowEmpty,
                    ensureUsable = ::ensureUsable,
                    loader = loader
                ),
                countOf = { list -> list.size }
            )
        }

        // endregion
    }
}
