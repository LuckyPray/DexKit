@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType

class AccessFlagsMatcher @JvmOverloads constructor(
    private var modifiers: Int,
    private var matchType: MatchType = MatchType.Equal
) : BaseQuery() {

    companion object {
        @JvmOverloads
        fun create(
            modifiers: Int,
            matchType: MatchType = MatchType.Equal
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