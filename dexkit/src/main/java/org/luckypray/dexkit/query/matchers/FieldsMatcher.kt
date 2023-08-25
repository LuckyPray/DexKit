@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFieldsMatcher
import org.luckypray.dexkit.query.FieldMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class FieldsMatcher : BaseQuery() {
    private var fields: List<FieldMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var countRange: IntRange? = null

    fun fields(fields: List<FieldMatcher>) = also {
        this.fields = fields
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun countRange(countRange: IntRange) = also {
        this.countRange = countRange
    }

    fun countRange(range: kotlin.ranges.IntRange) = also {
        countRange = IntRange(range)
    }

    fun countRange(count: Int) = also {
        this.countRange = IntRange(count)
    }

    fun countRange(min: Int, max: Int) = also {
        this.countRange = IntRange(min, max)
    }

    fun add(matcher: FieldMatcher) = also {
        fields = fields ?: FieldMatcherList()
        if (fields !is FieldMatcherList) {
            fields = FieldMatcherList(fields!!)
        }
        (fields as FieldMatcherList).add(matcher)
    }

    fun addForName(name: String) = also {
        add(FieldMatcher().apply { name(name) })
    }

    @JvmOverloads
    fun addForType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        add(FieldMatcher().apply { type(typeName, matchType, ignoreCase) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun add(init: FieldMatcher.() -> Unit) = also {
        add(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldsMatcher.createFieldsMatcher(
            fbb,
            fields?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}