@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerAnnotationElementMeta
import org.luckypray.dexkit.InnerAnnotationEncodeValueMeta
import org.luckypray.dexkit.result.base.BaseData

class AnnotationElementData(
    bridge: DexKitBridge,
    val name: String,
    val value: AnnotationEncodeValue
) : BaseData(bridge) {

    companion object {
        internal fun from(
            bridge: DexKitBridge,
            element: InnerAnnotationElementMeta
        ): AnnotationElementData {
            val value = element.value(InnerAnnotationEncodeValueMeta()) as InnerAnnotationEncodeValueMeta
            return AnnotationElementData(bridge, element.name!!, AnnotationEncodeValue.from(bridge, value))
        }
    }

    override fun toString(): String {
        return buildString {
            append(name)
            append("=")
            append(value)
        }
    }
}