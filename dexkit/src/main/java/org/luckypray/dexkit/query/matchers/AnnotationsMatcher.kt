@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationsMatcher : BaseQuery() {
    var annotationsMatcher: MutableList<AnnotationMatcher>? = null
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

    fun annotations(annotations: List<AnnotationMatcher>) = also {
        this.annotationsMatcher = annotations.toMutableList()
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

    fun add(annotation: AnnotationMatcher) = also {
        annotationsMatcher = annotationsMatcher ?: mutableListOf()
        annotationsMatcher!!.add(annotation)
    }

    fun add(typeName: String) = also {
        add(AnnotationMatcher().apply { type(typeName) })
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
            annotationsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0,
        )
        fbb.finish(root)
        return root;
    }
}