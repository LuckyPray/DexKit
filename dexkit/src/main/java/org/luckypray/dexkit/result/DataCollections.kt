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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.exceptions.NoResultException
import org.luckypray.dexkit.exceptions.NonUniqueResultException
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.base.IQuery


abstract class BaseDataList<T> : ArrayList<T>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<T>): super(elements)

    /**
     * Returns the first element, or `null` if the list is empty.
     * None-unique results may cause unexpected problems and are not recommended.
     * ----------------
     * 返回第一个元素，如果列表为空，则返回 `null`。
     * 非唯一结果可能会导致意外的问题，不推荐使用。
     */
    fun firstOrNull(): T? {
        return if (isEmpty()) null else first()
    }

    /**
     * Returns the first element matching the given [predicate], or `null` if no such element was found.
     * None-unique results may cause unexpected problems and are not recommended.
     * ----------------
     * 返回与给定 [predicate] 匹配的第一个元素，如果未找到此类元素，则返回 `null`。
     * 非唯一结果可能会导致意外的问题，不推荐使用。
     */
    fun firstOrNull(predicate: (T) -> Boolean): T? {
        for (element in this) if (predicate(element)) return element
        return null
    }

    /**
     * Returns the first element, or throws an exception if the list is empty.
     * None-unique results may cause unexpected problems and are not recommended.
     * ----------------
     * 返回第一个元素，如果列表为空，则抛出异常。
     * 非唯一结果可能会导致意外的问题，不推荐使用。
     */
    fun first(): T {
        return if (isEmpty()) error("list is empty") else get(0)
    }

    /**
     * Returns the first element matching the given [predicate], or throws an exception if no such element was found.
     * None-unique results may cause unexpected problems and are not recommended.
     * ----------------
     * 返回与给定 [predicate] 匹配的第一个元素，如果未找到此类元素，则抛出异常。
     * 非唯一结果可能会导致意外的问题，不推荐使用。
     */
    fun first(predicate: (T) -> Boolean): T {
        for (element in this) if (predicate(element)) return element
        error("No element matching predicate was found.")
    }

    /**
     * Returns the first element, or throws the specified exception if the list is empty.
     * None-unique results may cause unexpected problems and are not recommended.
     * ----------------
     * 返回第一个元素，如果列表为空，则抛出指定的异常。
     * 非唯一结果可能会导致意外的问题，不推荐使用。
     */
    fun firstOrThrow(exceptionSupplier: () -> Throwable): T {
        return if (isEmpty()) throw exceptionSupplier() else get(0)
    }

    /**
     * Returns the first element, or `null` if the list length is not 1.
     * ----------------
     * 返回第一个元素，如果列表长度不为 1，则返回 `null`。
     */
    fun singleOrNull(): T? {
        if (size == 0) return null
        val t = get(0)
        for (i in 1 until size) {
            if (t != get(i)) {
                return null
            }
        }
        return t
    }

    /**
     * Returns the first element matching the given [predicate], or `null` if no such element was found.
     * ----------------
     * 返回与给定 [predicate] 匹配的第一个元素，如果未找到此类元素，则返回 `null`。
     */
    fun singleOrNull(predicate: (T) -> Boolean): T? {
        if (size == 0) return null
        var t: T? = null
        for (element in this) {
            if (predicate(element)) {
                if (t != null && t != element) {
                    return null
                }
                t = element
            }
        }
        return t
    }

    /**
     * Returns the first element, or throws an exception if the list length is not 1.
     * ----------------
     * 返回第一个元素，如果列表长度不为 1，则抛出异常。
     */
    fun single(): T {
        if (size == 0) throw NoResultException("No result found for query")
        val t = get(0)
        for (i in 1 until size) {
            if (t != get(i)) {
                throw NonUniqueResultException(size)
            }
        }
        return t
    }

    /**
     * Returns the first element matching the given [predicate], or throws an exception if no such element was found.
     * ----------------
     * 返回与给定 [predicate] 匹配的第一个元素，如果未找到此类元素，则抛出异常。
     */
    fun single(predicate: (T) -> Boolean): T {
        if (size == 0) throw NoResultException("No result found for query")
        var t: T? = null
        for (element in this) {
            if (predicate(element)) {
                if (t != null && t != element) {
                    throw NonUniqueResultException(size)
                }
                t = element
            }
        }
        return t ?: throw NoResultException("No result found for query")
    }

    /**
     * Returns the first element, or throws the specified exception if the list length is not 1.
     * ----------------
     * 返回第一个元素，如果列表长度不为 1，则抛出指定的异常。
     */
    fun singleOrThrow(exceptionSupplier: () -> Throwable): T {
        return singleOrNull() ?: throw exceptionSupplier()
    }

    /**
     * Returns the first element, or throws an exception if the list length is not 1.
     * ----------------
     * 返回第一个元素，如果列表长度不为 1，则抛出异常。
     */
    @Deprecated("Use single instead", ReplaceWith("single()"))
    fun fetchOne(): T {
        if (isEmpty()) error("list is empty")
        if (size > 1) error("list has more than one element")
        return get(0)
    }
}

