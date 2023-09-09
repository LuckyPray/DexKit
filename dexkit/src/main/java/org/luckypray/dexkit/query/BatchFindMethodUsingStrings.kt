@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchFindMethodUsingStrings
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.StringMatchersGroup
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData

class BatchFindMethodUsingStrings : BaseQuery() {
    @set:JvmSynthetic
    var searchPackages: List<String>? = null
    @set:JvmSynthetic
    var excludePackages: List<String>? = null
    @set:JvmSynthetic
    var ignorePackagesCase: Boolean = false
    @set:JvmSynthetic
    var searchClasses: List<ClassData>? = null
    @set:JvmSynthetic
    var searchMethods: List<MethodData>? = null
    var searchGroups: MutableList<StringMatchersGroup>? = null
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

    fun searchInClasses(classes: List<ClassData>) = also {
        this.searchClasses = classes
    }

    fun searchInMethods(methods: List<MethodData>) = also {
        this.searchMethods = methods
    }

    fun matchers(matchers: List<StringMatchersGroup>) = also {
        this.searchGroups = matchers.toMutableList()
    }

    @JvmOverloads
    fun matchers(
        map: Map<String, Collection<String>>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.searchGroups = map.map { (key, value) ->
            StringMatchersGroup(key, value.map { StringMatcher(it, matchType, ignoreCase) })
        }.toMutableList()
    }

    fun addSearchGroup(matcher: StringMatchersGroup) = also {
        searchGroups = searchGroups ?: mutableListOf()
        searchGroups!!.add(matcher)
    }

    @JvmOverloads
    fun addSearchGroup(
        groupName: String,
        usingStrings: List<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        addSearchGroup(StringMatchersGroup(groupName, usingStrings.map { StringMatcher(it, matchType, ignoreCase) }))
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun matchers(init: StringMatchersGroupList.() -> Unit) = also {
        matchers(StringMatchersGroupList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addSearchGroup(init: StringMatchersGroup.() -> Unit) = also {
        addSearchGroup(StringMatchersGroup().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addSearchGroup(
        groupName: String,
        init: StringMatcherList.() -> Unit
    ) = also {
        addSearchGroup(StringMatchersGroup(groupName, StringMatcherList().apply(init)))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = BatchFindMethodUsingStrings()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        searchGroups ?: throw IllegalAccessException("searchGroups not be empty")
        if (searchGroups!!.map { it.groupName }.toSet().size < searchGroups!!.size) {
            throw IllegalAccessException("groupName must be unique")
        }
        val root = InnerBatchFindMethodUsingStrings.createBatchFindMethodUsingStrings(
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
                ?.let { InnerBatchFindMethodUsingStrings.createInClassesVector(fbb, it) } ?: 0,
            searchMethods
                ?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerBatchFindMethodUsingStrings.createInMethodsVector(fbb, it) } ?: 0,
            fbb.createVectorOfTables(searchGroups!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}