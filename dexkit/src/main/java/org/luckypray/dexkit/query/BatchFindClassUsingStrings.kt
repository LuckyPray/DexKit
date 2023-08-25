@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchFindClassUsingStrings
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.BatchUsingStringsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.result.ClassData

class BatchFindClassUsingStrings : BaseQuery() {
    private var searchPackage: String? = null
    private var searchClasses: LongArray? = null
    private var matchers: List<BatchUsingStringsMatcher>? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun searchIn(classes: List<ClassData>) = also {
        this.searchClasses = classes.map { getEncodeId(it.dexId, it.id) }.toLongArray()
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

    @kotlin.internal.InlineOnly
    inline fun matcher(init: BatchUsingStringsMatcherList.() -> Unit) = also {
        matchers(BatchUsingStringsMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = BatchFindClassUsingStrings()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        matchers ?: throw IllegalAccessException("matchers must be set")
        val root = InnerBatchFindClassUsingStrings.createBatchFindClassUsingStrings(
            fbb,
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            searchClasses?.let { InnerBatchFindClassUsingStrings.createInClassesVector(fbb, it) } ?: 0,
            fbb.createVectorOfTables(matchers!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}