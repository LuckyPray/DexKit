@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindClass
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.result.ClassData

class FindClass : BaseQuery() {
    @set:JvmSynthetic
    var searchPackages: List<String>? = null
    @set:JvmSynthetic
    var excludePackages: List<String>? = null
    @set:JvmSynthetic
    var ignorePackagesCase: Boolean = false
    @set:JvmSynthetic
    var searchClasses: List<ClassData>? = null
    @set:JvmSynthetic
    var findFirst: Boolean = false
    var matcher: ClassMatcher? = null
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

    fun searchInClass(classList: List<ClassData>) = also {
        this.searchClasses = classList
    }

    fun matcher(matcher: ClassMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun FindClass.matcher(init: ClassMatcher.() -> Unit) = also {
        matcher(ClassMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FindClass()
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFindClass.createFindClass(
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
                ?.let { InnerFindClass.createInClassesVector(fbb, it) } ?: 0,
            findFirst,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}