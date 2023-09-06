package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerOpCodeMatchType

enum class OpCodeMatchType(val value: Byte) {
    Contains(InnerOpCodeMatchType.Contains),
    StartsWith(InnerOpCodeMatchType.StartWith),
    EndsWith(InnerOpCodeMatchType.EndWith),
    Equals(InnerOpCodeMatchType.Equal),
    ;
}