@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationElementMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class AnnotationElementMatcher : BaseQuery() {
    private var name: StringMatcher? = null
    private var matcher: AnnotationEncodeValueMatcher? = null

    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.name = StringMatcher(name, StringMatchType.Equals, ignoreCase)
    }

    fun matcher(matcher: AnnotationEncodeValueMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun matcher(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        matcher(AnnotationEncodeValueMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementMatcher.createAnnotationElementMatcher(
            fbb,
            name?.build(fbb) ?: 0,
            matcher?.type?.value ?: 0U,
            matcher?.value?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}