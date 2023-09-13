@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers.base

import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.IQuery
import org.luckypray.dexkit.query.enums.AnnotationEncodeValueType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.AnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.matchers.AnnotationMatcher
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.EncodeValueBoolean
import org.luckypray.dexkit.query.matchers.EncodeValueByte
import org.luckypray.dexkit.query.matchers.EncodeValueChar
import org.luckypray.dexkit.query.matchers.EncodeValueDouble
import org.luckypray.dexkit.query.matchers.EncodeValueFloat
import org.luckypray.dexkit.query.matchers.EncodeValueInt
import org.luckypray.dexkit.query.matchers.EncodeValueLong
import org.luckypray.dexkit.query.matchers.EncodeValueShort
import org.luckypray.dexkit.query.matchers.FieldMatcher

class AnnotationEncodeValueMatcher : IQuery {
    @JvmSynthetic
    internal var value: BaseQuery? = null
        private set
    @JvmSynthetic
    internal var type: AnnotationEncodeValueType? = null
        private set

    constructor()

    private constructor(value: BaseQuery, type: AnnotationEncodeValueType) {
        this.value = value
        this.type = type
    }

    fun byteValue(value: Byte) = also {
        this.value = EncodeValueByte(value)
        this.type = AnnotationEncodeValueType.ByteValue
    }

    fun shortValue(value: Short) = also {
        this.value = EncodeValueShort(value)
        this.type = AnnotationEncodeValueType.ShortValue
    }

    fun charValue(value: Char) = also {
        this.value = EncodeValueChar(value)
        this.type = AnnotationEncodeValueType.CharValue
    }

    fun intValue(value: Int) = also {
        this.value = EncodeValueInt(value)
        this.type = AnnotationEncodeValueType.IntValue
    }

    fun longValue(value: Long) = also {
        this.value = EncodeValueLong(value)
        this.type = AnnotationEncodeValueType.LongValue
    }

    fun floatValue(value: Float) = also {
        this.value = EncodeValueFloat(value)
        this.type = AnnotationEncodeValueType.FloatValue
    }

    fun doubleValue(value: Double) = also {
        this.value = EncodeValueDouble(value)
        this.type = AnnotationEncodeValueType.DoubleValue
    }

    fun stringValue(value: StringMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.StringValue
    }

    @JvmOverloads
    fun stringValue(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.value = StringMatcher(value, matchType, ignoreCase)
        this.type = AnnotationEncodeValueType.StringValue
    }

    fun classValue(value: ClassMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.TypeValue
    }

    fun enumValue(value: FieldMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.EnumValue
    }

    fun arrayValue(value: AnnotationEncodeArrayMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.ArrayValue
    }

    fun annotationValue(value: AnnotationMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.AnnotationValue
    }

    fun boolValue(value: Boolean) = also {
        this.value = EncodeValueBoolean(value)
        this.type = AnnotationEncodeValueType.BoolValue
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun classValue(init: ClassMatcher.() -> Unit) = also {
        classValue(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun enumValue(init: FieldMatcher.() -> Unit) = also {
        enumValue(FieldMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun arrayValue(init: AnnotationEncodeArrayMatcher.() -> Unit) = also {
        arrayValue(AnnotationEncodeArrayMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun annotationValue(init: AnnotationMatcher.() -> Unit) = also {
        annotationValue(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun createByte(value: Byte): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ByteValue
            return AnnotationEncodeValueMatcher(EncodeValueByte(value), type)
        }

        @JvmStatic
        fun createShort(value: Short): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ShortValue
            return AnnotationEncodeValueMatcher(EncodeValueShort(value), type)
        }

        @JvmStatic
        fun createChar(value: Char): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.CharValue
            return AnnotationEncodeValueMatcher(EncodeValueChar(value), type)
        }

        @JvmStatic
        fun createInt(value: Int): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.IntValue
            return AnnotationEncodeValueMatcher(EncodeValueInt(value), type)
        }

        @JvmStatic
        fun createLong(value: Long): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.LongValue
            return AnnotationEncodeValueMatcher(EncodeValueLong(value), type)
        }

        @JvmStatic
        fun createFloat(value: Float): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.FloatValue
            return AnnotationEncodeValueMatcher(EncodeValueFloat(value), type)
        }

        @JvmStatic
        fun createDouble(value: Double): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.DoubleValue
            return AnnotationEncodeValueMatcher(EncodeValueDouble(value), type)
        }

        @JvmStatic
        fun createString(value: StringMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.StringValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        @JvmStatic
        @JvmOverloads
        fun createString(
            value: String,
            matchType: StringMatchType = StringMatchType.Contains,
            ignoreCase: Boolean = false
        ): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.StringValue
            return AnnotationEncodeValueMatcher(StringMatcher(value, matchType, ignoreCase), type)
        }

        @JvmStatic
        fun createClass(value: ClassMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.TypeValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        @JvmStatic
        fun createEnum(value: FieldMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.EnumValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        @JvmStatic
        fun createArray(value: AnnotationEncodeArrayMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ArrayValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        @JvmStatic
        fun createAnnotation(value: AnnotationMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.AnnotationValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        @JvmStatic
        fun createBoolean(value: Boolean): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.BoolValue
            return AnnotationEncodeValueMatcher(EncodeValueBoolean(value), type)
        }
    }
}