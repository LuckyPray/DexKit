@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerFieldsMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class FieldsMatcher : BaseQuery() {
    private var values: List<FieldMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var fieldCount: IntRange? = null

    fun fields(fields: List<FieldMatcher>) = also {
        this.values = fields
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun fieldCount(fieldCount: IntRange) = also {
        this.fieldCount = fieldCount
    }

    fun fieldCount(count: Int) = also {
        this.fieldCount = IntRange(count)
    }

    fun fieldCount(min: Int, max: Int) = also {
        this.fieldCount = IntRange(min, max)
    }

    fun addMatcher(matcher: FieldMatcher) {
        values = values ?: mutableListOf()
        if (values !is MutableList) {
            values = values!!.toMutableList()
        }
        (values as MutableList<FieldMatcher>).add(matcher)
    }

    fun addForName(name: String) = also {
        addMatcher(FieldMatcher().apply { name(name) })
    }

    fun addForType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        addMatcher(FieldMatcher().apply { type(typeName, matchType, ignoreCase) })
    }

    // region DSL

    fun addMatcher(init: FieldMatcher.() -> Unit) = also {
        addMatcher(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldsMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldsMatcher.createFieldsMatcher(
            fbb,
            values?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            fieldCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}