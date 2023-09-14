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
    var searchGroups: MutableList<StringMatchersGroup>? = null
        private set

    /**
     * Search classes in the specified packages.
     * ----------------
     * 在指定的包中搜索类。
     *
     *     searchPackages("java.lang", "java.util")
     *
     * @param searchPackages search packages / 搜索包
     * @return [BatchFindClassUsingStrings]
     */
    fun searchPackages(vararg searchPackages: String) = also {
        this.searchPackages = searchPackages.toList()
    }

    /**
     * Search classes in the specified packages.
     * ----------------
     * 在指定的包中搜索类。
     *
     *     searchPackages(listOf("java.lang", "java.util"))
     *
     * @param searchPackages search packages / 搜索包
     * @return [BatchFindClassUsingStrings]
     */
    fun searchPackages(searchPackages: List<String>) = also {
        this.searchPackages = searchPackages
    }

    /**
     * Exclude classes in the specified packages.
     * ----------------
     * 排除指定包中的类。
     *
     *     excludePackages("java.lang", "java.util")
     *
     * @param excludePackages exclude packages / 排除包
     * @return [BatchFindClassUsingStrings]
     */
    fun excludePackages(vararg excludePackages: String) = also {
        this.excludePackages = excludePackages.toList()
    }

    /**
     * Exclude classes in the specified packages.
     * ----------------
     * 排除指定包中的类。
     *
     *     excludePackages(listOf("java.lang", "java.util"))
     *
     * @param excludePackages exclude packages / 排除包
     * @return [BatchFindClassUsingStrings]
     */
    fun excludePackages(excludePackages: List<String>) = also {
        this.excludePackages = excludePackages
    }

    /**
     * Ignore case with [searchPackages] and [excludePackages].
     * ----------------
     * 忽略 [searchPackages] 和 [excludePackages] 的大小写。
     *
     * @param ignorePackagesCase ignore case / 忽略大小写
     * @return [BatchFindClassUsingStrings]
     */
    fun ignorePackagesCase(ignorePackagesCase: Boolean) = also {
        this.ignorePackagesCase = ignorePackagesCase
    }

    /**
     * Search in the specified [ClassData] list.
     * ----------------
     * 在指定的 [ClassData] 列表中搜索指定类。
     *
     * @param classes search classes / 类列表
     * @return [BatchFindClassUsingStrings]
     */
    fun searchIn(classes: List<ClassData>) = also {
        this.searchClasses = classes
    }

    /**
     * find all classes that use the specified strings.
     * ----------------
     * 查找所有使用指定字符串的类。
     *
     * @param matchers string matchers group / 字符串匹配器分组
     * @return [BatchFindClassUsingStrings]
     */
    fun matchers(matchers: List<StringMatchersGroup>) = also {
        this.searchGroups = matchers.toMutableList()
    }

    /**
     * find all classes that use the specified strings.
     * ----------------
     * 查找所有使用指定字符串的类。
     *
     *     val keywordsMap = mapOf(
     *         "Class1" to listOf("TAG", "TAG1", "TAG2"),
     *         "Class2" to listOf("Handler", "Handler1", "Handler2")
     *     )
     *     bridge.matchers(keywordsMap, StringMatchType.Equals, false)
     *
     * @param keywordsMap keywords group map / 关键字分组映射
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [BatchFindClassUsingStrings]
     */
    @JvmOverloads
    fun matchers(
        keywordsMap: Map<String, Collection<String>>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.searchGroups = keywordsMap.map { (key, value) ->
            StringMatchersGroup(key, value.map { StringMatcher(it, matchType, ignoreCase) })
        }.toMutableList()
    }

    /**
     * add a string matchers group.
     * ----------------
     * 添加一个字符串匹配器分组。
     *
     * @param matcher string matchers group / 字符串匹配器分组
     * @return [BatchFindClassUsingStrings]
     */
    fun addSearchGroup(matcher: StringMatchersGroup) = also {
        searchGroups = searchGroups ?: mutableListOf()
        searchGroups!!.add(matcher)
    }

    /**
     * add a string matchers group.
     * ----------------
     * 添加一个字符串匹配器分组。
     *
     * @param groupName group name / 分组名称
     * @param usingStrings using strings / 使用的字符串
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [BatchFindClassUsingStrings]
     */
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
        fun create() = BatchFindClassUsingStrings()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        searchGroups ?: throw IllegalAccessException("searchGroups not be empty")
        if (searchGroups!!.map { it.groupName }.toSet().size < searchGroups!!.size) {
            throw IllegalAccessException("groupName must be unique")
        }
        val root = InnerBatchFindClassUsingStrings.createBatchFindClassUsingStrings(
            fbb,
            searchPackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            excludePackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            ignorePackagesCase,
            searchClasses?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerBatchFindClassUsingStrings.createInClassesVector(fbb, it) } ?: 0,
            fbb.createVectorOfTables(searchGroups!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}