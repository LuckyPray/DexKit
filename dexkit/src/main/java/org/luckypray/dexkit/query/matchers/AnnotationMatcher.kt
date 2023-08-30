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
    var typeNameMatcher: StringMatcher? = null
        private set
    var targetElementTypes: TargetElementTypesMatcher? = null
        private set
    @set:JvmSynthetic
    var policy: RetentionPolicyType? = null
    var annotations: AnnotationsMatcher? = null
        private set
    var elements: AnnotationElementsMatcher? = null
        private set
    // TODO: methods / fields

    var typeName: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            typeNameMatcher = typeNameMatcher ?: StringMatcher(value)
            typeNameMatcher!!.value = value
        }

    fun typeName(typeName: StringMatcher) = also {
        this.typeNameMatcher = typeName
    }

    @JvmOverloads
    fun typeName(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.typeNameMatcher = StringMatcher(typeName, matchType, ignoreCase)
    }

    fun targetElementTypes(targetElementTypes: TargetElementTypesMatcher) = also {
        this.targetElementTypes = targetElementTypes
    }

    fun targetElementTypes(
        targetElementTypes: List<TargetElementType>,
        matchType: MatchType = MatchType.Contains
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
        (annotations as AnnotationsMatcher).range(countRange)
    }

    fun annotationCountRange(range: kotlin.ranges.IntRange) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).range(range)
    }

    fun annotationCount(count: Int) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).count(count)
    }

    fun annotationCountRange(min: Int, max: Int) = also {
        annotations = annotations ?: AnnotationsMatcher()
        (annotations as AnnotationsMatcher).range(min, max)
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
        (elements as AnnotationElementsMatcher).range(countRange)
    }

    fun elementCountRange(range: kotlin.ranges.IntRange) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).range(range)
    }

    fun elementCount(count: Int) = also {
        elements = elements ?: AnnotationElementsMatcher()
        (elements as AnnotationElementsMatcher).count(count)
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
            typeNameMatcher?.build(fbb) ?: 0,
            targetElementTypes?.build(fbb) ?: 0,
            policy?.value ?: 0,
            annotations?.build(fbb) ?: 0,
            elements?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}