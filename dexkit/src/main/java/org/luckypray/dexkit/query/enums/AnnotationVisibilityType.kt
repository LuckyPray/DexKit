package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerAnnotationVisibilityType

/**
 * https://source.android.com/docs/core/runtime/dex-format?hl=zh-cn#visibility
 */
enum class AnnotationVisibilityType {
    /**
     * VISIBILITY_BUILD
     */
    Build,

    /**
     * VISIBILITY_RUNTIME
     */
    Runtime,

    /**
     * VISIBILITY_SYSTEM
     */
    System,
    ;

    companion object {
        fun from(retentionPolicy: Byte): AnnotationVisibilityType? {
            return when (retentionPolicy) {
                InnerAnnotationVisibilityType.Build -> Build
                InnerAnnotationVisibilityType.Runtime -> Runtime
                InnerAnnotationVisibilityType.System -> System
                InnerAnnotationVisibilityType.None -> null
                else -> throw IllegalArgumentException("Unknown AnnotationVisibilityType: $retentionPolicy")
            }
        }
    }
}