@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerEncodeValueBoolean
import org.luckypray.dexkit.InnerEncodeValueByte
import org.luckypray.dexkit.InnerEncodeValueChar
import org.luckypray.dexkit.InnerEncodeValueDouble
import org.luckypray.dexkit.InnerEncodeValueFloat
import org.luckypray.dexkit.InnerEncodeValueInt
import org.luckypray.dexkit.InnerEncodeValueLong
import org.luckypray.dexkit.InnerEncodeValueShort
import org.luckypray.dexkit.InnerEncodeValueString
import org.luckypray.dexkit.query.base.BaseQuery

abstract class EncodeValue : BaseQuery()

class EncodeValueByte(val value: Byte) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueByte.createEncodeValueByte(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueShort(val value: Short) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueShort.createEncodeValueShort(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueChar(val value: Char) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueChar.createEncodeValueChar(fbb, value.code.toShort())
        fbb.finish(root)
        return root
    }
}

class EncodeValueInt(val value: Int) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueInt.createEncodeValueInt(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueLong(val value: Long) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueLong.createEncodeValueLong(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueFloat(val value: Float) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueFloat.createEncodeValueFloat(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueDouble(val value: Double) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueDouble.createEncodeValueDouble(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueString(val value: String) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueString.createEncodeValueString(fbb, fbb.createString(value))
        fbb.finish(root)
        return root
    }
}

class EncodeValueBoolean(val value: Boolean) : EncodeValue() {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueBoolean.createEncodeValueBoolean(fbb, value)
        fbb.finish(root)
        return root
    }
}
