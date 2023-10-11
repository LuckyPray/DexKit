@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class AnnotationEncodeArrayMatcher : BaseQuery(), IAnnotationEncodeValue {
    var encodeValuesMatcher: MutableList<AnnotationEncodeValueMatcher>? = null
        private set

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var rangeMatcher: IntRange? = null
        private set

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     */
    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            count(value)
        }

    /**
     * Need to match array elements.
     * ----------------
     * 要匹配的数组元素列表
     *
     * @param elements elements / 元素列表
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun matchers(elements: Collection<AnnotationEncodeValueMatcher>) = also {
        this.encodeValuesMatcher = elements.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param count count / 数量
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param range range / 范围
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param range range / 范围
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param min min / 最小值
     * @param max max / 最大值
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param min min / 最小值
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * array elements count to match.
     * ----------------
     * 要匹配的数组元素数量。
     *
     * @param max max / 最大值
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add a matcher.
     * ----------------
     * 添加一个匹配器。
     *
     * @param element matcher / 匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun add(element: AnnotationEncodeValueMatcher) = also {
        encodeValuesMatcher = encodeValuesMatcher ?: mutableListOf()
        encodeValuesMatcher!!.add(element)
    }

    /**
     * Add a number to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的数字。
     *
     * @param number number / 数字
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addNumber(number: Number) = also {
        add(AnnotationEncodeValueMatcher().apply {
            when (number) {
                is Byte -> byteValue(number)
                is Short -> shortValue(number)
                is Int -> intValue(number)
                is Long -> longValue(number)
                is Float -> floatValue(number)
                is Double -> doubleValue(number)
            }
        })
    }

    /**
     * Add a byte to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的字节。
     *
     * @param value byte / 字节
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addByte(value: Byte) = also {
        add(AnnotationEncodeValueMatcher().apply { byteValue(value) })
    }

    /**
     * Add a short to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的短整型。
     *
     * @param value short / 短整型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addShort(value: Short) = also {
        add(AnnotationEncodeValueMatcher().apply { shortValue(value) })
    }

    /**
     * Add a int to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的整型。
     *
     * @param value int / 整型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addInt(value: Int) = also {
        add(AnnotationEncodeValueMatcher().apply { intValue(value) })
    }

    /**
     * Add a long to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的长整型。
     *
     * @param value long / 长整型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addLong(value: Long) = also {
        add(AnnotationEncodeValueMatcher().apply { longValue(value) })
    }

    /**
     * Add a float to the array to be matched.
     * Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 向待匹配数组中添加一个已知的浮点型。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value float / 浮点型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addFloat(value: Float) = also {
        add(AnnotationEncodeValueMatcher().apply { floatValue(value) })
    }

    /**
     * Add a double to the array to be matched.
     * Because floating-point numbers are affected by precision,
     * so abs(value - realValue) < 1e-6 will match successfully.
     * ----------------
     * 向待匹配数组中添加一个已知的双精度浮点型。由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
     *
     * @param value double / 双精度浮点型
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addDouble(value: Double) = also {
        add(AnnotationEncodeValueMatcher().apply { doubleValue(value) })
    }

    /**
     * Add a string matcher to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的字符串匹配器。
     *
     * @param value string matcher / 字符串匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addString(value: StringMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { stringValue(value) })
    }


    /**
     * Add a string to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的字符串。
     *
     * @param value string / 字符串
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationEncodeArrayMatcher]
     */
    @JvmOverloads
    fun addString(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(AnnotationEncodeValueMatcher().apply { stringValue(value, matchType, ignoreCase) })
    }

    /**
     * Add a class matcher to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的类匹配器。
     *
     * @param value class matcher / 类匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addClass(value: ClassMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { classValue(value) })
    }

    /**
     * Add a class to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的类。
     *
     * @param className class name / 类名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationEncodeArrayMatcher]
     */
    @JvmOverloads
    fun addClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(AnnotationEncodeValueMatcher().apply {
            classValue { className(className, matchType, ignoreCase) }
        })
    }

    /**
     * Add a method matcher to the array to be matched.
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 向待匹配数组中添加一个已知的方法匹配器。
     * 注意：只有 dalvik.system 类型的注解才包含此元素。
     *
     * @param value method matcher / 方法匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addMethod(value: MethodMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { methodValue(value) })
    }

    /**
     * Add a enum matcher to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的枚举匹配器。
     *
     * @param value enum matcher / 枚举匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addEnum(value: FieldMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { enumValue(value) })
    }

    /**
     * Add a array matcher to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的数组匹配器。
     *
     * @param value array matcher / 数组匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addArray(value: AnnotationEncodeArrayMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { arrayValue(value) })
    }

    /**
     * Add a annotation matcher to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的注解匹配器。
     *
     * @param value annotation matcher / 注解匹配器
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addAnnotation(value: AnnotationMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { annotationValue(value) })
    }

    /**
     * Add a EncodeNull to the array to be matched.
     * Note: only dalvik.system type annotations contain this element.
     * ----------------
     * 向待匹配数组中添加一个 EncodeNull。
     * 注意：只有 dalvik.system 类型的注解才包含此元素。
     *
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addNull() = also {
        add(AnnotationEncodeValueMatcher().apply { nullValue() })
    }

    /**
     * Add a boolean to the array to be matched.
     * ----------------
     * 向待匹配数组中添加一个已知的布尔值。
     *
     * @param value boolean / 布尔值
     * @return [AnnotationEncodeArrayMatcher]
     */
    fun addBool(value: Boolean) = also {
        add(AnnotationEncodeValueMatcher().apply { boolValue(value) })
    }

    // region DSL

    /**
     * @see add
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        add(AnnotationEncodeValueMatcher().apply(init))
    }

    /**
     * @see addClass
     */
    @kotlin.internal.InlineOnly
    inline fun addClass(init: ClassMatcher.() -> Unit) = also {
        addClass(ClassMatcher().apply(init))
    }

    /**
     * @see addMethod
     */
    @kotlin.internal.InlineOnly
    inline fun addMethod(init: MethodMatcher.() -> Unit) = also {
        addMethod(MethodMatcher().apply(init))
    }

    /**
     * @see addEnum
     */
    @kotlin.internal.InlineOnly
    inline fun addEnum(init: FieldMatcher.() -> Unit) = also {
        addEnum(FieldMatcher().apply(init))
    }

    /**
     * @see addArray
     */
    @kotlin.internal.InlineOnly
    inline fun addArray(init: AnnotationEncodeArrayMatcher.() -> Unit) = also {
        addArray(AnnotationEncodeArrayMatcher().apply(init))
    }

    /**
     * @see addAnnotation
     */
    @kotlin.internal.InlineOnly
    inline fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementsMatcher()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationEncodeArrayMatcher.createAnnotationEncodeArrayMatcher(
            fbb,
            encodeValuesMatcher?.map { it.type!!.value }?.toUByteArray()
                ?.let { InnerAnnotationEncodeArrayMatcher.createValuesTypeVector(fbb, it) } ?: 0,
            encodeValuesMatcher?.map { (it.value as BaseQuery).build(fbb) }?.toIntArray()
                ?.let { InnerAnnotationEncodeArrayMatcher.createValuesVector(fbb, it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}