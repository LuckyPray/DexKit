@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerOpCodesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.util.OpCodeUtil

class OpCodesMatcher : BaseQuery {
    @set:JvmSynthetic
    var opCodes: List<Int>? = null
    @set:JvmSynthetic
    var matchType: OpCodeMatchType = OpCodeMatchType.Contains
    @set:JvmSynthetic
    var sizeRange: IntRange? = null

    constructor()

    @JvmOverloads
    constructor(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes
        this.matchType = matchType
        this.sizeRange = opCodeSize
    }

    @JvmOverloads
    constructor(
        opCodes: IntArray,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes.toList()
        this.matchType = matchType
        this.sizeRange = opCodeSize
    }

    fun opCodes(opCodes: List<Int>) = also { this.opCodes = opCodes }
    fun opCodes(opCodes: Array<Int>) = also { this.opCodes = opCodes.toList() }
    fun opNames(opNames: List<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun opNames(opNames: Array<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun matchType(matchType: OpCodeMatchType) = also {
        this.matchType = matchType
    }

    fun range(range: IntRange?) = also {
        this.sizeRange = range
    }

    fun range(range: kotlin.ranges.IntRange) = also {
        this.sizeRange = IntRange(range)
    }

    fun size(size: Int) = also {
        this.sizeRange = IntRange(size)
    }

    fun range(min: Int, max: Int) = also {
        this.sizeRange = IntRange(min, max)
    }

    companion object {
        @JvmStatic
        fun create(
            opCodes: List<Int>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opCodes, matchType, opCodeSize)
        }

        @JvmStatic
        fun create(
            opCodes: IntArray,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opCodes, matchType, opCodeSize)
        }

        @JvmStatic
        fun create(vararg opCodes: Int): OpCodesMatcher {
            return OpCodesMatcher(opCodes.toList())
        }

        @JvmStatic
        fun createForOpNames(
            opNames: List<String>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) }, matchType, opCodeSize)
        }

        @JvmStatic
        fun createForOpNames(
            opNames: Array<String>,
            matchType: OpCodeMatchType = OpCodeMatchType.Contains,
            opCodeSize: IntRange? = null
        ): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) }, matchType, opCodeSize)
        }

        @JvmStatic
        fun createForOpNames(vararg opNames: String): OpCodesMatcher {
            return OpCodesMatcher(opNames.map { OpCodeUtil.getOpCode(it) })
        }
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerOpCodesMatcher.createOpCodesMatcher(
            fbb,
            opCodes?.map { it.toShort() }?.let { InnerOpCodesMatcher.createOpCodesVector(fbb, it.toShortArray()) } ?: 0,
            matchType.value,
            sizeRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}