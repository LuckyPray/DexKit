package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerNumber

enum class NumberEncodeValueType(val value: UByte) {
    ByteValue(InnerNumber.EncodeValueByte),
    ShortValue(InnerNumber.EncodeValueShort),
    IntValue(InnerNumber.EncodeValueInt),
    LongValue(InnerNumber.EncodeValueLong),
    FloatValue(InnerNumber.EncodeValueFloat),
    DoubleValue(InnerNumber.EncodeValueDouble),
    ;
}