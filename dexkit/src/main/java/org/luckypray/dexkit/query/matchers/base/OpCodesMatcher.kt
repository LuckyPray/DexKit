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
    var rangeMatcher: IntRange? = null
        private set

    var size: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            size(value)
        }
    var range: kotlin.ranges.IntRange
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            range(value)
        }

    constructor()

    @JvmOverloads
    constructor(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes
        this.matchType = matchType
        this.rangeMatcher = opCodeSize
    }

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
        this.rangeMatcher = range
    }

    fun range(range: kotlin.ranges.IntRange) = also {
        this.rangeMatcher = IntRange(range)
    }

    fun range(min: Int, max: Int) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun size(size: Int) = also {
        this.rangeMatcher = IntRange(size)
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
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}