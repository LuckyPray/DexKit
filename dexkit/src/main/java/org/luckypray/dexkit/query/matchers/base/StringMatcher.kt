@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerStringMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType

class StringMatcher @JvmOverloads constructor(
    private var value: String? = null,
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
            value: String? = null,
            matchType: StringMatchType = StringMatchType.Contains,
            ignoreCase: Boolean = false
        ) = StringMatcher(value, matchType, ignoreCase)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        value ?: throw IllegalArgumentException("value must not be null")
        if (value!!.isEmpty() && matchType != StringMatchType.Equal) {
            throw IllegalAccessException("value '$value' is empty, matchType must be equal")
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