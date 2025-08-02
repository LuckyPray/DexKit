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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFieldsMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class FieldsMatcher : BaseMatcher() {
    var fieldsMatcher: MutableList<FieldMatcher>? = null
        private set

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var rangeMatcher: IntRange? = null
        private set

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     */
    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            count(value)
        }

    /**
     * Need to match fields.
     * ----------------
     * 要匹配的字段列表
     *
     * @param fields fields / 字段列表
     * @return [FieldsMatcher]
     */
    fun fields(fields: Collection<FieldMatcher>) = also {
        this.fieldsMatcher = fields.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [FieldsMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param count field count / 字段数量
     * @return [FieldsMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param range field count range / 字段数量范围
     * @return [FieldsMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param range field count range / 字段数量范围
     * @return [FieldsMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param min min field count / 最小字段数量
     * @param max max field count / 最大字段数量
     * @return [FieldsMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param min min field count / 最小字段数量
     * @return [FieldsMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Field count to match.
     * ----------------
     * 要匹配的字段数量。
     *
     * @param max max field count / 最大字段数量
     * @return [FieldsMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add field to match.
     * ----------------
     * 添加要匹配的字段。
     *
     * @param matcher field matcher / 字段匹配器
     * @return [FieldsMatcher]
     */
    fun add(matcher: FieldMatcher) = also {
        fieldsMatcher = fieldsMatcher ?: mutableListOf()
        fieldsMatcher!!.add(matcher)
    }

    /**
     * Add field name to match.
     * ----------------
     * 添加要匹配的字段名。
     *
     * @param name field name / 字段名
     * @return [FieldsMatcher]
     */
    fun addForName(name: String) = also {
        add(FieldMatcher().apply { name(name) })
    }

    /**
     * Add field type to match.
     * ----------------
     * 添加要匹配的字段类型。
     *
     * @param typeName field type name / 字段类型名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 是否忽略大小写
     * @return [FieldsMatcher]
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
     * add field type to match.
     * ----------------
     * 添加要匹配的字段类型。
     *
     * @param clazz field type / 字段类型
     * @return [FieldsMatcher]
     */
    fun addForType(clazz: Class<*>) = also {
        add(FieldMatcher().apply { type(clazz) })
    }

    // region DSL

    /**
     * @see add
     */
    @JvmSynthetic
    fun add(init: FieldMatcher.() -> Unit) = also {
        add(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldsMatcher.createFieldsMatcher(
            fbb,
            fieldsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}