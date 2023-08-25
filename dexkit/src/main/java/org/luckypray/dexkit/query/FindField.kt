@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFindField
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData

class FindField : BaseQuery() {
    private var searchPackage: String? = null
    private var searchClasses: LongArray? = null
    private var searchFields: LongArray? = null
    private var matcher: FieldMatcher? = null

    fun searchPackage(searchPackage: String) = also {
        this.searchPackage = searchPackage
    }

    fun searchInClass(classList: List<ClassData>) = also {
        this.searchClasses = classList.map { getEncodeId(it.dexId, it.id) }.toLongArray()
    }

    fun searchInField(fieldList: List<FieldData>) = also {
        this.searchFields = fieldList.map { getEncodeId(it.dexId, it.id) }.toLongArray()
    }

    fun matcher(matcher: FieldMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun FindField.matcher(init: FieldMatcher.() -> Unit) = also {
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
            searchPackage?.let { fbb.createString(searchPackage) } ?: 0,
            searchClasses?.let { InnerFindField.createInClassesVector(fbb, it) } ?: 0,
            searchFields?.let { InnerFindField.createInFieldsVector(fbb, it) } ?: 0,
            matcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}
