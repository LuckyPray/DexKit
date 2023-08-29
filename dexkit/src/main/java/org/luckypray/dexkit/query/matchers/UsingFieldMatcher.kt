@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerUsingFieldMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.UsingType

class UsingFieldMatcher : BaseQuery() {
    var matcher: FieldMatcher? = null
        private set
    @set:JvmSynthetic
    var usingType: UsingType = UsingType.Any

    fun matcher(matcher: FieldMatcher) = also {
        this.matcher = matcher
    }

    fun usingType(usingType: UsingType) = also {
        this.usingType = usingType
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun matcher(init: FieldMatcher.() -> Unit) = also {
        matcher(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = UsingFieldMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (matcher == null) throw IllegalArgumentException("UsingFieldMatcher matcher not set")
        val root = InnerUsingFieldMatcher.createUsingFieldMatcher(
            fbb,
            matcher!!.build(fbb),
            usingType.value
        )
        fbb.finish(root)
        return root
    }
}