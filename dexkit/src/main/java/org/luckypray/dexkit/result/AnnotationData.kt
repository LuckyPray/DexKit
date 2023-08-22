@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.query.enums.AnnotationVisibilityType
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexDescriptorUtil.getClassName

class AnnotationData(
    bridge: DexKitBridge,
    val dexId: Int,
    val typeId: Int,
    val typeDescriptor: String,
    val visibility: AnnotationVisibilityType,
    val elements: List<AnnotationElementData>
) : BaseData(bridge) {

    companion object {
        internal fun from(
            bridge: DexKitBridge,
            annotationMeta: InnerAnnotationMeta
        ): AnnotationData {
            val elements = mutableListOf<AnnotationElementData>().apply {
                for (i in 0 until annotationMeta.elementsLength) {
                    add(AnnotationElementData.from(bridge, annotationMeta.elements(i)!!))
                }
            }
            return AnnotationData(
                bridge,
                annotationMeta.dexId.toInt(),
                annotationMeta.typeId.toInt(),
                annotationMeta.typeDescriptor!!,
                AnnotationVisibilityType.from(annotationMeta.visibility),
                elements
            )
        }
    }

    override fun toString(): String {
        return buildString {
            append("@${getClassName(typeDescriptor)}")
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