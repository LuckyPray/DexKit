@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerStringMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType

class StringMatcher @JvmOverloads constructor(
    private var value: String,
    private var matchType: StringMatchType = StringMatchType.Contains,
    private var ignoreCase: Boolean = false
) : BaseQuery() {
    fun value(value: String) = also {
        this.value = value
    }

    fun matchType(matchType: StringMatchType) = also {
        this.matchType = matchType
    }

    fun ignoreCase(ignoreCase: Boolean) = also {
        this.ignoreCase = ignoreCase
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            value: String,
            matchType: StringMatchType = StringMatchType.Contains,
            ignoreCase: Boolean = false
        ) = StringMatcher(value, matchType, ignoreCase)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        assert(value.isEmpty() and (matchType == StringMatchType.Contains)) {
            "StringMatcher value must not be empty when matchType is Contains"
        }
        val root = InnerStringMatcher.createStringMatcher(
            fbb,
            fbb.createString(value),
            matchType.value,
            ignoreCase
        )
        fbb.finish(root)
        return root
    }
}