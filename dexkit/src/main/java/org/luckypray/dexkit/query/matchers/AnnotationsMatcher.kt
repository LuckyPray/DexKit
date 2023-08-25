@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationsMatcher : BaseQuery() {
    private var annotations: List<AnnotationMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var countRange: IntRange? = null

    fun annotations(annotations: List<AnnotationMatcher>) = also {
        this.annotations = annotations
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

    fun add(annotation: AnnotationMatcher) = also {
        annotations = annotations ?: mutableListOf()
        if (annotations !is MutableList) {
            annotations = annotations!!.toMutableList()
        }
        (annotations as MutableList<AnnotationMatcher>).add(annotation)
    }

    fun add(typeName: String) = also {
        add(AnnotationMatcher().apply { typeName(typeName) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationMatcher.() -> Unit) = also {
        add(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationsMatcher.createAnnotationsMatcher(
            fbb,
            annotations?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            countRange?.build(fbb) ?: 0,
        )
        fbb.finish(root)
        return root;
    }
}