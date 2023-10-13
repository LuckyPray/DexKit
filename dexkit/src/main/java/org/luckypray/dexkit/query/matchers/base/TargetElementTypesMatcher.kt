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
import org.luckypray.dexkit.InnerTargetElementTypesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.TargetElementType

class TargetElementTypesMatcher  : BaseQuery {
    /**
     * Target annotation declared element type list.
     * Corresponding to [java.lang.annotation.ElementType]
     * ----------------
     * 目标注解声明的元素类型列表。对应 [java.lang.annotation.ElementType]
     */
    @set:JvmSynthetic
    var types: Collection<TargetElementType>? = null

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains

    constructor()

    /**
     * Create a new [TargetElementTypesMatcher].
     * ----------------
     * 创建一个新的 [TargetElementTypesMatcher]。
     *
     * @param types target annotation declared element type list / 目标注解声明的元素类型列表
     * @param matchType match type / 匹配类型
     */
    @JvmOverloads
    constructor(
        types: Collection<TargetElementType>,
        matchType: MatchType = MatchType.Contains
    ) {
        this.types = types
        this.matchType = matchType
    }

    /**
     * Set the target annotation declared element type list.
     * Corresponding to [java.lang.annotation.ElementType]
     * ----------------
     * 设置目标注解声明的元素类型列表。对应 [java.lang.annotation.ElementType]
     *
     * @param types target annotation declared element type list / 目标注解声明的元素类型列表
     */
    fun types(types: Collection<TargetElementType>) = also {
        this.types = types
    }

    /**
     * Set the target annotation declared element type list.
     * Corresponding to [java.lang.annotation.ElementType]
     * ----------------
     * 设置目标注解声明的元素类型列表。对应 [java.lang.annotation.ElementType]
     *
     * @param types target annotation declared element type list / 目标注解声明的元素类型列表
     * @return [TargetElementTypesMatcher]
     */
    fun types(vararg types: TargetElementType) = also {
        this.types = types.toList()
    }

    /**
     * Set the match type.
     * ----------------
     * 设置匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [TargetElementTypesMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    companion object {
        /**
         * Create a new [TargetElementTypesMatcher].
         * ----------------
         * 创建一个新的 [TargetElementTypesMatcher]。
         *
         * @param types target annotation declared element type list / 目标注解声明的元素类型列表
         * @param matchType match type / 匹配类型
         */
        @JvmStatic
        fun create(
            types: Collection<TargetElementType>,
            matchType: MatchType = MatchType.Contains
        ) = TargetElementTypesMatcher(types, matchType)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerTargetElementTypesMatcher.createTargetElementTypesMatcher(
            fbb,
            types?.map { it.value }?.toByteArray()
                ?.let { InnerTargetElementTypesMatcher.createTypesVector(fbb, it) } ?: 0,
            matchType.value
        )
        fbb.finish(root)
        return root
    }
}