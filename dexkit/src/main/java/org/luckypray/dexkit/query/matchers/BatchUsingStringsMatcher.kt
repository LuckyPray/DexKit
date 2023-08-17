@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.DexKitDsl
import org.luckypray.dexkit.alias.InnerBatchUsingStringsMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class BatchUsingStringsMatcher @JvmOverloads constructor(
    private var unionKey: String,
    private var usingStrings: List<StringMatcher> = mutableListOf(),
) : BaseQuery() {

    fun unionKey(unionKey: String) = also {
        this.unionKey = unionKey
    }

    fun usingStringsMatcher(usingStrings: List<StringMatcher>) = also {
        this.usingStrings = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStrings = usingStrings.map { StringMatcher(it) }
    }

    fun usingStrings(vararg usingString: String) = also {
        this.usingStrings = usingString.map { StringMatcher(it) }
    }

    // region DSL

    fun usingStringsMatcher(init: (@DexKitDsl MutableList<StringMatcher>).() -> Unit) = also {
        usingStringsMatcher(mutableListOf<StringMatcher>().apply(init))
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

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        assert(unionKey.isNotEmpty()) { "unionKey must be set" }
        assert(usingStrings.isNotEmpty()) { "matchers not be empty" }
        val root = InnerBatchUsingStringsMatcher.createBatchUsingStringsMatcher(
            fbb,
            fbb.createString(unionKey),
            fbb.createVectorOfTables(usingStrings.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}