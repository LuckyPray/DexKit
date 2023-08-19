@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindField
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.query.matchers.FieldMatcher

class FindField : BaseQuery() {
    private var searchPackage: String? = null
    private var uniqueResult: Boolean = true
    private var searchClasses: IntArray? = null
    private var searchFields: IntArray? = null
    private var matcher: FieldMatcher? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun uniqueResult(uniqueResult: Boolean) = also {
        this.uniqueResult = uniqueResult
    }

    fun searchInClass(classList: List<ClassData>) = also {
        this.searchClasses = classList.map { it.id }.toIntArray()
    }

    fun searchInField(fieldList: List<FieldData>) = also {
        this.searchFields = fieldList.map { it.id }.toIntArray()
    }

    fun matcher(matcher: FieldMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    fun FindField.matcher(init: FieldMatcher.() -> Unit) = also {
        matcher(FieldMatcher().apply(init))
    }

    // endregion

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerFindField.createFindField(
            fbb,
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            uniqueResult,
            searchClasses?.let { fbb.createVectorOfTables(it) } ?: 0,
            searchFields?.let { fbb.createVectorOfTables(it) } ?: 0,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
