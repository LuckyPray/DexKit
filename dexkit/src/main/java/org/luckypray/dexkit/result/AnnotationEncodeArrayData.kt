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
import org.luckypray.dexkit.InnerEncodeValueShort
import org.luckypray.dexkit.InnerEncodeValueString
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.query.enums.AnnotationEncodeValueType
import org.luckypray.dexkit.result.base.BaseData

class AnnotationEncodeArrayData(
    bridge: DexKitBridge,
    val values: List<AnnotationEncodeValue>
) : BaseData(bridge) {

    companion object {
        internal fun from(
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
                            try {
                                (encodeValue.value(InnerEncodeValueString()) as InnerEncodeValueString).value!!
                            } catch (e: IllegalArgumentException) {
                                if (e.message?.contains("Invalid UTF-8") == false) {
                                    throw e
                                }
                                ""
                            }
                        }
                        AnnotationEncodeValueType.TypeValue -> ClassData.from(bridge, encodeValue.value(InnerClassMeta()) as InnerClassMeta)
                        AnnotationEncodeValueType.EnumValue -> FieldData.from(bridge, encodeValue.value(InnerFieldMeta()) as InnerFieldMeta)
                        AnnotationEncodeValueType.ArrayValue -> AnnotationEncodeArrayData.from(bridge, encodeValue.value(InnerAnnotationEncodeArray()) as InnerAnnotationEncodeArray)
                        AnnotationEncodeValueType.AnnotationValue -> AnnotationData.from(bridge, encodeValue.value(InnerAnnotationMeta()) as InnerAnnotationMeta)
                        AnnotationEncodeValueType.BoolValue -> (encodeValue.value(InnerEncodeValueBoolean()) as InnerEncodeValueBoolean).value
                    }
                    add(AnnotationEncodeValue(value, type))
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