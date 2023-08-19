@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationElementMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher

class AnnotationElementMatcher : BaseQuery() {
    private var name: String? = null
    private var matcher: AnnotationEncodeValueMatcher? = null

    fun name(name: String) = also {
        this.name = name
    }

    fun matcher(matcher: AnnotationEncodeValueMatcher) = also {
        this.matcher = matcher
    }

    // region DSL

    fun matcher(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        matcher = AnnotationEncodeValueMatcher().apply(init)
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementMatcher.createAnnotationElementMatcher(
            fbb,
            name?.let { fbb.createString(it) } ?: 0,
            matcher?.type?.value ?: 0U,
            matcher?.value?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}