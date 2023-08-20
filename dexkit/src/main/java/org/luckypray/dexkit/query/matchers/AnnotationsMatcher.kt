@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationsMatcher : BaseQuery() {
    private var annotations: List<AnnotationMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var annotationCount: IntRange? = null

    fun annotations(annotations: List<AnnotationMatcher>) = also {
        this.annotations = annotations
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun annotationCount(annotationCount: IntRange) = also {
        this.annotationCount = annotationCount
    }

    fun annotationCount(count: Int) = also {
        this.annotationCount = IntRange(count)
    }

    fun annotationCount(min: Int, max: Int) = also {
        this.annotationCount = IntRange(min, max)
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

    fun add(init: AnnotationMatcher.() -> Unit) = also {
        add(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationEncodeArrayMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationsMatcher.createAnnotationsMatcher(
            fbb,
            annotations?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            annotationCount?.build(fbb) ?: 0,
        )
        fbb.finish(root)
        return root;
    }
}