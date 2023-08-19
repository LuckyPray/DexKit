@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationElementMeta
import org.luckypray.dexkit.InnerAnnotationEncodeValueMeta
import org.luckypray.dexkit.InnerAnnotationMeta
import org.luckypray.dexkit.query.enums.RetentionPolicyType
import org.luckypray.dexkit.result.base.BaseData

class AnnotationData(
    bridge: DexKitBridge,
    val dexId: Int,
    val typeId: Int,
    val typeDescriptor: String,
    val retentionPolicyType: RetentionPolicyType,
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
                RetentionPolicyType.from(annotationMeta.retentionPolicy),
                elements
            )
        }
    }

    override fun toString(): String {
        return "AnnotationData(dexId=$dexId, typeId=$typeId, typeDescriptor='$typeDescriptor', retentionPolicyType=$retentionPolicyType, elements=$elements)"
    }
}