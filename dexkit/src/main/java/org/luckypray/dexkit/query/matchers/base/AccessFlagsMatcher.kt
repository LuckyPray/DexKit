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
import org.luckypray.dexkit.DexAccessFlags
import org.luckypray.dexkit.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.MatchType

/**
 * Matcher for raw DEX access flags.
 *
 * The public [java.lang.reflect.Modifier] constants are a numeric subset of the DEX flags and remain
 * usable when they are valid for the target being matched. Most callers only need `Modifier`. Use
 * [DexAccessFlags] when `Modifier` does not expose a required flag or exact DEX semantics are needed.
 * See
 * [DEX access_flags](https://source.android.com/docs/core/runtime/dex-format#access-flags).
 *
 * ----------------
 * 原始 DEX 访问标志匹配器。
 *
 * [java.lang.reflect.Modifier] 的公开常量在数值上是 DEX 标志的子集；当常量适用于当前目标时
 * 仍可直接使用。大多数用户只需要 `Modifier`；仅当它未公开所需标志或需要精确 DEX 语义时，
 * 才需要 [DexAccessFlags]。
 */
class AccessFlagsMatcher : BaseMatcher {
    /**
     * Raw DEX access flags to match.
     * ----------------
     * 要匹配的原始 DEX 访问标志。
     *
     *     modifiers = Modifier.PUBLIC or DexAccessFlags.SYNTHETIC
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
