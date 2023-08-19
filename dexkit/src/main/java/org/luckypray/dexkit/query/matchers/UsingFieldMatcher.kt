package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerUsingFieldMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.UsingType

class UsingFieldMatcher @JvmOverloads constructor(
    private val matcher: FieldMatcher? = null,
    private val usingType: UsingType = UsingType.Any
) : BaseQuery() {

    fun matcher(matcher: FieldMatcher) = also {
        this.matcher
    }

    fun usingType(usingType: UsingType) = also {
        this.usingType
    }

    // region DSL

    fun matcher(init: FieldMatcher.() -> Unit) = also {
        matcher(FieldMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            matcher: FieldMatcher? = null,
            usingType: UsingType = UsingType.Any
        ): UsingFieldMatcher {
            return UsingFieldMatcher(matcher, usingType)
        }
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        if (matcher == null) throw IllegalArgumentException("UsingFieldMatcher matcher not set")
        val root = InnerUsingFieldMatcher.createUsingFieldMatcher(
            fbb,
            matcher.build(fbb),
            usingType.value
        )
        fbb.finish(root)
        return root
    }
}