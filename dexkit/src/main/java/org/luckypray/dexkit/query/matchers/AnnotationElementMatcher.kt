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

    fun valueNumber(value: Number) = also {
        value(AnnotationEncodeValueMatcher().apply {
            when (value) {
                is Byte -> byteValue(value)
                is Short -> shortValue(value)
                is Int -> intValue(value)
                is Long -> longValue(value)
                is Float -> floatValue(value)
                is Double -> doubleValue(value)
            }
        })
    }

    fun valueByte(value: Byte) = also {
        value(AnnotationEncodeValueMatcher().apply { byteValue(value) })
    }

    fun valueShort(value: Short) = also {
        value(AnnotationEncodeValueMatcher().apply { shortValue(value) })
    }

    fun valueInt(value: Int) = also {
        value(AnnotationEncodeValueMatcher().apply { intValue(value) })
    }

    fun valueLong(value: Long) = also {
        value(AnnotationEncodeValueMatcher().apply { longValue(value) })
    }

    fun valueFloat(value: Float) = also {
        value(AnnotationEncodeValueMatcher().apply { floatValue(value) })
    }

    fun valueDouble(value: Double) = also {
        value(AnnotationEncodeValueMatcher().apply { doubleValue(value) })
    }

    fun valueString(value: StringMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { stringValue(value) })
    }

    @JvmOverloads
    fun valueString(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        value(AnnotationEncodeValueMatcher().apply { stringValue(value, matchType, ignoreCase) })
    }

    fun valueClass(value: ClassMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { classValue(value) })
    }

    @JvmOverloads
    fun valueClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        value(AnnotationEncodeValueMatcher().apply {
            classValue { className(className, matchType, ignoreCase) }
        })
    }

    fun valueEnum(value: FieldMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { enumValue(value) })
    }

    fun valueArray(value: AnnotationEncodeValuesMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { arrayValue(value) })
    }

    fun valueAnnotation(value: AnnotationMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { annotationValue(value) })
    }

    fun valueBool(value: Boolean) = also {
        value(AnnotationEncodeValueMatcher().apply { boolValue(value) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun value(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        value(AnnotationEncodeValueMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun valueClass(init: ClassMatcher.() -> Unit) = also {
        valueClass(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun valueEnum(init: FieldMatcher.() -> Unit) = also {
        valueEnum(FieldMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun valueArray(init: AnnotationEncodeValuesMatcher.() -> Unit) = also {
        valueArray(AnnotationEncodeValuesMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun valueAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        valueAnnotation(AnnotationMatcher().apply(init))
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