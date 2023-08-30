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
    @set:JvmSynthetic
    var searchPackages: List<String>? = null
    @set:JvmSynthetic
    var excludePackages: List<String>? = null
    @set:JvmSynthetic
    var ignorePackagesCase: Boolean = false
    @set:JvmSynthetic
    var searchClasses: List<ClassData>? = null
    var matchers: List<BatchUsingStringsMatcher>? = null
        private set

    fun searchPackages(vararg searchPackages: String) = also {
        this.searchPackages = searchPackages.toList()
    }

    fun searchPackages(searchPackages: List<String>) = also {
        this.searchPackages = searchPackages
    }

    fun excludePackages(vararg excludePackages: String) = also {
        this.excludePackages = excludePackages.toList()
    }

    fun excludePackages(excludePackages: List<String>) = also {
        this.excludePackages = excludePackages
    }

    fun ignorePackagesCase(ignorePackagesCase: Boolean) = also {
        this.ignorePackagesCase = ignorePackagesCase
    }

    fun searchIn(classes: List<ClassData>) = also {
        this.searchClasses = classes
    }

    fun matchers(matchers: List<BatchUsingStringsMatcher>) = also {
        this.matchers = matchers
    }

    @JvmOverloads
    fun matchers(
        map: Map<String, List<String>>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.matchers = map.map { (key, value) ->
            BatchUsingStringsMatcher(key, value.map { StringMatcher(it, matchType, ignoreCase) })
        }
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun matchers(init: BatchUsingStringsMatcherList.() -> Unit) = also {
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
            searchPackages
                ?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            excludePackages
                ?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            ignorePackagesCase,
            searchClasses
                ?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerBatchFindClassUsingStrings.createInClassesVector(fbb, it) } ?: 0,
            fbb.createVectorOfTables(matchers!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}