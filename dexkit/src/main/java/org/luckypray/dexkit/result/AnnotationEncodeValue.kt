@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationEncodeArray
import org.luckypray.dexkit.query.enums.AnnotationEncodeValueType
import org.luckypray.dexkit.InnerAnnotationEncodeValueMeta
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.InnerClassMeta
import org.luckypray.dexkit.InnerEncodeValueBoolean
import org.luckypray.dexkit.InnerEncodeValueByte
import org.luckypray.dexkit.InnerEncodeValueChar
import org.luckypray.dexkit.InnerEncodeValueDouble
import org.luckypray.dexkit.InnerEncodeValueFloat
import org.luckypray.dexkit.InnerEncodeValueInt
import org.luckypray.dexkit.InnerEncodeValueLong
import org.luckypray.dexkit.InnerEncodeValueShort
import org.luckypray.dexkit.InnerEncodeValueString
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.result.base.BaseData

class AnnotationEncodeValue(
    val value: Any,
    val type: AnnotationEncodeValueType
) {

    companion object {
        internal fun from(bridge: DexKitBridge, encodeValueMeta: InnerAnnotationEncodeValueMeta): AnnotationEncodeValue {
            val type = AnnotationEncodeValueType.from(encodeValueMeta.valueType)
            val value: Any = when (type) {
                AnnotationEncodeValueType.ByteValue -> (encodeValueMeta.value(InnerEncodeValueByte()) as InnerEncodeValueByte).value
                AnnotationEncodeValueType.ShortValue -> (encodeValueMeta.value(InnerEncodeValueShort()) as InnerEncodeValueShort).value
                AnnotationEncodeValueType.CharValue -> (encodeValueMeta.value(InnerEncodeValueChar()) as InnerEncodeValueChar).value
                AnnotationEncodeValueType.IntValue -> (encodeValueMeta.value(InnerEncodeValueInt()) as InnerEncodeValueInt).value
                AnnotationEncodeValueType.LongValue -> (encodeValueMeta.value(InnerEncodeValueLong()) as InnerEncodeValueLong).value
                AnnotationEncodeValueType.FloatValue -> (encodeValueMeta.value(InnerEncodeValueFloat()) as InnerEncodeValueFloat).value
                AnnotationEncodeValueType.DoubleValue -> (encodeValueMeta.value(InnerEncodeValueDouble()) as InnerEncodeValueDouble).value
                AnnotationEncodeValueType.StringValue -> (encodeValueMeta.value(InnerEncodeValueString()) as InnerEncodeValueString).value!!
                AnnotationEncodeValueType.TypeValue -> ClassData.from(bridge, encodeValueMeta.value(InnerClassMeta()) as InnerClassMeta)
                AnnotationEncodeValueType.EnumValue -> FieldData.from(bridge, encodeValueMeta.value(InnerFieldMeta()) as InnerFieldMeta)
                AnnotationEncodeValueType.ArrayValue -> AnnotationEncodeArrayData.from(bridge, encodeValueMeta.value(InnerAnnotationEncodeArray()) as InnerAnnotationEncodeArray)
                AnnotationEncodeValueType.AnnotationValue -> AnnotationData.from(bridge, encodeValueMeta.value(InnerAnnotationMeta()) as InnerAnnotationMeta)
                AnnotationEncodeValueType.BoolValue -> (encodeValueMeta.value(InnerEncodeValueBoolean()) as InnerEncodeValueBoolean).value
            }
            return AnnotationEncodeValue(value, type)
        }
    }

    override fun toString(): String {
        return "AnnotationEncodeValue(type=$type, value=$value)"
    }
}