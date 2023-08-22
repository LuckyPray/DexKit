package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerAnnotationVisibilityType

enum class AnnotationVisibilityType {
    Build,
    Runtime,
    System,
    ;

    companion object {
        fun from(retentionPolicy: Byte): AnnotationVisibilityType {
            return when (retentionPolicy) {
                InnerAnnotationVisibilityType.Build -> Build
                InnerAnnotationVisibilityType.Runtime -> Runtime
                InnerAnnotationVisibilityType.System -> System
                else -> throw IllegalArgumentException("Unknown AnnotationVisibilityType: $retentionPolicy")
            }
        }
    }
}