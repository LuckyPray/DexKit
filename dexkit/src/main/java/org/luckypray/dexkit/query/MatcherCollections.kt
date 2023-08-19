@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import org.luckypray.dexkit.query.base.IQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.TargetElementType
import org.luckypray.dexkit.query.matchers.BatchUsingStringsMatcher
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.UsingFieldMatcher
import org.luckypray.dexkit.query.matchers.base.NumberEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData

class StringMatcherList : ArrayList<StringMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<StringMatcher>): super(elements)

    @JvmOverloads
    fun add(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(StringMatcher(value, matchType, ignoreCase))
    }

    fun add(init: StringMatcher.() -> Unit) = also {
        add(StringMatcher().apply(init))
    }
}

class FieldMatcherList : ArrayList<FieldMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<FieldMatcher>): super(elements)

    fun add(init: FieldMatcher.() -> Unit) = also {
        add(FieldMatcher().apply(init))
    }

    @JvmOverloads
    fun addForType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        add(FieldMatcher().apply { type(typeName, matchType, ignoreCase) })
    }

    fun addForName(name: String) = also {
        add(FieldMatcher().apply { name(name) })
    }
}

class UsingFieldMatcherList : ArrayList<UsingFieldMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<UsingFieldMatcher>): super(elements)

    fun add(init: UsingFieldMatcher.() -> Unit) = also {
        add(UsingFieldMatcher().apply(init))
    }
}

class NumberEncodeValueMatcherList : ArrayList<NumberEncodeValueMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<NumberEncodeValueMatcher>): super(elements)

    fun add(init: NumberEncodeValueMatcher.() -> Unit) = also {
        add(NumberEncodeValueMatcher().apply(init))
    }
}

class BatchUsingStringsMatcherList : ArrayList<BatchUsingStringsMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<BatchUsingStringsMatcher>): super(elements)

    fun add(
        unionKey: String,
        matchers: List<StringMatcher>
    ) = also {
        add(BatchUsingStringsMatcher(unionKey, matchers))
    }

    fun add(
        unionKey: String,
        init: StringMatcherList.() -> Unit
    ) = also {
        add(BatchUsingStringsMatcher(unionKey, StringMatcherList().apply(init)))
    }
}

class ClassDataList : ArrayList<ClassData>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<ClassData>): super(elements)

    fun findClass(findClass: FindClass): List<ClassData> {
        if (isEmpty()) return emptyList()
        val bridge = first().getBridge()
        findClass.searchInClass(this)
        return bridge.findClass(findClass)
    }

    fun findClass(init: FindClass.() -> Unit): List<ClassData> {
        return findClass(FindClass().apply(init))
    }

    fun findMethod(findMethod: FindMethod): List<MethodData> {
        if (isEmpty()) return emptyList()
        val bridge = first().getBridge()
        findMethod.searchInClass(this)
        return bridge.findMethod(findMethod)
    }

    fun findMethod(init: FindMethod.() -> Unit): List<MethodData> {
        return findMethod(FindMethod().apply(init))
    }

    fun findField(findField: FindField): List<FieldData> {
        if (isEmpty()) return emptyList()
        val bridge = first().getBridge()
        findField.searchInClass(this)
        return bridge.findField(findField)
    }

    fun findField(init: FindField.() -> Unit): List<FieldData> {
        return findField(FindField().apply(init))
    }
}

class MethodDataList : ArrayList<MethodData>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<MethodData>): super(elements)

    fun findMethod(findMethod: FindMethod): List<MethodData> {
        if (isEmpty()) return emptyList()
        val bridge = first().getBridge()
        findMethod.searchInMethod(this)
        return bridge.findMethod(findMethod)
    }

    fun findMethod(init: FindMethod.() -> Unit): List<MethodData> {
        return findMethod(FindMethod().apply(init))
    }
}

class FieldDataList : ArrayList<FieldData>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<FieldData>): super(elements)
    fun findField(findField: FindField): List<FieldData> {
        if (isEmpty()) return emptyList()
        val bridge = first().getBridge()
        findField.searchInField(this)
        return bridge.findField(findField)
    }

    fun findField(init: FindField.() -> Unit): List<FieldData> {
        return findField(FindField().apply(init))
    }
}
