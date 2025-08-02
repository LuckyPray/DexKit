/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchFindMethodUsingStrings
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.StringMatchersGroup
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData

class BatchFindMethodUsingStrings : BaseFinder() {
    /**
     * Search methods in the specified packages.
     * ----------------
     * 在指定的包中搜索方法。
     */
    @set:JvmSynthetic
    var searchPackages: Collection<String>? = null

    /**
     * Exclude methods in the specified packages.
     * ----------------
     * 排除指定包中的方法。
     */
    @set:JvmSynthetic
    var excludePackages: Collection<String>? = null

    /**
     * Ignore case with [searchPackages] and [excludePackages].
     * ----------------
     * 忽略 [searchPackages] 和 [excludePackages] 的大小写。
     */
    @set:JvmSynthetic
    var ignorePackagesCase: Boolean = false

    /**
     * Searches the specified [ClassData] list for methods matching the criteria.
     * ----------------
     * 在指定的 [ClassData] 列表中搜索匹配符合条件的类。
     */
    @set:JvmSynthetic
    var searchClasses: Collection<ClassData>? = null

    /**
     * Searches the specified [MethodData] list for methods matching the criteria.
     * ----------------
     * 在指定的 [MethodData] 列表中搜索匹配符合条件的方法。
     */
    @set:JvmSynthetic
    var searchMethods: Collection<MethodData>? = null
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
     * @return [BatchFindMethodUsingStrings]
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
     * @return [BatchFindMethodUsingStrings]
     */
    fun searchPackages(searchPackages: Collection<String>) = also {
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
     * @return [BatchFindMethodUsingStrings]
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
     * @return [BatchFindMethodUsingStrings]
     */
    fun excludePackages(excludePackages: Collection<String>) = also {
        this.excludePackages = excludePackages
    }

    /**
     * Ignore case with [searchPackages] and [excludePackages].
     * ----------------
     * 忽略 [searchPackages] 和 [excludePackages] 的大小写。
     *
     * @param ignorePackagesCase ignore case / 忽略大小写
     * @return [BatchFindMethodUsingStrings]
     */
    fun ignorePackagesCase(ignorePackagesCase: Boolean) = also {
        this.ignorePackagesCase = ignorePackagesCase
    }

    /**
     * Search in the specified [ClassData] list.
     * ----------------
     * 在指定的 [ClassData] 列表中搜索指定类。
     *
     * @param classes search classes / 搜索类
     * @return [BatchFindMethodUsingStrings]
     */
    fun searchInClasses(classes: Collection<ClassData>) = also {
        this.searchClasses = classes
    }

    /**
     * Search in the specified [MethodData] list.
     * ----------------
     * 在指定的 [MethodData] 列表中搜索指定方法。
     *
     * @param methods search methods / 搜索方法
     * @return [BatchFindMethodUsingStrings]
     */
    fun searchInMethods(methods: Collection<MethodData>) = also {
        this.searchMethods = methods
    }

    /**
     * find all methods that use the specified strings.
     * ----------------
     * 查找所有使用指定字符串的方法。
     *
     * @param groups string matchers group / 字符串匹配器分组
     * @return [BatchFindMethodUsingStrings]
     */
    fun groups(groups: Collection<StringMatchersGroup>) = also {
        this.searchGroups = groups.toMutableList()
    }

    /**
     * find all methods that use the specified strings.
     * ----------------
     * 查找所有使用指定字符串的类。
     *
     *     val keywordsMap = mapOf(
     *         "Method1" to listOf("TAG", "TAG1", "TAG2"),
     *         "Method2" to listOf("Handler", "Handler1", "Handler2")
     *     )
     *     bridge.matchers(keywordsMap, StringMatchType.Equals, false)
     *
     * @param keywordsMap keywords group map / 关键字分组映射
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [BatchFindMethodUsingStrings]
     */
    @JvmOverloads
    fun groups(
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
     * @return [BatchFindMethodUsingStrings]
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
     * @return [BatchFindMethodUsingStrings]
     */
    @JvmOverloads
    fun addSearchGroup(
        groupName: String,
        usingStrings: Collection<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        addSearchGroup(StringMatchersGroup(groupName, usingStrings.map { StringMatcher(it, matchType, ignoreCase) }))
    }

    // region DSL

    @JvmSynthetic
    fun groups(init: StringMatchersGroupList.() -> Unit) = also {
        groups(StringMatchersGroupList().apply(init))
    }

    @JvmSynthetic
    fun addSearchGroup(init: StringMatchersGroup.() -> Unit) = also {
        addSearchGroup(StringMatchersGroup().apply(init))
    }

    @JvmSynthetic
    fun addSearchGroup(
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
            searchPackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            excludePackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            ignorePackagesCase,
            searchClasses?.map { it.getEncodeId() }?.toLongArray()
                ?.let { InnerBatchFindMethodUsingStrings.createInClassesVector(fbb, it) } ?: 0,
            searchMethods?.map { it.getEncodeId() }?.toLongArray()
                ?.let { InnerBatchFindMethodUsingStrings.createInMethodsVector(fbb, it) } ?: 0,
            fbb.createVectorOfTables(searchGroups!!.map { it.build(fbb) }.toIntArray())
        )
        fbb.finish(root)
        return root
    }
}