package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerUsingType

enum class UsingType(val value: Byte) {
    Any(InnerUsingType.Any_),
    Get(InnerUsingType.Get),
    Put(InnerUsingType.Put),
    ;
}