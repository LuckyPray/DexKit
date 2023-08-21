@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationMatcher
import org.luckypray.dexkit.InnerRetentionPolicyType
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.RetentionPolicyType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.TargetElementType
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

    fun elements(elements: AnnotationElementsMatcher) = also {
        this.elements = elements
    }

    // region DSL

    fun targetElementTypes(init: TargetElementTypesMatcher.() -> Unit) = also {
        targetElementTypes(TargetElementTypesMatcher().apply(init))
    }

    fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    fun elements(init: AnnotationElementsMatcher.() -> Unit) = also {
        elements(AnnotationElementsMatcher().apply(init))
    }

    // endregion

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
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