@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.RetentionPolicyType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.TargetElementType
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.query.matchers.base.TargetElementTypesMatcher

class AnnotationMatcher : BaseQuery() {
    private var typeName: StringMatcher? = null
    private var targetElementTypes: TargetElementTypesMatcher? = null
    private var policy: RetentionPolicyType? = null
    private var annotations: AnnotationsMatcher? = null
    private var elements: AnnotationElementsMatcher? = null

    fun typeName(typeName: StringMatcher) = also {
        this.typeName = typeName
    }

    @JvmOverloads
    fun typeName(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.typeName = StringMatcher(typeName, matchType, ignoreCase)
    }

    fun targetElementTypes(targetElementTypes: TargetElementTypesMatcher) = also {
        this.targetElementTypes = targetElementTypes
    }

    fun targetElementTypes(
        targetElementTypes: List<TargetElementType>,
        matchType: MatchType = MatchType.Equal
    ) = also {
        this.targetElementTypes = TargetElementTypesMatcher().apply {
            types(targetElementTypes)
            matchType(matchType)
        }
    }

    fun policy(policy: RetentionPolicyType) = also {
        this.policy = policy
    }

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotations = annotations
    }

    fun addAnnotation(annotation: AnnotationMatcher) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).add(annotation)
    }

    fun annotationMatchType(matchType: MatchType) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).matchType(matchType)
    }

    fun annotationCountRange(countRange: IntRange) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).countRange(countRange)
    }

    fun annotationCountRange(range: kotlin.ranges.IntRange) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).countRange(range)
    }

    fun annotationCount(count: Int) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).countRange(count)
    }

    fun annotationCountRange(min: Int, max: Int) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).countRange(min, max)
    }

    fun elements(elements: AnnotationElementsMatcher) = also {
        this.elements = elements
    }

    fun addElement(element: AnnotationElementMatcher) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).add(element)
    }

    fun elementMatchType(matchType: MatchType) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).matchType(matchType)
    }

    fun elementCountRange(countRange: IntRange) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).countRange(countRange)
    }

    fun elementCountRange(range: kotlin.ranges.IntRange) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).countRange(range)
    }

    fun elementCount(count: Int) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).countRange(count)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun targetElementTypes(init: TargetElementTypesMatcher.() -> Unit) = also {
        targetElementTypes(TargetElementTypesMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun elements(init: AnnotationElementsMatcher.() -> Unit) = also {
        elements(AnnotationElementsMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationMatcher()
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationMatcher.createAnnotationMatcher(
            fbb,
            typeName?.build(fbb) ?: 0,
            targetElementTypes?.build(fbb) ?: 0,
            policy?.value ?: 0,
            annotations?.build(fbb) ?: 0,
            elements?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}