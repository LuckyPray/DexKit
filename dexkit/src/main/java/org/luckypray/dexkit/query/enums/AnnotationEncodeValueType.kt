package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.alias.InnerAnnotationEncodeValue

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
}