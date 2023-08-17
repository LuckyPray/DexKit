package org.luckypray.dexkit.query.util

import org.luckypray.dexkit.DexKitDsl
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.BatchUsingStringsMatcher
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.result.BaseData

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
@JvmOverloads
fun MutableList<StringMatcher>.addMatcher(
    value: String,
    matchType: StringMatchType = StringMatchType.Equal,
    ignoreCase: Boolean = false
) = also {
    add(StringMatcher(value, matchType, ignoreCase))
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun MutableList<FieldMatcher>.addMatcher(init: FieldMatcher.() -> Unit) = also {
    add(FieldMatcher().apply(init))
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
@JvmOverloads
fun MutableList<FieldMatcher>.addForType(
    typeName: String,
    matchType: StringMatchType = StringMatchType.Equal,
    ignoreCase: Boolean = false
) = also {
    add(FieldMatcher().apply { type(typeName, matchType, ignoreCase) })
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun MutableList<FieldMatcher>.addForName(name: String) = also {
    add(FieldMatcher().apply { name(name) })
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun MutableList<BatchUsingStringsMatcher>.addMatcher(
    unionKey: String,
    matchers: List<StringMatcher>
) = also {
    add(BatchUsingStringsMatcher(unionKey, matchers))
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun MutableList<BatchUsingStringsMatcher>.addMatcher(
    unionKey: String,
    init: (@DexKitDsl MutableList<StringMatcher>).() -> Unit
) = also {
    add(BatchUsingStringsMatcher(unionKey, mutableListOf<StringMatcher>().apply(init)))
}

// region DexKitBridge chain call

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findClass(findClass: FindClass): List<ClassData> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptyList()
    val dataList = mutableListOf<BaseData>().apply {
        iterator.forEach { add(it) }
    }
    when (dataList.first()) {
        is ClassData -> findClass.searchInClass(dataList as List<ClassData>)
        else -> throw IllegalAccessError("findClass() only support Iterable<ClassData>")
    }
    return dataList.first().getBridge().findClass(findClass)
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findClass(init: FindClass.() -> Unit): List<ClassData> {
    return findClass(FindClass().apply(init))
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findMethod(findMethod: FindMethod): List<MethodData> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptyList()
    val dataList = mutableListOf<BaseData>().apply {
        iterator.forEach { add(it) }
    }
    when (dataList.first()) {
        is ClassData -> findMethod.searchInClass(dataList as List<ClassData>)
        is MethodData -> findMethod.searchInMethod(dataList as List<MethodData>)
        else -> throw IllegalAccessError("findMethod() not support Iterable<FieldData>")
    }
    return dataList.first().getBridge().findMethod(findMethod)
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findMethod(init: FindMethod.() -> Unit): List<MethodData> {
    return findMethod(FindMethod().apply(init))
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findField(findField: FindField): List<FieldData> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptyList()
    val dataList = mutableListOf<BaseData>().apply {
        iterator.forEach { add(it) }
    }
    when (dataList.first()) {
        is ClassData -> findField.searchInClass(dataList as List<ClassData>)
        is FieldData -> findField.searchInField(dataList as List<FieldData>)
        else -> throw IllegalAccessError("findField() not support Iterable<MethodData>")
    }
    return dataList.first().getBridge().findField(findField)
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
fun Iterable<BaseData>.findField(init: FindField.() -> Unit): List<FieldData> {
    return findField(FindField().apply(init))
}

// endregion