class ClassDataList : BaseDataList<ClassData> {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<ClassData>): super(elements)

    /**
     * Search in this list with multiple conditions.
     * ----------------
     * 在本列表中进行多条件类搜索。
     *
     * @param [findClass] query object / 查询对象
     * @return [ClassDataList]
     */
    fun findClass(findClass: FindClass): ClassDataList {
        if (isEmpty()) return ClassDataList()
        val bridge = first().getBridge()
        findClass.searchIn(this)
        return bridge.findClass(findClass)
    }

    /**
     * @see findClass
     */
    @kotlin.internal.InlineOnly
    inline fun findClass(init: FindClass.() -> Unit): ClassDataList {
        return findClass(FindClass().apply(init))
    }

    /**
     * Search in this list with multiple conditions.
     * ----------------
     * 在本列表中进行多条件方法搜索。
     *
     * @param [findMethod] query object / 查询对象
     * @return [MethodDataList]
     */
    fun findMethod(findMethod: FindMethod): MethodDataList {
        if (isEmpty()) return MethodDataList()
        val bridge = first().getBridge()
        findMethod.searchInClass(this)
        return bridge.findMethod(findMethod)
    }

    /**
     * @see findMethod
     */
    @kotlin.internal.InlineOnly
    inline fun findMethod(init: FindMethod.() -> Unit): MethodDataList {
        return findMethod(FindMethod().apply(init))
    }

    /**
     * Search in this list with multiple conditions.
     * ----------------
     * 在本列表中进行多条件字段搜索。
     *
     * @param [findField] query object / 查询对象
     * @return [FieldDataList]
     */
    fun findField(findField: FindField): FieldDataList {
        if (isEmpty()) return FieldDataList()
        val bridge = first().getBridge()
        findField.searchInClass(this)
        return bridge.findField(findField)
    }

    /**
     * @see findField
     */
    @kotlin.internal.InlineOnly
    inline fun findField(init: FindField.() -> Unit): FieldDataList {
        return findField(FindField().apply(init))
    }
}

class MethodDataList : BaseDataList<MethodData> {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<MethodData>): super(elements)

    /**
     * Search in this list with multiple conditions.
     * ----------------
     * 在本列表中进行多条件方法搜索。
     *
     * @param [findMethod] query object / 查询对象
     * @return [MethodDataList]
     */
    fun findMethod(findMethod: FindMethod): MethodDataList {
        if (isEmpty()) return MethodDataList()
        val bridge = first().getBridge()
        findMethod.searchInMethod(this)
        return bridge.findMethod(findMethod)
    }

    /**
     * @see findMethod
     */
    @kotlin.internal.InlineOnly
    inline fun findMethod(init: FindMethod.() -> Unit): MethodDataList {
        return findMethod(FindMethod().apply(init))
    }
}

class FieldDataList : BaseDataList<FieldData> {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<FieldData>): super(elements)

    /**
     * Search in this list with multiple conditions.
     * ----------------
     * 在本列表中进行多条件字段搜索。
     *
     * @param [findField] query object / 查询对象
     * @return [FieldDataList]
     */
    fun findField(findField: FindField): FieldDataList {
        if (isEmpty()) return FieldDataList()
        val bridge = first().getBridge()
        findField.searchInField(this)
        return bridge.findField(findField)
    }

    /**
     * @see findField
     */
    @kotlin.internal.InlineOnly
    inline fun findField(init: FindField.() -> Unit): FieldDataList {
        return findField(FindField().apply(init))
    }
}