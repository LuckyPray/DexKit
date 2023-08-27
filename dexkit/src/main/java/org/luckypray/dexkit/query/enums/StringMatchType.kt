package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerStringMatchType

enum class StringMatchType(val value: Byte) {
    Contains(InnerStringMatchType.Contains),
    StartsWith(InnerStringMatchType.StartWith),
    EndsWith(InnerStringMatchType.EndWith),
    SimilarRegex(InnerStringMatchType.SimilarRegex),
    Equals(InnerStringMatchType.Equal),
    ;
}