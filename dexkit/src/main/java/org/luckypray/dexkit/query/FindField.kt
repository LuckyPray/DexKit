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
import org.luckypray.dexkit.InnerFindField
import org.luckypray.dexkit.query.base.BaseFinder
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData

class FindField : BaseFinder() {
    /**
     * Search field in the specified packages.
     * ----------------
     * 在指定的包中搜索字段。
     */
    @set:JvmSynthetic
    var searchPackages: Collection<String>? = null

    /**
     * Exclude field in the specified packages.
     * ----------------
     * 排除指定包中的字段。
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
     * Searches the specified [ClassData] list for fields matching the criteria.
     * ----------------
     * 在指定的 [ClassData] 列表中搜索匹配符合条件的字段。
     */
    @set:JvmSynthetic
    var searchClasses: Collection<ClassData>? = null

    /**
     * Searches the specified [FieldData] list for fields matching the criteria.
     * ----------------
     * 在指定的 [FieldData] 列表中搜索匹配符合条件的字段。
     */
    @set:JvmSynthetic
    var searchFields: Collection<FieldData>? = null

    /**
     * Terminates the search after finding the first matching field.
     * ----------------
     * 找到第一个匹配的字段后终止搜索。
     */
    @set:JvmSynthetic
    var findFirst: Boolean = false

    var matcher: FieldMatcher? = null
        private set

    /**
     * Search classes in the specified packages.
     * ----------------
     * 在指定的包中搜索类。
     *
     *     searchPackages("java.lang", "java.util")
     *
     * @param searchPackages search packages / 搜索包
     * @return [FindField]
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
     * @return [FindField]
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
     * @return [FindField]
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
     * @return [FindField]
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
     * @return [FindField]
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
     * @return [FindField]
     */
    fun searchInClass(classes: Collection<ClassData>) = also {
        this.searchClasses = classes
    }

    /**
     * Search in the specified [FieldData] list.
     * ----------------
     * 在指定的 [FieldData] 列表中搜索指定类。
     *
     * @param fields search fields / 字段列表
     * @return [FindField]
     */
    fun searchInField(fields: Collection<FieldData>) = also {
        this.searchFields = fields
    }

    /**
     * Build a [FieldMatcher] to match fields.
     * ----------------
     * 构建一个 [FieldMatcher] 用于匹配字段。
     *
     *     matcher(FieldMatcher.create().declaredClass("android.app.Activity"))
     *
     * @param matcher field matcher / 字段匹配器
     * @return [FindField]
     */
    fun matcher(matcher: FieldMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    /**
     * @see matcher
     */
    @JvmSynthetic
    fun matcher(init: FieldMatcher.() -> Unit) = also {
        matcher(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FindField()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFindField.createFindField(
            fbb,
            searchPackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            excludePackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            ignorePackagesCase,
            searchClasses?.map { it.getEncodeId() }?.toLongArray()
                ?.let { InnerFindField.createInClassesVector(fbb, it) } ?: 0,
            searchFields?.map { it.getEncodeId() }?.toLongArray()
                ?.let { InnerFindField.createInFieldsVector(fbb, it) } ?: 0,
            findFirst,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
