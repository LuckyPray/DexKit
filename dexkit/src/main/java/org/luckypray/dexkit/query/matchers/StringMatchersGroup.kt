@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchUsingStringsMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class StringMatchersGroup : BaseQuery {

    @set:JvmSynthetic
    var groupName: String? = null
    var stringMatchers = mutableListOf<StringMatcher>()
        private set

    constructor()
    constructor(groupName: String, stringMatchers: List<StringMatcher>) {
        this.groupName = groupName
        this.stringMatchers = stringMatchers.toMutableList()
    }

    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            usingStrings(value)
        }

    fun groupName(groupName: String) = also {
        this.groupName = groupName
    }

    fun usingStrings(usingStrings: StringMatcherList) = also {
        this.stringMatchers = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.stringMatchers = StringMatcherList(usingStrings.map { StringMatcher(it) })
    }

    fun usingStrings(vararg usingStrings: String) = also {
        this.stringMatchers = StringMatcherList(usingStrings.map { StringMatcher(it) })
    }

    fun add(matcher: StringMatcher) = also {
        stringMatchers.add(matcher)
    }

    @JvmOverloads
    fun add(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(StringMatcher(usingString, matchType, ignoreCase))
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = StringMatchersGroup()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        groupName ?: throw IllegalAccessException("groupName not be null")
        if (stringMatchers.isEmpty()) throw IllegalAccessException("matchers not be empty")
        val root = InnerBatchUsingStringsMatcher.createBatchUsingStringsMatcher(
            fbb,
            fbb.createString(groupName),
            fbb.createVectorOfTables(stringMatchers.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}