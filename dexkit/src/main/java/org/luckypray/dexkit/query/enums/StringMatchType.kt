package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerStringMatchType

enum class StringMatchType(val value: Byte) {
    Contains(InnerStringMatchType.Contains),
    StartWith(InnerStringMatchType.StartWith),
    EndWith(InnerStringMatchType.EndWith),
    SimilarRegex(InnerStringMatchType.SimilarRegex),
    Equal(InnerStringMatchType.Equal),
    ;
}