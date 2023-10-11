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

    /**
     * Annotation element name to match.
     * ----------------
     * 要匹配的注解元素名称。
     *
     *     @Router(value = "/play")
     *              ^
     *             name
     */
    var name: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            name(value)
        }

    /**
     * Annotation element value to match.
     * ----------------
     * 要匹配的注解元素值。
     *
     *     @Router(value = "/play")
     *              ^
     *             name
     */
    var value: AnnotationEncodeValueMatcher
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            value(value)
        }

    /**
     * Annotation element name to match.
     * ----------------
     * 要匹配的注解元素名称。
     *
     *     @Router(value = "/play")
     *              ^
     *             name
     *
     * @param name annotation element name / 注解元素名称
     * @return [AnnotationElementMatcher]
     */
    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.nameMatcher = StringMatcher(name, StringMatchType.Equals, ignoreCase)
    }

    /**
     * Annotation element value to match.
     * ----------------
     * 要匹配的注解元素值。
     *
     *     @Router(value = "/play")
     *              ^
     *             name
     *
     * @param matcher annotation element value matcher / 注解元素值匹配器
     * @return [AnnotationElementMatcher]
     */
    fun value(matcher: AnnotationEncodeValueMatcher) = also {
        this.valueMatcher = matcher
    }

    /**
     * Match the annotation element value as the specified [Number].
     * ----------------
     * 匹配元素值为指定 [Number] 的注解。
     *
     * @param value number value / 数值
     * @return [AnnotationElementMatcher]
     */
    fun numberValue(value: Number) = also {
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

    /**
     * Match the annotation element value as the specified [Byte].
     * ----------------
     * 匹配元素值为指定 [Byte] 的注解。
     *
     * @param value byte value / 字节
     * @return [AnnotationElementMatcher]
     */
    fun byteValue(value: Byte) = also {
        value(AnnotationEncodeValueMatcher().apply { byteValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Short].
     * ----------------
     * 匹配元素值为指定 [Short] 的注解。
     *
     * @param value short value / 短整型
     * @return [AnnotationElementMatcher]
     */
    fun shortValue(value: Short) = also {
        value(AnnotationEncodeValueMatcher().apply { shortValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Int].
     * ----------------
     * 匹配元素值为指定 [Int] 的注解。
     *
     * @param value int value / 整型
     * @return [AnnotationElementMatcher]
     */
    fun intValue(value: Int) = also {
        value(AnnotationEncodeValueMatcher().apply { intValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Long].
     * ----------------
     * 匹配元素值为指定 [Long] 的注解。
     *
     * @param value long value / 长整型
     * @return [AnnotationElementMatcher]
     */
    fun longValue(value: Long) = also {
        value(AnnotationEncodeValueMatcher().apply { longValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Float].
     * Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 匹配元素值为指定 [Float] 的注解。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value float value / 浮点型
     * @return [AnnotationElementMatcher]
     */
    fun floatValue(value: Float) = also {
        value(AnnotationEncodeValueMatcher().apply { floatValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Double].
     * Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 匹配元素值为指定 [Double] 的注解。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value double value / 双精度浮点型
     * @return [AnnotationElementMatcher]
     */
    fun doubleValue(value: Double) = also {
        value(AnnotationEncodeValueMatcher().apply { doubleValue(value) })
    }

    /**
     * Match the annotation element value as the specified [StringMatcher].
     * ----------------
     * 匹配元素值为指定 [StringMatcher] 的注解。
     *
     * @param value string matcher / 字符串匹配器
     * @return [AnnotationElementMatcher]
     */
    fun stringValue(value: StringMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { stringValue(value) })
    }

    /**
     * Match the annotation element value as the specified [String].
     * ----------------
     * 匹配元素值为指定 [String] 的注解。
     *
     * @param value string value / 字符串
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationElementMatcher]
     */
    @JvmOverloads
    fun stringValue(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        value(AnnotationEncodeValueMatcher().apply { stringValue(value, matchType, ignoreCase) })
    }

    /**
     * Match the annotation element value as the specified [ClassMatcher].
     * ----------------
     * 匹配元素值为指定 [ClassMatcher] 的注解。
     *
     * @param value class matcher / 类匹配器
     * @return [AnnotationElementMatcher]
     */
    fun classValue(value: ClassMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { classValue(value) })
    }

    /**
     * Match the annotation element value as the specified [Class].
     * ----------------
     * 匹配元素值为指定 [Class] 的注解。
     *
     * @param className class name / 类名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationElementMatcher]
     */
    @JvmOverloads
    fun classValue(
        className: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        value(AnnotationEncodeValueMatcher().apply {
            classValue { className(className, matchType, ignoreCase) }
        })
    }

    /**
     * Match the annotation element value as the specified [MethodMatcher].
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 匹配元素值为指定 [MethodMatcher] 的注解。
     * 注意：只有 dalvik.system 类型的注解才包含此元素。
     *
     * @param value method matcher / 方法匹配器
     * @return [AnnotationElementMatcher]
     */
    fun methodValue(value: MethodMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { methodValue(value) })
    }

    /**
     * Match the annotation element value as the specified [FieldMatcher].
     * ----------------
     * 匹配元素值为指定 [FieldMatcher] 的注解。
     *
     * @param value field matcher / 字段匹配器
     * @return [AnnotationElementMatcher]
     */
    fun enumValue(value: FieldMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { enumValue(value) })
    }

    /**
     * Match the annotation element value as the specified [AnnotationEncodeArrayMatcher].
     * ----------------
     * 匹配元素值为指定 [AnnotationEncodeArrayMatcher] 的注解。
     *
     * @param value encode array matcher / 数组匹配器
     * @return [AnnotationElementMatcher]
     */
    fun arrayValue(value: AnnotationEncodeArrayMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { arrayValue(value) })
    }

    /**
     * Match the annotation element value as the specified [AnnotationMatcher].
     * ----------------
     * 匹配元素值为指定 [AnnotationMatcher] 的注解。
     *
     * @param value annotation matcher / 注解匹配器
     * @return [AnnotationElementMatcher]
     */
    fun annotationValue(value: AnnotationMatcher) = also {
        value(AnnotationEncodeValueMatcher().apply { annotationValue(value) })
    }

    /**
     * Create nullValue matcher.
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 创建 nullValue 匹配器。
     * 注意：只有 dalvik.system 类型的注解才包含此元素。
     *
     * @return [AnnotationEncodeValueMatcher]
     */
    fun nullValue() = also {
        value(AnnotationEncodeValueMatcher().apply { nullValue() })
    }

    /**
     * Match the annotation element value as the specified [Boolean].
     * ----------------
     * 匹配元素值为指定 [Boolean] 的注解。
     *
     * @param value boolean value / 布尔值
     * @return [AnnotationElementMatcher]
     */
    fun boolValue(value: Boolean) = also {
        value(AnnotationEncodeValueMatcher().apply { boolValue(value) })
    }

    // region DSL

    /**
     * @see value
     */
    @kotlin.internal.InlineOnly
    inline fun value(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        value(AnnotationEncodeValueMatcher().apply(init))
    }

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
        @JvmStatic
        fun create() = AnnotationElementMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationElementMatcher.createAnnotationElementMatcher(
            fbb,
            nameMatcher?.build(fbb) ?: 0,
            valueMatcher?.type?.value ?: 0U,
            (valueMatcher?.value as BaseQuery?)?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}