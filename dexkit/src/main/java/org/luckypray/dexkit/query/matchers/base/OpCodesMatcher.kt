@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerOpCodesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.util.OpCodeUtil

class OpCodesMatcher : BaseQuery {
    @set:JvmSynthetic
    var opCodes: Collection<Int>? = null
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

    constructor()

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

    fun opCodes(opCodes: Collection<Int>) = also {
        this.opCodes = opCodes
    }
    fun opCodes(opCodes: Array<Int>) = also {
        this.opCodes = opCodes.toList()
    }
    fun opNames(opNames: Collection<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun opNames(opNames: Array<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun matchType(matchType: OpCodeMatchType) = also {
        this.matchType = matchType
    }

    fun size(size: Int) = also {
        this.rangeMatcher = IntRange(size)
    }

    fun size(range: IntRange?) = also {
        this.rangeMatcher = range
    }

    fun size(range: kotlin.ranges.IntRange) = also {
        this.rangeMatcher = IntRange(range)
    }

    fun size(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun sizeMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    fun sizeMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    companion object {
        @JvmStatic
        fun create(
            opCodes: Collection<Int>,
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
            opNames: Collection<String>,
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
            opCodes?.map { it.toShort() }?.toShortArray()
                ?.let { InnerOpCodesMatcher.createOpCodesVector(fbb, it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}