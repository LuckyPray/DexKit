@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindClass
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.result.ClassData

class FindClass : BaseQuery() {
    private var searchPackage: String? = null
    private var uniqueResult: Boolean = true
    private var searchClasses: LongArray? = null
    private var matcher: ClassMatcher? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun uniqueResult(uniqueResult: Boolean) = also {
        this.uniqueResult = uniqueResult
    }

    fun searchInClass(classList: List<ClassData>) = also {
        this.searchClasses = classList.map { getEncodeId(it.dexId, it.id) }.toLongArray()
    }

    fun matcher(matcher: ClassMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    fun FindClass.matcher(init: ClassMatcher.() -> Unit) = also {
        matcher(ClassMatcher().apply(init))
    }

    // endregion

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerFindClass.createFindClass(
            fbb,
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            uniqueResult,
            searchClasses?.let { InnerFindClass.createInClassesVector(fbb, it) } ?: 0,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}