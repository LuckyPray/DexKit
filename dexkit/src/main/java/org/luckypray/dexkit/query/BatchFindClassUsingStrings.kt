@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchFindClassUsingStrings
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.StringMatchersGroup
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
    var matcherGroups: MutableList<StringMatchersGroup>? = null
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

    fun matchers(matchers: List<StringMatchersGroup>) = also {
        this.matcherGroups = matchers.toMutableList()
    }

    @JvmOverloads
    fun matchers(
        map: Map<String, List<String>>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.matcherGroups = map.map { (key, value) ->
            StringMatchersGroup(key, value.map { StringMatcher(it, matchType, ignoreCase) })
        }.toMutableList()
    }

    fun addGroup(matcher: StringMatchersGroup) = also {
        matcherGroups = matcherGroups ?: mutableListOf()
        matcherGroups!!.add(matcher)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun matchers(init: StringMatchersGroupList.() -> Unit) = also {
        matchers(StringMatchersGroupList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addGroup(init: StringMatchersGroup.() -> Unit) = also {
        addGroup(StringMatchersGroup().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addGroup(
        groupName: String,
        init: StringMatcherList.() -> Unit
    ) = also {
        addGroup(StringMatchersGroup(groupName, StringMatcherList().apply(init)))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = BatchFindClassUsingStrings()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        matcherGroups ?: throw IllegalAccessException("matchers must be set")
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
            fbb.createVectorOfTables(matcherGroups!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}