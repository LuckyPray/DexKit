@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindMethod
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData

class FindMethod : BaseQuery() {
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
     * 在指定的 [ClassData] 列表中搜索匹配符合条件的方法。
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

    /**
     * Terminates the search after finding the first matching method.
     * ----------------
     * 找到第一个匹配的方法后终止搜索。
     */
    @set:JvmSynthetic
    var findFirst: Boolean = false
    var matcher: MethodMatcher? = null
        private set

    /**
     * Search classes in the specified packages.
     * ----------------
     * 在指定的包中搜索类。
     *
     *     searchPackages("java.lang", "java.util")
     *
     * @param searchPackages search packages / 搜索包
     * @return [FindMethod]
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
     * @return [FindMethod]
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
     * @return [FindMethod]
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
     * @return [FindMethod]
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
     * @return [FindMethod]
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
     * @return [FindMethod]
     */
    fun searchInClass(classes: Collection<ClassData>) = also {
        this.searchClasses = classes
    }

    /**
     * Search in the specified [MethodData] list.
     * ----------------
     * 在指定的 [MethodData] 列表中搜索指定方法。
     *
     * @param methods search methods / 方法列表
     * @return [FindMethod]
     */
    fun searchInMethod(methods: Collection<MethodData>) = also {
        this.searchMethods = methods
    }

    /**
     * Build a [MethodMatcher] to match methods.
     * ----------------
     * 构建一个 [MethodMatcher] 来匹配方法。
     *
     *     matcher(MethodMatcher.create().declaredClass("android.app.Activity"))
     *
     * @param matcher [MethodMatcher]
     * @return [FindMethod]
     */
    fun matcher(matcher: MethodMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    /**
     * @see matcher
     */
    @kotlin.internal.InlineOnly
    inline fun matcher(init: MethodMatcher.() -> Unit) = also {
        matcher(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FindMethod()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFindMethod.createFindMethod(
            fbb,
            searchPackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            excludePackages?.map { fbb.createString(it) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            ignorePackagesCase,
            searchClasses?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerFindMethod.createInClassesVector(fbb, it) } ?: 0,
            searchMethods?.map { getEncodeId(it.dexId, it.id) }?.toLongArray()
                ?.let { InnerFindMethod.createInMethodsVector(fbb, it) } ?: 0,
            findFirst,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
