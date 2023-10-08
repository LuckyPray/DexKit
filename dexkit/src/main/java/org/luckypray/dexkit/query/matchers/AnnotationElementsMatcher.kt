@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationElementsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationElementsMatcher : BaseQuery() {
    var elementsMatcher: MutableList<AnnotationElementMatcher>? = null
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

    fun elements(elements: Collection<AnnotationElementMatcher>) = also {
        this.elementsMatcher = elements.toMutableList()
    }

    fun elements(vararg elements: AnnotationElementMatcher) = also {
        this.elementsMatcher = elements.toMutableList()
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

    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }
    
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    fun add(element: AnnotationElementMatcher) = also {
        elementsMatcher = elementsMatcher ?: mutableListOf()
        elementsMatcher!!.add(element)
    }

    @JvmOverloads
    fun add(name: String, matcher: AnnotationEncodeValueMatcher? = null) = also {
        add(AnnotationElementMatcher().apply {
            name(name)
            matcher?.let { this.value(it) }
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
            elementsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}