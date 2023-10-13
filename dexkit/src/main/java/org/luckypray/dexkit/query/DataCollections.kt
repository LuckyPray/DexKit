@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import org.luckypray.dexkit.query.base.IQuery
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData


abstract class BaseDataList<T> : ArrayList<T>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<T>): super(elements)

    /**
     * Returns the first element, or `null` if the list is empty.
     * ----------------
     * 返回第一个元素，如果列表为空，则返回 `null`。
     */
    fun firstOrNull(): T? {
        return if (isEmpty()) null else first()
    }

    /**
     * Returns the first element, or throws an exception if the list is empty.
     * ----------------
     * 返回第一个元素，如果列表为空，则抛出异常。
     */
    fun first(): T {
        return if (isEmpty()) error("list is empty") else get(0)
    }

    /**
     * Returns the first element, or throws the specified exception if the list is empty.
     * ----------------
     * 返回第一个元素，如果列表为空，则抛出指定的异常。
     */
    fun firstOrThrow(exceptionSupplier: () -> Throwable): T {
        return if (isEmpty()) throw exceptionSupplier() else get(0)
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
        findClass.searchInClass(this)
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