@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationElementsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationElementsMatcher : BaseQuery() {
    var elements: List<AnnotationElementMatcher>? = null
        private set
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var countRange: IntRange? = null
        private set

    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            countRange = IntRange(value)
        }
    var range: kotlin.ranges.IntRange
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            countRange = IntRange(value)
        }

    fun elements(elements: List<AnnotationElementMatcher>) = also {
        this.elements = elements
    }

    fun elements(vararg elements: AnnotationElementMatcher) = also {
        this.elements = elements.toList()
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

    fun add(element: AnnotationElementMatcher) = also {
        elements = elements ?: mutableListOf()
        if (elements !is MutableList) {
            elements = elements!!.toMutableList()
        }
        (elements as MutableList<AnnotationElementMatcher>).add(element)
    }

    @JvmOverloads
    fun add(name: String, matcher: AnnotationEncodeValueMatcher? = null) = also {
        add(AnnotationElementMatcher().apply {
            this.name(name)
            matcher?.let { this.matcher(it) }
        })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationElementMatcher.() -> Unit) = also {
        add(AnnotationElementMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementsMatcher.createAnnotationElementsMatcher(
            fbb,
            elements?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}