@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationEncodeValuesMatcher : BaseQuery() {
    var encodeValuesMatcher: List<AnnotationEncodeValueMatcher>? = null
        private set
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var rangeMatcher: IntRange? = null
        private set

    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            count(value)
        }

    fun matchers(matchers: List<AnnotationEncodeValueMatcher>) = also {
        this.encodeValuesMatcher = matchers
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    fun count(min: Int, max: Int) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun add(matcher: AnnotationEncodeValueMatcher) = also {
        encodeValuesMatcher = encodeValuesMatcher ?: mutableListOf()
        if (encodeValuesMatcher !is MutableList) {
            encodeValuesMatcher = encodeValuesMatcher!!.toMutableList()
        }
        (encodeValuesMatcher as MutableList<AnnotationEncodeValueMatcher>).add(matcher)
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
            encodeValuesMatcher?.map { it.type!!.value }?.let { InnerAnnotationEncodeArrayMatcher.createValuesTypeVector(fbb, it.toUByteArray()) } ?: 0,
            encodeValuesMatcher?.map { it.value!!.build(fbb) }?.let { InnerAnnotationEncodeArrayMatcher.createValuesVector(fbb, it.toIntArray()) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}