package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.alias.InnerRetentionPolicyType

enum class RetentionPolicyType(val value: Byte) {
    Source(InnerRetentionPolicyType.Source),
    Class(InnerRetentionPolicyType.Class),
    Runtime(InnerRetentionPolicyType.Runtime),
    ;
}