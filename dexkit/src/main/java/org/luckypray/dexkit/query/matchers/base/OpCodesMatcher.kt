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
import org.luckypray.dexkit.InnerOpCodesMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.util.OpCodeUtil

class OpCodesMatcher : BaseMatcher {
    /**
     * OpCodes to match.
     */
    @set:JvmSynthetic
    var opCodes: Collection<Int>? = null

    /**
     * Match type. Default is [OpCodeMatchType.Contains].
     */
    @set:JvmSynthetic
    var matchType: OpCodeMatchType = OpCodeMatchType.Contains

    var rangeMatcher: IntRange? = null
        private set

    /**
     * OpCodes size to match.
     */
    var size: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            size(value)
        }

    constructor()

    /**
     * Create a new [OpCodesMatcher].
     * ----------------
     * 创建一个新的 [OpCodesMatcher]。
     *
     * @param opCodes opCodes / 操作码
     * @param matchType match type / 匹配类型
     * @param opCodeSize opCode size / 操作码长度
     * @return [OpCodesMatcher]
     */
    @JvmOverloads
    constructor(
        opCodes: Collection<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes
        this.matchType = matchType
        this.rangeMatcher = opCodeSize
    }

    /**
     * Create a new [OpCodesMatcher].
     * ----------------
     * 创建一个新的 [OpCodesMatcher]。
     *
     * @param opCodes opCodes / 操作码
     * @param matchType match type / 匹配类型
     * @param opCodeSize opCode size / 操作码长度
     * @return [OpCodesMatcher]
     */
    @JvmOverloads
    constructor(
        opCodes: IntArray,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes.toList()
        this.matchType = matchType
        this.rangeMatcher = opCodeSize
    }

    /**
     * Set the opCodes to be matched.
     * ----------------
     * 设置待匹配的操作码。
     *
     * @param opCodes opCodes / 操作码
     * @return [OpCodesMatcher]
     */
    fun opCodes(opCodes: Collection<Int>) = also {
        this.opCodes = opCodes
    }

    /**
     * Set the opCodes to be matched.
     * ----------------
     * 设置待匹配的操作码。
     *
     * @param opCodes opCodes / 操作码
     * @return [OpCodesMatcher]
     */
    fun opCodes(opCodes: Array<Int>) = also {
        this.opCodes = opCodes.toList()
    }

    /**
     * Set the smali instruction sequence to be matched.
     * ----------------
     * 设置待匹配的 smali 指令序列。
     *
     * @param opNames smali instruction sequence / smali 指令序列
     * @return [OpCodesMatcher]
     */
    fun opNames(opNames: Collection<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    /**
     * Set the smali instruction sequence to be matched.
     * ----------------
     * 设置待匹配的 smali 指令序列。
     *
     * @param opNames smali instruction sequence / smali 指令序列
     * @return [OpCodesMatcher]
     */
    fun opNames(opNames: Array<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    /**
     * Set the match type.
     * ----------------
     * 设置匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [OpCodesMatcher]
     */
    fun matchType(matchType: OpCodeMatchType) = also {
        this.matchType = matchType
    }

    /**
     * Set the opCodes size to be matched.
     * ----------------
     * 设置待匹配的操作码长度。
     *
     * @param size opCodes size / 操作码长度
     * @return [OpCodesMatcher]
     */
    fun size(size: Int) = also {
        this.rangeMatcher = IntRange(size)
    }

    /**
     * Set the opCodes size range to be matched.
     * ----------------
     * 设置待匹配的操作码长度范围。
     *
     * @param range opCodes size range / 操作码长度范围
     * @return [OpCodesMatcher]
     */
    fun size(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Set the opCodes size range to be matched.
     * ----------------
     * 设置待匹配的操作码长度范围。
     *
     * @param range opCodes size range / 操作码长度范围
     * @return [OpCodesMatcher]
     */
    fun size(range: kotlin.ranges.IntRange) = also {
        this.rangeMatcher = IntRange(range)
    }

    /**
     * Set the opCodes size range to be matched.
     * ----------------
     * 设置待匹配的操作码长度范围。
     *
     * @param min min opCodes size / 最小操作码长度
     * @param max max opCodes size / 最大操作码长度
     * @return [OpCodesMatcher]
     */
    fun size(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Set the opCodes size min to be matched.
     * ----------------
     * 设置待匹配的操作码长度最小值。
     *
     * @param min min opCodes size / 最小操作码长度
     * @return [OpCodesMatcher]
     */
    fun sizeMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Set the opCodes size max to be matched.
     * ----------------
     * 设置待匹配的操作码长度最大值。
     */
    fun sizeMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    companion object {
        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opCodes opCodes / 操作码
         * @param matchType match type / 匹配类型
         * @param opCodeSize opCode size / 操作码长度
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun create(
            opCodes: Collection<Int>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opCodes, matchType, opCodeSize)
        }

        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opCodes opCodes / 操作码
         * @param matchType match type / 匹配类型
         * @param opCodeSize opCode size / 操作码长度
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun create(
            opCodes: IntArray,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opCodes, matchType, opCodeSize)
        }

        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opCodes opCodes / 操作码
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun create(vararg opCodes: Int): OpCodesMatcher {
            return OpCodesMatcher(opCodes.toList())
        }

        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opNames smali instruction sequence / smali 指令序列
         * @param matchType match type / 匹配类型
         * @param opCodeSize opCode size / 操作码长度
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun createForOpNames(
            opNames: Collection<String>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) }, matchType, opCodeSize)
        }

        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opNames smali instruction sequence / smali 指令序列
         * @param matchType match type / 匹配类型
         * @param opCodeSize opCode size / 操作码长度
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun createForOpNames(
            opNames: Array<String>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) }, matchType, opCodeSize)
        }

        /**
         * Create a new [OpCodesMatcher].
         * ----------------
         * 创建一个新的 [OpCodesMatcher]。
         *
         * @param opNames smali instruction sequence / smali 指令序列
         * @return [OpCodesMatcher]
         */
        @JvmStatic
        fun createForOpNames(vararg opNames: String): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) })
        }
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerOpCodesMatcher.createOpCodesMatcher(
            fbb,
            opCodes?.map { it.toShort() }?.toShortArray()
                ?.let { InnerOpCodesMatcher.createOpCodesVector(fbb, it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}