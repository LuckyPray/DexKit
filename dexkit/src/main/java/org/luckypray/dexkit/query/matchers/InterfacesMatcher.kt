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
import org.luckypray.dexkit.InnerInterfacesMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class InterfacesMatcher : BaseMatcher() {
    var interfacesMatcher: MutableList<ClassMatcher>? = null
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
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
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
     * Need to match interfaces.
     * ----------------
     * 要匹配的接口列表
     *
     * @param interfaces interfaces / 接口列表
     * @return [InterfacesMatcher]
     */
    fun interfaces(interfaces: Collection<ClassMatcher>) = also {
        this.interfacesMatcher = interfaces.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [InterfacesMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param count interface count / 接口数量
     * @return [InterfacesMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param range interface count range / 接口数量范围
     * @return [InterfacesMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param range interface count range / 接口数量范围
     * @return [InterfacesMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param min min count / 最小数量
     * @param max max count / 最大数量
     * @return [InterfacesMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param min min count / 最小数量
     * @return [InterfacesMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Interface count to match.
     * ----------------
     * 要匹配的接口数量。
     *
     * @param max max count / 最大数量
     * @return [InterfacesMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add interface class matcher to match.
     * ----------------
     * 添加要匹配的接口类匹配器
     *
     * @param interfaceMatcher interface class matcher / 接口类匹配器
     * @return [InterfacesMatcher]
     */
    fun add(interfaceMatcher: ClassMatcher) = also {
        interfacesMatcher = interfacesMatcher ?: mutableListOf()
        interfacesMatcher!!.add(interfaceMatcher)
    }

    /**
     * Add interface class name to match.
     * ----------------
     * 添加要匹配的接口类名
     *
     * @param className interface class name / 接口类名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [InterfacesMatcher]
     */
    @JvmOverloads
    fun add(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        add(ClassMatcher().apply { className(className, matchType, ignoreCase) })
    }

    // region DSL

    /**
     * @see add
     */
    @JvmSynthetic
    fun add(init: ClassMatcher.() -> Unit) = also {
        add(ClassMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = InterfacesMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerInterfacesMatcher.createInterfacesMatcher(
            fbb,
            interfacesMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}