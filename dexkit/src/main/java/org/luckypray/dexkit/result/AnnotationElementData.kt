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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationElementMeta
import org.luckypray.dexkit.InnerAnnotationEncodeValueMeta
import org.luckypray.dexkit.result.base.BaseData

class AnnotationElementData private constructor(
    bridge: DexKitBridge,
    val name: String,
    val value: AnnotationEncodeValue
) : BaseData(bridge) {

    internal companion object `-Companion` {
        fun from(
            bridge: DexKitBridge,
            element: InnerAnnotationElementMeta
        ): AnnotationElementData {
            val value = element.value(InnerAnnotationEncodeValueMeta()) as InnerAnnotationEncodeValueMeta
            return AnnotationElementData(
                bridge,
                element.name!!,
                AnnotationEncodeValue.from(bridge, value)
            )
        }
    }

    override fun toString(): String {
        return buildString {
            append(name)
            append(" = ")
            append(value)
        }
    }
}