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
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.query.enums.AnnotationVisibilityType
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil
import org.luckypray.dexkit.wrap.DexClass

class AnnotationData private constructor(
    bridge: DexKitBridge,
    dexId: Int,
    val typeId: Int,
    val typeDescriptor: String,
    val visibility: AnnotationVisibilityType?,
    val elements: List<AnnotationElementData>
) : BaseData(bridge) {

    internal companion object `-Companion` {
        fun from(
            bridge: DexKitBridge,
            annotationMeta: InnerAnnotationMeta
        ) = AnnotationData(
            bridge,
            annotationMeta.dexId.toInt(),
            annotationMeta.typeId.toInt(),
            annotationMeta.typeDescriptor!!,
            AnnotationVisibilityType.from(annotationMeta.visibility),
            mutableListOf<AnnotationElementData>().apply {
                for (i in 0 until annotationMeta.elementsLength) {
                    add(AnnotationElementData.from(bridge, annotationMeta.elements(i)!!))
                }
            }
        )
    }

    private val dexClass by lazy {
        DexClass(typeDescriptor)
    }

    val typeName get() = dexClass.typeName

    override fun toString(): String {
        return buildString {
            append("@${DexSignUtil.getTypeName(typeDescriptor)}")
            append("(")
            elements.forEachIndexed { index, element ->
                if (index != 0) {
                    append(", ")
                }
                append(element)
            }
            append(")")
        }
    }
}