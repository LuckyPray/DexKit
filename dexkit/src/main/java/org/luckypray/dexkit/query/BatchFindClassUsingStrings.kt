@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerBatchFindClassUsingStrings
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.BatchUsingStringsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class BatchFindClassUsingStrings : BaseQuery() {
    private var searchPackage: String? = null
    private var searchClasses: IntArray? = null
    private var matchers: List<BatchUsingStringsMatcher>? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun searchIn(classes: List<ClassData>) = also {
        this.searchClasses = classes.map { it.id }.toIntArray()
    }

    fun matchers(matchers: List<BatchUsingStringsMatcher>) = also {
        this.matchers = matchers
    }

    @JvmOverloads
    fun matchers(
        map: Map<String, List<String>>,
        matchType: StringMatchType = StringMatchType.SimilarRegex,
        ignoreCase: Boolean = false
    ) = also {
        this.matchers = map.map { (key, value) ->
            BatchUsingStringsMatcher(key, value.map { StringMatcher(it, matchType, ignoreCase) })
        }
    }

    // region DSL

    fun matcher(init: BatchUsingStringsMatcherList.() -> Unit) = also {
        this.matchers = BatchUsingStringsMatcherList().apply(init)
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = BatchFindClassUsingStrings()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        matchers ?: throw IllegalAccessException("matchers must be set")
        val root = InnerBatchFindClassUsingStrings.createBatchFindClassUsingStrings(
            fbb,
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            searchClasses?.let { fbb.createVectorOfTables(it) } ?: 0,
            fbb.createVectorOfTables(matchers!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}