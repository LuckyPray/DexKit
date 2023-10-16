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

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationEncodeArray
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.InnerClassMeta
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
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.InnerMethodMeta
import org.luckypray.dexkit.query.enums.AnnotationEncodeValueType
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.MUtf8Util
import org.luckypray.dexkit.util.StringUnicodeEncoderDecoder

class AnnotationEncodeArrayData private constructor(
    bridge: DexKitBridge,
    val values: List<AnnotationEncodeValue>
) : BaseData(bridge) {

    internal companion object `-Companion` {
        fun from(
            bridge: DexKitBridge,
            annotationEncodeArray: InnerAnnotationEncodeArray
        ): AnnotationEncodeArrayData {
            val values = mutableListOf<AnnotationEncodeValue>().apply {
                for (i in 0 until annotationEncodeArray.valuesLength) {
                    val encodeValue = annotationEncodeArray.values(i)!!
                    val type = AnnotationEncodeValueType.from(encodeValue.valueType)
                    val value: Any = when (type) {
                        AnnotationEncodeValueType.ByteValue -> (encodeValue.value(InnerEncodeValueByte()) as InnerEncodeValueByte).value
                        AnnotationEncodeValueType.ShortValue -> (encodeValue.value(InnerEncodeValueShort()) as InnerEncodeValueShort).value
                        AnnotationEncodeValueType.CharValue -> (encodeValue.value(InnerEncodeValueChar()) as InnerEncodeValueChar).value
                        AnnotationEncodeValueType.IntValue -> (encodeValue.value(InnerEncodeValueInt()) as InnerEncodeValueInt).value
                        AnnotationEncodeValueType.LongValue -> (encodeValue.value(InnerEncodeValueLong()) as InnerEncodeValueLong).value
                        AnnotationEncodeValueType.FloatValue -> (encodeValue.value(InnerEncodeValueFloat()) as InnerEncodeValueFloat).value
                        AnnotationEncodeValueType.DoubleValue -> (encodeValue.value(InnerEncodeValueDouble()) as InnerEncodeValueDouble).value
                        AnnotationEncodeValueType.StringValue -> {
                            val encodeValueString = (encodeValue.value(InnerEncodeValueString()) as InnerEncodeValueString)
                            try {
                                encodeValueString.value!!
                            } catch (e: IllegalArgumentException) {
                                // try to unescape unicode
                                runCatching {
                                    encodeValueString.valueAsByteBuffer.let {
                                        val mUtf8String = MUtf8Util.decode(it)
                                        StringUnicodeEncoderDecoder.encodeStringToUnicodeSequence(mUtf8String)
                                    }
                                }.getOrElse { "" }
                            }
                        }
                        AnnotationEncodeValueType.TypeValue -> ClassData.from(bridge, encodeValue.value(InnerClassMeta()) as InnerClassMeta)
                        AnnotationEncodeValueType.MethodValue -> MethodData.from(bridge, encodeValue.value(InnerMethodMeta()) as InnerMethodMeta)
                        AnnotationEncodeValueType.EnumValue -> FieldData.from(bridge, encodeValue.value(InnerFieldMeta()) as InnerFieldMeta)
                        AnnotationEncodeValueType.ArrayValue -> from(bridge, encodeValue.value(InnerAnnotationEncodeArray()) as InnerAnnotationEncodeArray)
                        AnnotationEncodeValueType.AnnotationValue -> AnnotationData.from(bridge, encodeValue.value(InnerAnnotationMeta()) as InnerAnnotationMeta)
                        AnnotationEncodeValueType.NullValue -> encodeValue.value(InnerEncodeValueNull()) as InnerEncodeValueNull
                        AnnotationEncodeValueType.BoolValue -> (encodeValue.value(InnerEncodeValueBoolean()) as InnerEncodeValueBoolean).value
                    }
                    add(AnnotationEncodeValue.from(value, type))
                }
            }
            return AnnotationEncodeArrayData(bridge, values)
        }
    }

    override fun toString(): String {
        return buildString {
            append("{")
            values.forEachIndexed { index, value ->
                if (index != 0) {
                    append(", ")
                }
                append(value)
            }
            append("}")
        }
    }
}