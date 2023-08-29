@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindClass
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.result.ClassData

class FindClass : BaseQuery() {
    private var searchPackages: List<String>? = null
    private var excludePackages: List<String>? = null
    @set:JvmSynthetic
    var ignorePackagesCase: Boolean = false
    private var searchClasses: LongArray? = null
    private var matcher: ClassMatcher? = null

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
        this.searchClasses = classList.map { getEncodeId(it.dexId, it.id) }.toLongArray()
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
            searchPackages?.map { fbb.createString(it) }?.let { fbb.createVectorOfTables(it.toIntArray()) } ?: 0,
            excludePackages?.map { fbb.createString(it) }?.let { fbb.createVectorOfTables(it.toIntArray()) } ?: 0,
            ignorePackagesCase,
            searchClasses?.let { InnerFindClass.createInClassesVector(fbb, it) } ?: 0,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}