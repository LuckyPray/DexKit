@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerOpCodesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.util.OpCodeUtil

class OpCodesMatcher : BaseQuery {
    private var opCodes: List<Int>? = null
    private var matchType: OpCodeMatchType = OpCodeMatchType.Contains
    private var methodOpCodeSize: IntRange? = null

    constructor()

    @JvmOverloads
    constructor(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes
        this.matchType = matchType
        this.methodOpCodeSize = opCodeSize
    }

    @JvmOverloads
    constructor(
        opCodes: IntArray,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = opCodes.toList()
        this.matchType = matchType
        this.methodOpCodeSize = opCodeSize
    }

    fun opCodes(opCodes: List<Int>) = also { this.opCodes = opCodes }
    fun opCodes(opCodes: Array<Int>) = also { this.opCodes = opCodes.toList() }
    fun opNames(opNames: List<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun opNames(opNames: Array<String>) = also {
        this.opCodes = opNames.map { OpCodeUtil.getOpCode(it) }
    }

    fun matchType(matchType: OpCodeMatchType) = also { this.matchType = matchType }
    fun opCodeSize(opCodeSize: IntRange?) = also { this.methodOpCodeSize = opCodeSize }

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
            opCodes?.let { fbb.createVectorOfTables(it.toIntArray()) } ?: 0,
            matchType.value,
            methodOpCodeSize?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}