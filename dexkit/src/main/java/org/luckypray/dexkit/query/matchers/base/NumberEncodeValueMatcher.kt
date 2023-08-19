@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.NumberEncodeValueType
import org.luckypray.dexkit.query.matchers.EncodeValueByte
import org.luckypray.dexkit.query.matchers.EncodeValueDouble
import org.luckypray.dexkit.query.matchers.EncodeValueFloat
import org.luckypray.dexkit.query.matchers.EncodeValueInt
import org.luckypray.dexkit.query.matchers.EncodeValueLong
import org.luckypray.dexkit.query.matchers.EncodeValueShort

class NumberEncodeValueMatcher {
    @JvmSynthetic
    internal var value: BaseQuery? = null
        private set
    @JvmSynthetic
    internal var type: NumberEncodeValueType? = null
        private set

    constructor()
    constructor(value: BaseQuery, type: NumberEncodeValueType) {
        this.value = value
        this.type = type
    }

    fun byteValue(value: Byte) = also {
        this.value = EncodeValueByte(value)
        this.type = NumberEncodeValueType.ByteValue
    }

    fun shortValue(value: Short) = also {
        this.value = EncodeValueShort(value)
        this.type = NumberEncodeValueType.ShortValue
    }

    fun intValue(value: Int) = also {
        this.value = EncodeValueInt(value)
        this.type = NumberEncodeValueType.IntValue
    }

    fun longValue(value: Long) = also {
        this.value = EncodeValueLong(value)
        this.type = NumberEncodeValueType.LongValue
    }

    fun floatValue(value: Float) = also {
        this.value = EncodeValueFloat(value)
        this.type = NumberEncodeValueType.FloatValue
    }

    fun doubleValue(value: Double) = also {
        this.value = EncodeValueDouble(value)
        this.type = NumberEncodeValueType.DoubleValue
    }

    companion object {
        @JvmStatic
        fun create() = NumberEncodeValueMatcher()

        @JvmStatic
        fun createByte(value: Byte) = NumberEncodeValueMatcher().byteValue(value)

        @JvmStatic
        fun createShort(value: Short) = NumberEncodeValueMatcher().shortValue(value)

        @JvmStatic
        fun createInt(value: Int) = NumberEncodeValueMatcher().intValue(value)

        @JvmStatic
        fun createLong(value: Long) = NumberEncodeValueMatcher().longValue(value)

        @JvmStatic
        fun createFloat(value: Float) = NumberEncodeValueMatcher().floatValue(value)

        @JvmStatic
        fun createDouble(value: Double) = NumberEncodeValueMatcher().doubleValue(value)
    }
}