package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerRetentionPolicyType

enum class RetentionPolicyType(val value: Byte) {
    /**
     * [java.lang.annotation.RetentionPolicy.SOURCE]
     */
    Source(InnerRetentionPolicyType.Source),

    /**
     * [java.lang.annotation.RetentionPolicy.CLASS]
     */
    Class(InnerRetentionPolicyType.Class),

    /**
     * [java.lang.annotation.RetentionPolicy.RUNTIME]
     */
    Runtime(InnerRetentionPolicyType.Runtime),
    ;
}