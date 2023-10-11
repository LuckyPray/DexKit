@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers.base

import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
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
import org.luckypray.dexkit.query.matchers.EncodeValueNull
import org.luckypray.dexkit.query.matchers.EncodeValueShort
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher

class AnnotationEncodeValueMatcher : IQuery {
    var value: IAnnotationEncodeValue? = null
        private set
    var type: AnnotationEncodeValueType? = null
        private set

    constructor()

    private constructor(value: IAnnotationEncodeValue, type: AnnotationEncodeValueType) {
        this.value = value
        this.type = type
    }

    /**
     * Create numberValue matcher.
     * ----------------
     * 创建 numberValue 匹配器。
     *
     * @param number number / 数字
     * @return [AnnotationEncodeValueMatcher]
     */
    fun numberValue(number: Number) = also {
        when (number) {
            is Byte -> byteValue(number)
            is Short -> shortValue(number)
            is Int -> intValue(number)
            is Long -> longValue(number)
            is Float -> floatValue(number)
            is Double -> doubleValue(number)
        }
    }

    /**
     * Create byteValue matcher.
     * ----------------
     * 创建 byteValue 匹配器。
     *
     * @param value byte value / 字节值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun byteValue(value: Byte) = also {
        this.value = EncodeValueByte(value)
        this.type = AnnotationEncodeValueType.ByteValue
    }

    /**
     * Create shortValue matcher.
     * ----------------
     * 创建 shortValue 匹配器。
     *
     * @param value short value / 短整型值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun shortValue(value: Short) = also {
        this.value = EncodeValueShort(value)
        this.type = AnnotationEncodeValueType.ShortValue
    }

    /**
     * Create charValue matcher.
     * ----------------
     * 创建 charValue 匹配器。
     *
     * @param value char value / 字符值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun charValue(value: Char) = also {
        this.value = EncodeValueChar(value)
        this.type = AnnotationEncodeValueType.CharValue
    }

    /**
     * Create intValue matcher.
     * ----------------
     * 创建 intValue 匹配器。
     *
     * @param value int value / 整型值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun intValue(value: Int) = also {
        this.value = EncodeValueInt(value)
        this.type = AnnotationEncodeValueType.IntValue
    }

    /**
     * Create longValue matcher.
     * ----------------
     * 创建 longValue 匹配器。
     *
     * @param value long value / 长整型值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun longValue(value: Long) = also {
        this.value = EncodeValueLong(value)
        this.type = AnnotationEncodeValueType.LongValue
    }

    /**
     * Create floatValue matcher. Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 创建 floatValue 匹配器。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value float value / 单精度浮点型值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun floatValue(value: Float) = also {
        this.value = EncodeValueFloat(value)
        this.type = AnnotationEncodeValueType.FloatValue
    }

    /**
     * Create doubleValue matcher. Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 创建 doubleValue 匹配器。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value double value / 双精度浮点型值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun doubleValue(value: Double) = also {
        this.value = EncodeValueDouble(value)
        this.type = AnnotationEncodeValueType.DoubleValue
    }

    /**
     * Create stringValue matcher.
     * ----------------
     * 创建 stringValue 匹配器。
     *
     * @param value string matcher / 字符串匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun stringValue(value: StringMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.StringValue
    }

    /**
     * Create stringValue matcher.
     * ----------------
     * 创建 stringValue 匹配器。
     *
     * @param value string / 字符串
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationEncodeValueMatcher]
     */
    @JvmOverloads
    fun stringValue(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.value = StringMatcher(value, matchType, ignoreCase)
        this.type = AnnotationEncodeValueType.StringValue
    }

    /**
     * Create classValue matcher.
     * ----------------
     * 创建 classValue 匹配器。
     *
     * @param value class matcher / 类匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun classValue(value: ClassMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.TypeValue
    }

    /**
     * Create methodValue matcher.
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 创建 methodValue 匹配器。
     * 注意：只有 dalvik.system 类型的注解才包含这个元素。
     *
     * @param value method matcher / 方法匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun methodValue(value: MethodMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.MethodValue
    }

    /**
     * Create enumValue matcher. The value of enum is a Field, so here use FieldMatcher.
     * ----------------
     * 创建 enumValue 匹配器。enum 的值是一个 Field，所以这里使用 FieldMatcher。
     *
     * @param value enum matcher / 枚举匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun enumValue(value: FieldMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.EnumValue
    }

    /**
     * Create arrayValue matcher.
     * ----------------
     * 创建 arrayValue 匹配器。
     *
     * @param value array matcher / 数组匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun arrayValue(value: AnnotationEncodeArrayMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.ArrayValue
    }

    /**
     * Create annotationValue matcher.
     * ----------------
     * 创建 annotationValue 匹配器。
     *
     * @param value annotation matcher / 注解匹配器
     * @return [AnnotationEncodeValueMatcher]
     */
    fun annotationValue(value: AnnotationMatcher) = also {
        this.value = value
        this.type = AnnotationEncodeValueType.AnnotationValue
    }

    /**
     * Create nullValue matcher.
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 创建 nullValue 匹配器。
     * 注意：只有 dalvik.system 类型的注解才包含这个元素。
     *
     * @return [AnnotationEncodeValueMatcher]
     */
    fun nullValue() = also {
        this.value = EncodeValueNull()
        this.type = AnnotationEncodeValueType.NullValue
    }

