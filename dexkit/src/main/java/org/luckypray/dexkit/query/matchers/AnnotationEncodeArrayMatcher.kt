@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerAnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationEncodeArrayMatcher : BaseQuery() {
    private var elements: List<AnnotationElementMatcher>? = null
    private var matchType: MatchType = MatchType.Equal
    private var elementCount: IntRange? = null

    fun elements(elements: List<AnnotationElementMatcher>) = also {
        this.elements = elements
    }

    fun elements(vararg elements: AnnotationElementMatcher) = also {
        this.elements = elements.toList()
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun elementCount(elementCount: IntRange?) = also {
        this.elementCount = elementCount
    }

    fun elementCount(count: Int) = also {
        this.elementCount = IntRange(count)
    }

    fun elementCount(min: Int, max: Int) = also {
        this.elementCount = IntRange(min, max)
    }

    fun add(element: AnnotationElementMatcher) = also {
        elements = elements ?: mutableListOf()
        if (elements !is MutableList) {
            elements = elements!!.toMutableList()
        }
        (elements as MutableList<AnnotationElementMatcher>).add(element)
    }

    // region DSL

    fun add(init: AnnotationElementMatcher.() -> Unit) = also {
        add(AnnotationElementMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationEncodeArrayMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationEncodeArrayMatcher.createAnnotationEncodeArrayMatcher(
            fbb,
            elements?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            elementCount?.build(fbb) ?: 0
        )
        return 0
    }
}