@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType

class AccessFlagsMatcher  : BaseQuery {
    @set:JvmSynthetic
    var modifiers: Int = 0
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains

    constructor()

    @JvmOverloads
    constructor(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) {
        this.modifiers = modifiers
        this.matchType = matchType
    }

    companion object {
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