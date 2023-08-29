@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationEncodeValuesMatcher : BaseQuery() {
    var matchers: List<AnnotationEncodeValueMatcher>? = null
        private set
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var valueCount: IntRange? = null
        private set

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

    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        add(AnnotationEncodeValueMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementsMatcher()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationEncodeArrayMatcher.createAnnotationEncodeArrayMatcher(
            fbb,
            matchers?.map { it.type!!.value }?.let { InnerAnnotationEncodeArrayMatcher.createValuesTypeVector(fbb, it.toUByteArray()) } ?: 0,
            matchers?.map { it.value!!.build(fbb) }?.let { InnerAnnotationEncodeArrayMatcher.createValuesVector(fbb, it.toIntArray()) } ?: 0,
            matchType.value,
            valueCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}