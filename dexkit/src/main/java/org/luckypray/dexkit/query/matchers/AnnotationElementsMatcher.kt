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
import org.luckypray.dexkit.InnerAnnotationElementsMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationElementsMatcher : BaseMatcher() {
    var elementsMatcher: MutableList<AnnotationElementMatcher>? = null
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
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
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
     * Need to match annotation elements.
     * ----------------
     * 要匹配的注解元素列表
     *
     * @param elements elements / 元素列表
     * @return [AnnotationElementsMatcher]
     */
    fun elements(elements: Collection<AnnotationElementMatcher>) = also {
        this.elementsMatcher = elements.toMutableList()
    }

    /**
     * Need to match annotation elements.
     * ----------------
     * 要匹配的注解元素列表
     *
     * @param elements elements / 元素列表
     * @return [AnnotationElementsMatcher]
     */
    fun elements(vararg elements: AnnotationElementMatcher) = also {
        this.elementsMatcher = elements.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [AnnotationElementsMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param count count / 数量
     * @return [AnnotationElementsMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param range count range / 数量范围
     * @return [AnnotationElementsMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param range count range / 数量范围
     * @return [AnnotationElementsMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param min min count / 最小数量
     * @param max max count / 最大数量
     * @return [AnnotationElementsMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param min min count / 最小数量
     * @return [AnnotationElementsMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Annotation elements count to match.
     * ----------------
     * 要匹配的注解元素数量。
     *
     * @param max max count / 最大数量
     * @return [AnnotationElementsMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add annotation element to match.
     * ----------------
     * 添加要匹配的注解元素。
     *
     * @param element element / 元素
     * @return [AnnotationElementsMatcher]
     */
    fun add(element: AnnotationElementMatcher) = also {
        elementsMatcher = elementsMatcher ?: mutableListOf()
        elementsMatcher!!.add(element)
    }

    /**
     * Add annotation element to match.
     * ----------------
     * 添加要匹配的注解元素。
     *
     * @param name element name / 元素名称
     * @param matcher element value matcher / 元素值匹配器
     * @return [AnnotationElementsMatcher]
     */
    @JvmOverloads
    fun add(name: String, matcher: AnnotationEncodeValueMatcher? = null) = also {
        add(AnnotationElementMatcher().apply {
            name(name)
            matcher?.let { this.value(it) }
        })
    }

    // region DSL

    /**
     * @see add
     */
    @JvmSynthetic
    fun add(init: AnnotationElementMatcher.() -> Unit) = also {
        add(AnnotationElementMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementsMatcher.createAnnotationElementsMatcher(
            fbb,
            elementsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}