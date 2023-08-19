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
    EnumValue(InnerAnnotationEncodeValue.FieldMeta),
    ArrayValue(InnerAnnotationEncodeValue.AnnotationEncodeArray),
    AnnotationValue(InnerAnnotationEncodeValue.AnnotationMeta),
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
                InnerAnnotationEncodeValue.FieldMeta -> EnumValue
                InnerAnnotationEncodeValue.AnnotationEncodeArray -> ArrayValue
                InnerAnnotationEncodeValue.AnnotationMeta -> AnnotationValue
                InnerAnnotationEncodeValue.EncodeValueBoolean -> BoolValue
                else -> throw IllegalArgumentException("Unknown AnnotationEncodeValueType: $value")
            }
        }
    }
}