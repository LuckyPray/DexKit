@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationEncodeArray
import org.luckypray.dexkit.InnerAnnotationEncodeValueMeta
import org.luckypray.dexkit.result.base.BaseData

class AnnotationEncodeArrayData(
    bridge: DexKitBridge,
    val values: List<AnnotationEncodeValue>
) : BaseData(bridge) {

    companion object {
        internal fun from(
            bridge: DexKitBridge,
            annotationEncodeArray: InnerAnnotationEncodeArray
        ): AnnotationEncodeArrayData {
            val values = mutableListOf<AnnotationEncodeValue>().apply {
                for (i in 0 until annotationEncodeArray.valuesLength) {
                    val value = annotationEncodeArray.values(i)!!.value(InnerAnnotationEncodeValueMeta())
                    add(AnnotationEncodeValue.from(bridge, value as InnerAnnotationEncodeValueMeta))
                }
            }
            return AnnotationEncodeArrayData(bridge, values)
        }
    }

    override fun toString(): String {
        return "AnnotationEncodeArrayData(values=$values)"
    }
}