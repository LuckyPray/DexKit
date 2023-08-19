@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerTargetElementTypesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.TargetElementType

class TargetElementTypesMatcher @JvmOverloads constructor(
    private var types: List<TargetElementType>? = null,
    private var matchType: MatchType = MatchType.Equal
) : BaseQuery() {

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
            types: List<TargetElementType>? = null,
            matchType: MatchType = MatchType.Equal
        ) = TargetElementTypesMatcher(types, matchType)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerTargetElementTypesMatcher.createTargetElementTypesMatcher(
            fbb,
            types?.let { fbb.createVectorOfTables(it.map { it.value.toInt() }.toIntArray()) } ?: 0,
            matchType.value
        )
        fbb.finish(root)
        return root
    }
}