@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerStringMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType

class StringMatcher : BaseQuery {
    @set:JvmSynthetic
    var value: String? = null
    @set:JvmSynthetic
    var matchType: StringMatchType = StringMatchType.Contains
    @set:JvmSynthetic
    var ignoreCase: Boolean = false

    constructor()
    @JvmOverloads
    constructor(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) {
        this.value = value
        this.matchType = matchType
        this.ignoreCase = ignoreCase
    }

    @JvmSynthetic
    internal fun getValue() = value

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
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        value ?: throw IllegalArgumentException("value must not be null")
        if (value!!.isEmpty() && matchType != StringMatchType.Equals) {
            throw IllegalAccessException("value '$value' is empty, matchType must be equals")
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