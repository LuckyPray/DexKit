/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */

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