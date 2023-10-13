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

package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerAnnotationEncodeValue

enum class AnnotationEncodeValueType(val value: UByte) {
    ByteValue(InnerAnnotationEncodeValue.EncodeValueByte),
    ShortValue(InnerAnnotationEncodeValue.EncodeValueShort),
    CharValue(InnerAnnotationEncodeValue.EncodeValueChar),
    IntValue(InnerAnnotationEncodeValue.EncodeValueInt),
    LongValue(InnerAnnotationEncodeValue.EncodeValueLong),
    FloatValue(InnerAnnotationEncodeValue.EncodeValueFloat),
    DoubleValue(InnerAnnotationEncodeValue.EncodeValueDouble),
    StringValue(InnerAnnotationEncodeValue.EncodeValueString),
    TypeValue(InnerAnnotationEncodeValue.ClassMeta),
    MethodValue(InnerAnnotationEncodeValue.MethodMeta),
    EnumValue(InnerAnnotationEncodeValue.FieldMeta),
    ArrayValue(InnerAnnotationEncodeValue.AnnotationEncodeArray),
    AnnotationValue(InnerAnnotationEncodeValue.AnnotationMeta),
    NullValue(InnerAnnotationEncodeValue.EncodeValueNull),
    BoolValue(InnerAnnotationEncodeValue.EncodeValueBoolean),
    ;

    companion object {
        fun from(value: UByte): AnnotationEncodeValueType {
            return when (value) {
                InnerAnnotationEncodeValue.EncodeValueByte -> ByteValue
                InnerAnnotationEncodeValue.EncodeValueShort -> ShortValue
                InnerAnnotationEncodeValue.EncodeValueChar -> CharValue
                InnerAnnotationEncodeValue.EncodeValueInt -> IntValue
                InnerAnnotationEncodeValue.EncodeValueLong -> LongValue
                InnerAnnotationEncodeValue.EncodeValueFloat -> FloatValue
                InnerAnnotationEncodeValue.EncodeValueDouble -> DoubleValue
                InnerAnnotationEncodeValue.EncodeValueString -> StringValue
                InnerAnnotationEncodeValue.ClassMeta -> TypeValue
                InnerAnnotationEncodeValue.MethodMeta -> MethodValue
                InnerAnnotationEncodeValue.FieldMeta -> EnumValue
                InnerAnnotationEncodeValue.AnnotationEncodeArray -> ArrayValue
                InnerAnnotationEncodeValue.AnnotationMeta -> AnnotationValue
                InnerAnnotationEncodeValue.EncodeValueNull -> NullValue
                InnerAnnotationEncodeValue.EncodeValueBoolean -> BoolValue
                else -> throw IllegalArgumentException("Unknown AnnotationEncodeValueType: $value")
            }
        }
    }
}