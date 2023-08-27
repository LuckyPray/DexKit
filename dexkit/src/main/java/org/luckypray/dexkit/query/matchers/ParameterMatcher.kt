@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerParameterMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType

class ParameterMatcher : BaseQuery() {
    private var annotations: AnnotationsMatcher? = null
    private var type: ClassMatcher? = null

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotations = annotations
    }

    fun type(type: ClassMatcher) = also { this.type = type }
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.type = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun type(init: ClassMatcher.() -> Unit) = also {
        type(ClassMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ParameterMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerParameterMatcher.createParameterMatcher(
            fbb,
            annotations?.build(fbb) ?: 0,
            type?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}