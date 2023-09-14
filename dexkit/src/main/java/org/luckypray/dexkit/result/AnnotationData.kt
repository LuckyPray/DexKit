@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.query.enums.AnnotationVisibilityType
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil

class AnnotationData private constructor(
    bridge: DexKitBridge,
    val dexId: Int,
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

    val typeName: String by lazy {
        DexSignUtil.getSimpleName(typeDescriptor)
    }

    val className get() = typeName

    val name get() = typeName

    override fun toString(): String {
        return buildString {
            append("@${DexSignUtil.getSimpleName(typeDescriptor)}")
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