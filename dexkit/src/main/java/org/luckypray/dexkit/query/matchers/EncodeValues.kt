/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
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
import org.luckypray.dexkit.InnerEncodeValueNull
import org.luckypray.dexkit.InnerEncodeValueShort
import org.luckypray.dexkit.InnerEncodeValueString
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.base.INumberEncodeValue

abstract class EncodeValue : BaseQuery()

class EncodeValueByte(val value: Byte) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueByte.createEncodeValueByte(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueShort(val value: Short) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueShort.createEncodeValueShort(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueChar(val value: Char) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueChar.createEncodeValueChar(fbb, value.code.toShort())
        fbb.finish(root)
        return root
    }
}

class EncodeValueInt(val value: Int) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueInt.createEncodeValueInt(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueLong(val value: Long) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueLong.createEncodeValueLong(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueFloat(val value: Float) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueFloat.createEncodeValueFloat(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueDouble(val value: Double) : EncodeValue(), IAnnotationEncodeValue, INumberEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueDouble.createEncodeValueDouble(fbb, value)
        fbb.finish(root)
        return root
    }
}

class EncodeValueString(val value: String) : EncodeValue() {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueString.createEncodeValueString(fbb, fbb.createString(value))
        fbb.finish(root)
        return root
    }
}

class EncodeValueNull : EncodeValue(), IAnnotationEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueNull.createEncodeValueNull(fbb, 0)
        fbb.finish(root)
        return root
    }
}

class EncodeValueBoolean(val value: Boolean) : EncodeValue(), IAnnotationEncodeValue {
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerEncodeValueBoolean.createEncodeValueBoolean(fbb, value)
        fbb.finish(root)
        return root
    }
}
