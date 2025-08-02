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
import org.luckypray.dexkit.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.MatchType

class AccessFlagsMatcher : BaseMatcher {
    /**
     * Access flags to match.
     * ----------------
     * 要匹配的访问标志。
     *
     *     modifiers = Modifier.PUBLIC or Modifier.STATIC
     */
    @set:JvmSynthetic
    var modifiers: Int = 0

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains

    constructor()

    /**
     * Create a new [AccessFlagsMatcher].
     * ----------------
     * 创建一个新的 [AccessFlagsMatcher]。
     *
     * @param modifiers access flags / 访问标志
     * @param matchType match type / 匹配类型
     * @return [AccessFlagsMatcher]
     */
    @JvmOverloads
    constructor(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) {
        this.modifiers = modifiers
        this.matchType = matchType
    }

    companion object {
        /**
         * Create a new [AccessFlagsMatcher].
         * ----------------
         * 创建一个新的 [AccessFlagsMatcher]。
         *
         * @param modifiers access flags / 访问标志
         * @param matchType match type / 匹配类型
         * @return [AccessFlagsMatcher]
         */
        @JvmOverloads
        fun create(
            modifiers: Int,
            matchType: MatchType = MatchType.Contains
        ) = AccessFlagsMatcher(modifiers, matchType)
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (modifiers == 0) throw IllegalArgumentException("modifiers must not be 0")
        val root = InnerAccessFlagsMatcher.createAccessFlagsMatcher(
            fbb,
            modifiers.toUInt(),
            matchType.value
        )
        fbb.finish(root)
        return root
    }
}