    /**
     * Create boolValue matcher.
     * ----------------
     * 创建 boolValue 匹配器。
     *
     * @param value bool value / 布尔值
     * @return [AnnotationEncodeValueMatcher]
     */
    fun boolValue(value: Boolean) = also {
        this.value = EncodeValueBoolean(value)
        this.type = AnnotationEncodeValueType.BoolValue
    }

    // region DSL

    /**
     * @see classValue
     */
    @kotlin.internal.InlineOnly
    inline fun classValue(init: ClassMatcher.() -> Unit) = also {
        classValue(ClassMatcher().apply(init))
    }

    /**
     * @see methodValue
     */
    @kotlin.internal.InlineOnly
    inline fun methodValue(init: MethodMatcher.() -> Unit) = also {
        methodValue(MethodMatcher().apply(init))
    }

    /**
     * @see enumValue
     */
    @kotlin.internal.InlineOnly
    inline fun enumValue(init: FieldMatcher.() -> Unit) = also {
        enumValue(FieldMatcher().apply(init))
    }

    /**
     * @see arrayValue
     */
    @kotlin.internal.InlineOnly
    inline fun arrayValue(init: AnnotationEncodeArrayMatcher.() -> Unit) = also {
        arrayValue(AnnotationEncodeArrayMatcher().apply(init))
    }

    /**
     * @see annotationValue
     */
    @kotlin.internal.InlineOnly
    inline fun annotationValue(init: AnnotationMatcher.() -> Unit) = also {
        annotationValue(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified number.
         * ----------------
         * 根据指定的数字创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param number number / 数字
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun create(number: Number): AnnotationEncodeValueMatcher {
            return AnnotationEncodeValueMatcher().numberValue(number)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Byte].
         * ----------------
         * 根据指定的 [Byte] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value byte value / 字节值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createByte(value: Byte): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ByteValue
            return AnnotationEncodeValueMatcher(EncodeValueByte(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Short].
         * ----------------
         * 根据指定的 [Short] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 
         * @param value short value / 短整型值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createShort(value: Short): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ShortValue
            return AnnotationEncodeValueMatcher(EncodeValueShort(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Char].
         * ----------------
         * 根据指定的 [Char] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 
         * @param value char value / 字符值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createChar(value: Char): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.CharValue
            return AnnotationEncodeValueMatcher(EncodeValueChar(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Int].
         * ----------------
         * 根据指定的 [Int] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 
         * @param value int value / 整型值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createInt(value: Int): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.IntValue
            return AnnotationEncodeValueMatcher(EncodeValueInt(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Long].
         * ----------------
         * 根据指定的 [Long] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 
         * @param value long value / 长整型值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createLong(value: Long): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.LongValue
            return AnnotationEncodeValueMatcher(EncodeValueLong(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Float].
         * Because floating-point numbers are affected by precision,
         * so abs(value - realValue) < 1e-6 will match successfully.
         * ----------------
         * 根据指定的 [Float] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
         * 
         * @param value float value / 单精度浮点型值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createFloat(value: Float): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.FloatValue
            return AnnotationEncodeValueMatcher(EncodeValueFloat(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Double].
         * Because floating-point numbers are affected by precision,
         * so abs(value - realValue) < 1e-6 will match successfully.
         * ----------------
         * 根据指定的 [Double] 创建一个新的 [AnnotationEncodeValueMatcher]。
         * 由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
         * 
         * @param value double value / 双精度浮点型值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createDouble(value: Double): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.DoubleValue
            return AnnotationEncodeValueMatcher(EncodeValueDouble(value), type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [StringMatcher].
         * ----------------
         * 根据指定的 [StringMatcher] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value string matcher / 字符串匹配器
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createString(value: StringMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.StringValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [String].
         * ----------------
         * 根据指定的 [String] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value string / 字符串
         * @param matchType match type / 匹配类型
         * @param ignoreCase ignore case / 忽略大小写
         * @return [AnnotationEncodeValueMatcher]
         */
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

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [ClassMatcher].
         * ----------------
         * 根据指定的 [ClassMatcher] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value class matcher / 类匹配器
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createClass(value: ClassMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.TypeValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified enumValue.
         * The value of enum is a Field, so here use FieldMatcher.
         * ----------------
         * 根据指定的 enumValue 创建一个新的 [AnnotationEncodeValueMatcher]。
         * enum 的值是一个 Field，所以这里使用 FieldMatcher。
         *
         * @param value enum matcher / 枚举匹配器
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createEnum(value: FieldMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.EnumValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [AnnotationEncodeArrayMatcher].
         * ----------------
         * 根据指定的 [AnnotationEncodeArrayMatcher] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value array matcher / 数组匹配器
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createArray(value: AnnotationEncodeArrayMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.ArrayValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [AnnotationMatcher].
         * ----------------
         * 根据指定的 [AnnotationMatcher] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value annotation matcher / 注解匹配器
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createAnnotation(value: AnnotationMatcher): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.AnnotationValue
            return AnnotationEncodeValueMatcher(value, type)
        }

        /**
         * Create a new [AnnotationEncodeValueMatcher] from the specified [Boolean].
         * ----------------
         * 根据指定的 [Boolean] 创建一个新的 [AnnotationEncodeValueMatcher]。
         *
         * @param value bool value / 布尔值
         * @return [AnnotationEncodeValueMatcher]
         */
        @JvmStatic
        fun createBoolean(value: Boolean): AnnotationEncodeValueMatcher {
            val type = AnnotationEncodeValueType.BoolValue
            return AnnotationEncodeValueMatcher(EncodeValueBoolean(value), type)
        }
    }
}