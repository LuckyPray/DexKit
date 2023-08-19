@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindMethod
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.query.matchers.MethodMatcher

class FindMethod : BaseQuery() {
    private var searchPackage: String? = null
    private var uniqueResult: Boolean = true
    private var searchClasses: IntArray? = null
    private var searchMethods: IntArray? = null
    private var matcher: MethodMatcher? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun uniqueResult(uniqueResult: Boolean) = also {
        this.uniqueResult = uniqueResult
    }

    fun searchInClass(classList: List<ClassData>) = also {
        this.searchClasses = classList.map { it.id }.toIntArray()
    }

    fun searchInMethod(methodList: List<MethodData>) = also {
        this.searchMethods = methodList.map { it.id }.toIntArray()
    }
    fun matcher(matcher: MethodMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    fun FindMethod.matcher(init: MethodMatcher.() -> Unit) = also {
        matcher(MethodMatcher().apply(init))
    }

    // endregion

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerFindMethod.createFindMethod(
            fbb,
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            uniqueResult,
            searchClasses?.let { fbb.createVectorOfTables(it) } ?: 0,
            searchMethods?.let { fbb.createVectorOfTables(it) } ?: 0,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
