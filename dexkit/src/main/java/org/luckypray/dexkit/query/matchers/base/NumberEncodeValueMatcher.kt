@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.INumberEncodeValue
import org.luckypray.dexkit.query.enums.NumberEncodeValueType
import org.luckypray.dexkit.query.matchers.EncodeValueByte
import org.luckypray.dexkit.query.matchers.EncodeValueDouble
import org.luckypray.dexkit.query.matchers.EncodeValueFloat
import org.luckypray.dexkit.query.matchers.EncodeValueInt
import org.luckypray.dexkit.query.matchers.EncodeValueLong
import org.luckypray.dexkit.query.matchers.EncodeValueShort

class NumberEncodeValueMatcher {
    @JvmSynthetic
    var value: INumberEncodeValue? = null
        private set
    @JvmSynthetic
    var type: NumberEncodeValueType? = null
        private set

    /**
     * Create empty [NumberEncodeValueMatcher].
     * ----------------
     * 创建空的 [NumberEncodeValueMatcher]。
     * 
     * @return [NumberEncodeValueMatcher]
     */
    constructor()

    /**
     * Create a new [NumberEncodeValueMatcher] from the specified [Number].
     * ----------------
     * 根据指定的 [Number] 创建一个新的 [NumberEncodeValueMatcher]。
     * 
     * @param value number / 数字
     * @return [NumberEncodeValueMatcher]
     */
    constructor(value: Number) {
        value(value)
    }
    
    private constructor(value: INumberEncodeValue, type: NumberEncodeValueType) {
        this.value = value
        this.type = type
    }

    /**
     * Set the value to be matched.
     * ----------------
     * 设置待匹配的数值。
     * 
     * @param value number / 数字
     * @return [NumberEncodeValueMatcher]
     */
    fun value(value: Number) = also {
        when (value) {
            is Byte -> byteValue(value)
            is Short -> shortValue(value)
            is Int -> intValue(value)
            is Long -> longValue(value)
            is Float -> floatValue(value)
            is Double -> doubleValue(value)
        }
    }

    /**
     * Set byteValue to be matched.
     * ----------------
     * 设置待匹配的 byteValue。
     * 
     * @param value byteValue / 字节
     * @return [NumberEncodeValueMatcher]
     */
    fun byteValue(value: Byte) = also {
        this.value = EncodeValueByte(value)
        this.type = NumberEncodeValueType.ByteValue
    }

    /**
     * Set shortValue to be matched.
     * ----------------
     * 设置待匹配的 shortValue。
     * 
     * @param value shortValue / 短整型
     * @return [NumberEncodeValueMatcher]
     */
    fun shortValue(value: Short) = also {
        this.value = EncodeValueShort(value)
        this.type = NumberEncodeValueType.ShortValue
    }

    /**
     * Set intValue to be matched.
     * ----------------
     * 设置待匹配的 intValue。
     * 
     * @param value intValue / 整型
     * @return [NumberEncodeValueMatcher]
     */
    fun intValue(value: Int) = also {
        this.value = EncodeValueInt(value)
        this.type = NumberEncodeValueType.IntValue
    }

    /**
     * Set longValue to be matched.
     * ----------------
     * 设置待匹配的 longValue。
     * 
     * @param value longValue / 长整型
     * @return [NumberEncodeValueMatcher]
     */
    fun longValue(value: Long) = also {
        this.value = EncodeValueLong(value)
        this.type = NumberEncodeValueType.LongValue
    }

    /**
     * Set floatValue to be matched.
     * ----------------
     * 设置待匹配的 floatValue。   
     * 
     * @param value floatValue / 单精度浮点型
     * @return [NumberEncodeValueMatcher]
     */
    fun floatValue(value: Float) = also {
        this.value = EncodeValueFloat(value)
        this.type = NumberEncodeValueType.FloatValue
    }

    /**
     * Set doubleValue to be matched.
     * ----------------
     * 设置待匹配的 doubleValue。
     * 
     * @param value doubleValue / 双精度浮点型
     * @return [NumberEncodeValueMatcher]
     */
    fun doubleValue(value: Double) = also {
        this.value = EncodeValueDouble(value)
        this.type = NumberEncodeValueType.DoubleValue
    }

    companion object {
        /**
         * Create empty [NumberEncodeValueMatcher].
         * ----------------
         * 创建空的 [NumberEncodeValueMatcher]。
         * 
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun create() = NumberEncodeValueMatcher()

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Number].
         * ----------------
         * 根据指定的 [Number] 创建一个新的 [NumberEncodeValueMatcher]。
         * 
         * @param value number / 数字
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun create(value: Number) = NumberEncodeValueMatcher().value(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Byte].
         * ----------------
         * 根据指定的 [Byte] 创建一个新的 [NumberEncodeValueMatcher]。
         * 
         * @param value byteValue / 字节
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createByte(value: Byte) = NumberEncodeValueMatcher().byteValue(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Short].
         * ----------------
         * 根据指定的 [Short] 创建一个新的 [NumberEncodeValueMatcher]。
         * 
         * @param value shortValue / 短整型
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createShort(value: Short) = NumberEncodeValueMatcher().shortValue(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Int].
         * ----------------
         * 根据指定的 [Int] 创建一个新的 [NumberEncodeValueMatcher]。
         * 
         * @param value intValue / 整型
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createInt(value: Int) = NumberEncodeValueMatcher().intValue(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Long].
         * ----------------
         * 根据指定的 [Long] 创建一个新的 [NumberEncodeValueMatcher]。
         * 
         * @param value longValue / 长整型
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createLong(value: Long) = NumberEncodeValueMatcher().longValue(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Float].
         * Because floating-point numbers are affected by precision,
         * so abs(value - realValue) < 1e-6 will match successfully.
         * ----------------
         * 根据指定的 [Float] 创建一个新的 [NumberEncodeValueMatcher]。
         * 由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
         * 
         * @param value floatValue / 单精度浮点型
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createFloat(value: Float) = NumberEncodeValueMatcher().floatValue(value)

        /**
         * Create a new [NumberEncodeValueMatcher] from the specified [Double].
         * Because floating-point numbers are affected by precision,
         * so abs(value - realValue) < 1e-6 will match successfully.
         * ----------------
         * 根据指定的 [Double] 创建一个新的 [NumberEncodeValueMatcher]。
         * 由于浮点数受到精度影响，所以 abs(value - realValue) < 1e-6 时会匹配成功。
         * 
         * @param value doubleValue / 双精度浮点型
         * @return [NumberEncodeValueMatcher]
         */
        @JvmStatic
        fun createDouble(value: Double) = NumberEncodeValueMatcher().doubleValue(value)
    }
}