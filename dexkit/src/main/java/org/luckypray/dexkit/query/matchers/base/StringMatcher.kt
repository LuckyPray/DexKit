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

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerStringMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.enums.StringMatchType

class StringMatcher : BaseMatcher, IAnnotationEncodeValue {
    @set:JvmSynthetic
    var value: String? = null
    @set:JvmSynthetic
    var matchType: StringMatchType = StringMatchType.Contains
    @set:JvmSynthetic
    var ignoreCase: Boolean = false
    var allOfMatchers: MutableList<StringMatcher>? = null
        private set
    var anyOfMatchers: MutableList<StringMatcher>? = null
        private set
    var noneOfMatchers: MutableList<StringMatcher>? = null
        private set

    constructor()

    /**
     * Create a new [StringMatcher].
     * ----------------
     * 创建一个新的 [StringMatcher]。
     *
     * @param value string / 字符串
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatcher]
     */
    @JvmOverloads
    constructor(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) {
        this.value = value
        this.matchType = matchType
        this.ignoreCase = ignoreCase
    }

    /**
     * Set the matched string.
     * ----------------
     * 设置待匹配的字符串。
     *
     * @param value string / 字符串
     * @return [StringMatcher]
     */
    fun value(value: String) = also {
        this.value = value
    }

    /**
     * Set the match type.
     * ----------------
     * 设置匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [StringMatcher]
     */
    fun matchType(matchType: StringMatchType) = also {
        this.matchType = matchType
    }

    /**
     * Set whether to ignore case.
     * ----------------
     * 设置是否忽略大小写。
     *
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatcher]
     */
    fun ignoreCase(ignoreCase: Boolean) = also {
        this.ignoreCase = ignoreCase
    }

    fun allOf(matchers: Collection<StringMatcher>) = also {
        this.allOfMatchers = matchers.takeIf { it.isNotEmpty() }?.toMutableList()
    }

    fun allOf(vararg matchers: StringMatcher) = also {
        allOf(matchers.asList())
    }

    fun addAllOf(matcher: StringMatcher) = also {
        allOfMatchers = allOfMatchers ?: mutableListOf()
        allOfMatchers!!.add(matcher)
    }

    fun anyOf(matchers: Collection<StringMatcher>) = also {
        this.anyOfMatchers = matchers.takeIf { it.isNotEmpty() }?.toMutableList()
    }

    fun anyOf(vararg matchers: StringMatcher) = also {
        anyOf(matchers.asList())
    }

    fun addAnyOf(matcher: StringMatcher) = also {
        anyOfMatchers = anyOfMatchers ?: mutableListOf()
        anyOfMatchers!!.add(matcher)
    }

    fun noneOf(matchers: Collection<StringMatcher>) = also {
        this.noneOfMatchers = matchers.takeIf { it.isNotEmpty() }?.toMutableList()
    }

    fun noneOf(vararg matchers: StringMatcher) = also {
        noneOf(matchers.asList())
    }

    fun addNoneOf(matcher: StringMatcher) = also {
        noneOfMatchers = noneOfMatchers ?: mutableListOf()
        noneOfMatchers!!.add(matcher)
    }

    fun not(matcher: StringMatcher) = also {
        addNoneOf(matcher)
    }

    @JvmSynthetic
    fun allOf(init: StringMatcherList.() -> Unit) = also {
        allOf(StringMatcherList().apply(init))
    }

    @JvmSynthetic
    fun addAllOf(init: StringMatcher.() -> Unit) = also {
        addAllOf(StringMatcher().apply(init))
    }

    @JvmSynthetic
    fun anyOf(init: StringMatcherList.() -> Unit) = also {
        anyOf(StringMatcherList().apply(init))
    }

    @JvmSynthetic
    fun addAnyOf(init: StringMatcher.() -> Unit) = also {
        addAnyOf(StringMatcher().apply(init))
    }

    @JvmSynthetic
    fun noneOf(init: StringMatcherList.() -> Unit) = also {
        noneOf(StringMatcherList().apply(init))
    }

    @JvmSynthetic
    fun addNoneOf(init: StringMatcher.() -> Unit) = also {
        addNoneOf(StringMatcher().apply(init))
    }

    @JvmSynthetic
    fun not(init: StringMatcher.() -> Unit) = also {
        not(StringMatcher().apply(init))
    }

    companion object {
        @JvmStatic
        fun create() = StringMatcher()

        /**
         * Create a new [StringMatcher].
         * ----------------
         * 创建一个新的 [StringMatcher]。
         *
         * @param value string / 字符串
         * @param matchType match type / 匹配类型
         * @param ignoreCase ignore case / 忽略大小写
         * @return [StringMatcher]
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            value: String,
            matchType: StringMatchType = StringMatchType.Contains,
            ignoreCase: Boolean = false
        ) = StringMatcher(value, matchType, ignoreCase)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val hasAtomicMatcher = value != null
        val hasCompositeMatcher = !allOfMatchers.isNullOrEmpty()
            || !anyOfMatchers.isNullOrEmpty()
            || !noneOfMatchers.isNullOrEmpty()
        require(hasAtomicMatcher || hasCompositeMatcher) {
            "either value or composite matchers must be specified"
        }
        if (hasAtomicMatcher && value!!.isEmpty() && matchType != StringMatchType.Equals) {
            matchType = StringMatchType.Equals
        }
        val root = InnerStringMatcher.createStringMatcher(
            fbb,
            value?.let { fbb.createString(it) } ?: 0,
            matchType.value,
            ignoreCase,
            allOfMatchers?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            anyOfMatchers?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            noneOfMatchers?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0
        )
        fbb.finish(root)
        return root
    }
}
