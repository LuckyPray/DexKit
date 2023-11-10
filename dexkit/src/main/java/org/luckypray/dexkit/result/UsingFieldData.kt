package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerUsingFieldMeta
import org.luckypray.dexkit.InnerUsingType

data class UsingFieldData(
    val fieldData: FieldData,
    val usingType: FieldUsingType,
) {
    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, usingFieldMeta: InnerUsingFieldMeta): UsingFieldData {
            val fieldData = FieldData.from(bridge, usingFieldMeta.field!!)
            val usingType = when (usingFieldMeta.usingType) {
                InnerUsingType.Get -> FieldUsingType.Get
                InnerUsingType.Put -> FieldUsingType.Put
                else -> throw IllegalArgumentException("Unknown using type: ${usingFieldMeta.usingType}")
            }
            return UsingFieldData(fieldData, usingType)
        }
    }
}