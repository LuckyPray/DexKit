package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerRetentionPolicyType

enum class RetentionPolicyType(val value: Byte) {
    Source(InnerRetentionPolicyType.Source),
    Class(InnerRetentionPolicyType.Class),
    Runtime(InnerRetentionPolicyType.Runtime),
    ;

    companion object {
        fun from(retentionPolicy: Byte): RetentionPolicyType {
            return when (retentionPolicy) {
                InnerRetentionPolicyType.Source -> Source
                InnerRetentionPolicyType.Class -> Class
                InnerRetentionPolicyType.Runtime -> Runtime
                else -> throw IllegalArgumentException("Unknown RetentionPolicyType: $retentionPolicy")
            }
        }
    }
}