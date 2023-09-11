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
import org.luckypray.dexkit.query.matchers.base.TargetElementTypesMatcher

class AnnotationMatcher : BaseQuery() {
    var typeMatcher: ClassMatcher? = null
        private set
    var targetElementTypesMatcher: TargetElementTypesMatcher? = null
        private set
    @set:JvmSynthetic
    var policy: RetentionPolicyType? = null
    var elementsMatcher: AnnotationElementsMatcher? = null
        private set

    var type: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            type(value)
        }

    fun type(typeMatcher: ClassMatcher) = also {
        this.typeMatcher = typeMatcher
    }

    @JvmOverloads
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        typeMatcher = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    fun targetElementTypes(targetElementTypes: TargetElementTypesMatcher) = also {
        this.targetElementTypesMatcher = targetElementTypes
    }

    fun targetElementTypes(
        targetElementTypes: List<TargetElementType>,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.targetElementTypesMatcher = TargetElementTypesMatcher().apply {
            types(targetElementTypes)
            matchType(matchType)
        }
    }

    fun policy(policy: RetentionPolicyType) = also {
        this.policy = policy
    }

    fun elements(elements: AnnotationElementsMatcher) = also {
        this.elementsMatcher = elements
    }

    fun addElement(element: AnnotationElementMatcher) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).add(element)
    }

    fun elementMatchType(matchType: MatchType) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).matchType(matchType)
    }

    fun elementCount(count: Int) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(count)
    }

    fun elementCount(range: IntRange) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(range)
    }

    fun elementCount(range: kotlin.ranges.IntRange) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(range)
    }

    fun elementCount(min: Int, max: Int) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(min, max)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun type(init: ClassMatcher.() -> Unit) = also {
        type(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun targetElementTypes(init: TargetElementTypesMatcher.() -> Unit) = also {
        targetElementTypes(TargetElementTypesMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun elements(init: AnnotationElementsMatcher.() -> Unit) = also {
        elements(AnnotationElementsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addElement(init: AnnotationElementMatcher.() -> Unit) = also {
        addElement(AnnotationElementMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationMatcher()
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationMatcher.createAnnotationMatcher(
            fbb,
            typeMatcher?.build(fbb) ?: 0,
            targetElementTypesMatcher?.build(fbb) ?: 0,
            policy?.value ?: 0,
            elementsMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}