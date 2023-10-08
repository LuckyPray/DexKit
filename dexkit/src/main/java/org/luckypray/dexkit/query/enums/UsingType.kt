package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerUsingType

enum class UsingType(val value: Byte) {
    /**
     * Get or Put
     */
    Any(InnerUsingType.Any_),
    Get(InnerUsingType.Get),
    Put(InnerUsingType.Put),
    ;
}