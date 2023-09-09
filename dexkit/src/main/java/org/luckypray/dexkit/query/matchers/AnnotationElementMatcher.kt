@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationElementMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class AnnotationElementMatcher : BaseQuery() {
    var nameMatcher: StringMatcher? = null
        private set
    var valueMatcher: AnnotationEncodeValueMatcher? = null
        private set

    var name: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            name(value)
        }
    var value: AnnotationEncodeValueMatcher
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            value(value)
        }

    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.nameMatcher = StringMatcher(name, StringMatchType.Equals, ignoreCase)
    }

    fun value(matcher: AnnotationEncodeValueMatcher) = also {
        this.valueMatcher = matcher
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun value(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        value(AnnotationEncodeValueMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementMatcher.createAnnotationElementMatcher(
            fbb,
            nameMatcher?.build(fbb) ?: 0,
            valueMatcher?.type?.value ?: 0U,
            valueMatcher?.value?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}