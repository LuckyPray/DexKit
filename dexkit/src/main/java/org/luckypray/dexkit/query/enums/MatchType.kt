package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerMatchType

enum class MatchType(val value: Byte) {
    Contains(InnerMatchType.Contains),
    Equals(InnerMatchType.Equal),
    ;
}