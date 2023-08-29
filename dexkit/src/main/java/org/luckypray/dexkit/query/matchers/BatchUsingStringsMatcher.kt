@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchUsingStringsMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class BatchUsingStringsMatcher @JvmOverloads constructor(
    @set:JvmSynthetic
    var unionKey: String,
    @set:JvmSynthetic
    var usingStringsMatcher: List<StringMatcher> = StringMatcherList(),
) : BaseQuery() {

    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            usingStringsMatcher = value.map { StringMatcher(it) }
        }

    fun unionKey(unionKey: String) = also {
        this.unionKey = unionKey
    }

    fun usingStrings(usingStrings: StringMatcherList) = also {
        this.usingStringsMatcher = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }
    }

    fun usingStrings(vararg usingString: String) = also {
        this.usingStringsMatcher = usingString.map { StringMatcher(it) }
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            unionKey: String,
            matchers: List<StringMatcher> = mutableListOf()
        ) = BatchUsingStringsMatcher(unionKey, matchers)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (unionKey.isEmpty()) throw IllegalAccessException("unionKey not be empty")
        if (usingStringsMatcher.isEmpty()) throw IllegalAccessException("matchers not be empty")
        val root = InnerBatchUsingStringsMatcher.createBatchUsingStringsMatcher(
            fbb,
            fbb.createString(unionKey),
            fbb.createVectorOfTables(usingStringsMatcher.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}