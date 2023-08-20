@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationEncodeValuesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationEncodeValuesMatcher : BaseQuery() {
    private var matchers: List<AnnotationEncodeValueMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var valueCount: IntRange? = null

    fun matchers(matchers: List<AnnotationEncodeValueMatcher>) = also {
        this.matchers = matchers
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun valueCount(valueCount: IntRange?) = also {
        this.valueCount = valueCount
    }

    fun valueCount(count: Int) = also {
        this.valueCount = IntRange(count)
    }

    fun valueCount(min: Int, max: Int) = also {
        this.valueCount = IntRange(min, max)
    }

    fun add(matcher: AnnotationEncodeValueMatcher) = also {
        matchers = matchers ?: mutableListOf()
        if (matchers !is MutableList) {
            matchers = matchers!!.toMutableList()
        }
        (matchers as MutableList<AnnotationEncodeValueMatcher>).add(matcher)
    }

    // region DSL

    fun add(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        add(AnnotationEncodeValueMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationEncodeArrayMatcher()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationEncodeValuesMatcher.createAnnotationEncodeValuesMatcher(
            fbb,
            matchers?.map { it.type!!.value }?.let { InnerAnnotationEncodeValuesMatcher.createValuesTypeVector(fbb, it.toUByteArray()) } ?: 0,
            matchers?.map { it.value!!.build(fbb) }?.let { InnerAnnotationEncodeValuesMatcher.createValuesVector(fbb, it.toIntArray()) } ?: 0,
            matchType.value,
            valueCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}