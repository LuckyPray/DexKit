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

package org.luckypray.dexkit.query

import org.luckypray.dexkit.query.base.IQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.StringMatchersGroup
import org.luckypray.dexkit.query.matchers.UsingFieldMatcher
import org.luckypray.dexkit.query.matchers.base.NumberEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class StringMatcherList : ArrayList<StringMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<StringMatcher>): super(elements)

    /**
     * Add using string.
     * ----------------
     * 添加使用字符串。
     *
     *     addUsingString("string", StringMatchType.Equals, false)
     *
     * @param usingString using string / 使用字符串
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatcherList]
     */
    @JvmOverloads
    fun add(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(StringMatcher(usingString, matchType, ignoreCase))
    }
}

class FieldMatcherList : ArrayList<FieldMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<FieldMatcher>): super(elements)

    /**
     * Add class field type matcher.
     * ----------------
     * 添加类字段的类型的匹配器。
     *
     *     addForType("org.luckypray.dexkit.demo.Annotation", StringMatchType.Equals, false)
     *
     * @param typeName field type name / 字段类型名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcherList]
     */
    @JvmOverloads
    fun addForType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        add(FieldMatcher().apply { type(typeName, matchType, ignoreCase) })
    }

    /**
     * Add class field type matcher.
     * ----------------
     * 添加类字段的类型的匹配器。
     *
     * @param clazz type class / 类型
     * @return [FieldMatcherList]
     */
    fun addForType(clazz: Class<*>) = also {
        add(FieldMatcher().apply { type(clazz) })
    }

    /**
     * Add class field name matcher.
     * ----------------
     * 添加类字段的名的匹配器。
     *
     *     addField("field", false)
     *
     * @param name field name / 字段名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcherList]
     */
    @JvmOverloads
    fun addForName(
        name: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        add(FieldMatcher().apply { name(name, matchType, ignoreCase) })
    }

    /**
     * Add [FieldMatcher].
     * ----------------
     * 添加 [FieldMatcher]。
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: FieldMatcher.() -> Unit) = also {
        add(FieldMatcher().apply(init))
    }
}

class UsingFieldMatcherList : ArrayList<UsingFieldMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<UsingFieldMatcher>): super(elements)

    /**
     * Add [UsingFieldMatcher].
     * ----------------
     * 添加 [UsingFieldMatcher]。
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: UsingFieldMatcher.() -> Unit) = also {
        add(UsingFieldMatcher().apply(init))
    }
}

class NumberEncodeValueMatcherList : ArrayList<NumberEncodeValueMatcher>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<NumberEncodeValueMatcher>): super(elements)

    /**
     * add number to be matched.
     * ----------------
     * 添加待匹配的数字。
     *
     * @param number number / 数字
     * @return [NumberEncodeValueMatcherList]
     */
    fun add(number: Number) = also {
        when (number) {
            is Byte -> addByte(number)
            is Short -> addShort(number)
            is Int -> addInt(number)
            is Long -> addLong(number)
            is Float -> addFloat(number)
            is Double -> addDouble(number)
        }
    }

    /**
     * add byteValue to be matched.
     * ----------------
     * 添加待匹配的 byteValue。
     *
     * @param value byteValue / 字节
     * @return [NumberEncodeValueMatcherList]
     */
    fun addByte(value: Byte) = also {
        add(NumberEncodeValueMatcher.createByte(value))
    }

    /**
     * add shortValue to be matched.
     * ----------------
     * 添加待匹配的 shortValue。
     *
     * @param value shortValue / 短整型
     * @return [NumberEncodeValueMatcherList]
     */
    fun addShort(value: Short) = also {
        add(NumberEncodeValueMatcher.createShort(value))
    }

    /**
     * add intValue to be matched.
     * ----------------
     * 添加待匹配的 intValue。
     *
     * @param value intValue / 整型
     * @return [NumberEncodeValueMatcherList]
     */
    fun addInt(value: Int) = also {
        add(NumberEncodeValueMatcher.createInt(value))
    }

    /**
     * add longValue to be matched.
     * ----------------
     * 添加待匹配的 longValue。
     *
     * @param value longValue / 长整型
     * @return [NumberEncodeValueMatcherList]
     */
    fun addLong(value: Long) = also {
        add(NumberEncodeValueMatcher.createLong(value))
    }

    /**
     * add floatValue to be matched.
     * ----------------
     * 添加待匹配的 floatValue。
     *
     * @param value floatValue / 单精度浮点型
     * @return [NumberEncodeValueMatcherList]
     */
    fun addFloat(value: Float) = also {
        add(NumberEncodeValueMatcher.createFloat(value))
    }

    /**
     * add doubleValue to be matched.
     * ----------------
     * 添加待匹配的 doubleValue。
     *
     * @param value doubleValue / 双精度浮点型
     * @return [NumberEncodeValueMatcherList]
     */
    fun addDouble(value: Double) = also {
        add(NumberEncodeValueMatcher.createDouble(value))
    }

    /**
     * Add [NumberEncodeValueMatcher].
     * ----------------
     * 添加 [NumberEncodeValueMatcher]。
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: NumberEncodeValueMatcher.() -> Unit) = also {
        add(NumberEncodeValueMatcher().apply(init))
    }
}

class StringMatchersGroupList : ArrayList<StringMatchersGroup>, IQuery {
    constructor(): super()
    constructor(initialCapacity: Int): super(initialCapacity)
    constructor(elements: Collection<StringMatchersGroup>): super(elements)

    /**
     * add a string matchers group.
     * ----------------
     * 添加一个字符串匹配器分组。
     *
     * @param groupName group name / 分组名称
     * @param usingStrings using strings / 使用的字符串
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatchersGroupList]
     */
    @JvmOverloads
    fun add(
        groupName: String,
        usingStrings: Collection<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(StringMatchersGroup(groupName, usingStrings.map { StringMatcher(it, matchType, ignoreCase) }))
    }

    /**
     * add a string matchers group.
     * ----------------
     * 添加一个字符串匹配器分组。
     *
     * @param groupName group name / 分组名称
     * @param init init / 初始化
     * @return [StringMatchersGroupList]
     */
    @kotlin.internal.InlineOnly
    inline fun add(
        groupName: String,
        init: StringMatcherList.() -> Unit
    ) = also {
        add(StringMatchersGroup(groupName, StringMatcherList().apply(init)))
    }

    /**
     * Add [StringMatchersGroup].
     * ----------------
     * 添加 [StringMatchersGroup]。
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: StringMatchersGroup.() -> Unit) = also {
        add(StringMatchersGroup().apply(init))
    }
}

