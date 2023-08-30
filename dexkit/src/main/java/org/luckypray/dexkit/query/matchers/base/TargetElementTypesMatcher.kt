@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerTargetElementTypesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.TargetElementType

class TargetElementTypesMatcher  : BaseQuery {
    @set:JvmSynthetic
    var types: List<TargetElementType>? = null
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains

    constructor()

    @JvmOverloads
    constructor(
        types: List<TargetElementType>,
        matchType: MatchType = MatchType.Contains
    ) {
        this.types = types
        this.matchType = matchType
    }

    fun types(types: List<TargetElementType>) = also {
        this.types = types
    }

    fun types(vararg types: TargetElementType) = also {
        this.types = types.toList()
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    companion object {
        @JvmStatic
        fun create(
            types: List<TargetElementType>,
            matchType: MatchType = MatchType.Contains
        ) = TargetElementTypesMatcher(types, matchType)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerTargetElementTypesMatcher.createTargetElementTypesMatcher(
            fbb,
            types?.let { fbb.createVectorOfTables(it.map { it.value.toInt() }.toIntArray()) } ?: 0,
            matchType.value
        )
        fbb.finish(root)
        return root
    }
}