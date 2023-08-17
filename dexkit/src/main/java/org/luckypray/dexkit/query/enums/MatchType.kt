package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.alias.InnerMatchType

enum class MatchType(val value: Byte) {
    Contains(InnerMatchType.Contains),
    Equal(InnerMatchType.Equal),
    ;
}