@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindField
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData

class FindField : BaseQuery() {
    /**
     * Search field in the specified packages.
     * ----------------
     * 在指定的包中搜索字段。
     */
    @set:JvmSynthetic
    var searchPackages: List<String>? = null

    /**
     * Exclude field in the specified packages.
     * ----------------
     * 排除指定包中的字段。
     */
    @set:JvmSynthetic
    var excludePackages: List<String>? = null

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
    var searchClasses: List<ClassData>? = null

    /**
     * Searches the specified [FieldData] list for fields matching the criteria.
     * ----------------
     * 在指定的 [FieldData] 列表中搜索匹配符合条件的字段。
     */
    @set:JvmSynthetic
    var searchFields: List<FieldData>? = null

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
    fun excludePackages(excludePackages: List<String>) = also {
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
    fun searchInClass(classes: List<ClassData>) = also {
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
    fun searchInField(fields: List<FieldData>) = also {
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
    @kotlin.internal.InlineOnly
    inline fun matcher(init: FieldMatcher.() -> Unit) = also {
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
            searchClasses?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerFindField.createInClassesVector(fbb, it) } ?: 0,
            searchFields?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerFindField.createInFieldsVector(fbb, it) } ?: 0,
            findFirst,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
