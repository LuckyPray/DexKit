@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerParameterMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType

class ParameterMatcher : BaseQuery() {
    // TODO
//    var annotations: AnnotationsMatcher? = null
    private var type: ClassMatcher? = null

    fun type(type: ClassMatcher) = also { this.type = type }
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.type = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    // region DSL

    fun type(init: ClassMatcher.() -> Unit) = also {
        type = ClassMatcher().apply(init)
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ParameterMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerParameterMatcher.createParameterMatcher(
            fbb,
            // TODO
            0,
            type?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}