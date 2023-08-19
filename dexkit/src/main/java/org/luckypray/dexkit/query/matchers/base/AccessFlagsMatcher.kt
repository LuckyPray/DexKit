@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType

class AccessFlagsMatcher @JvmOverloads constructor(
    private var modifiers: Int,
    private var matchType: MatchType = MatchType.Equal
) : BaseQuery() {

    companion object {
        @JvmOverloads
        fun builder(
            modifiers: Int,
            matchType: MatchType = MatchType.Equal
        ) = AccessFlagsMatcher(modifiers, matchType)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
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