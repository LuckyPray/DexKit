package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerUsingFieldMeta
import org.luckypray.dexkit.InnerUsingType

data class UsingFieldData(
    val fieldData: FieldData,
    val usingType: UsingType,
) {
    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, usingFieldMeta: InnerUsingFieldMeta): UsingFieldData {
            val fieldData = FieldData.from(bridge, usingFieldMeta.field!!)
            val usingType = when (usingFieldMeta.usingType) {
                InnerUsingType.Get -> UsingType.Get
                InnerUsingType.Put -> UsingType.Put
                else -> throw IllegalArgumentException("Unknown using type: ${usingFieldMeta.usingType}")
            }
            return UsingFieldData(fieldData, usingType)
        }
    }

    /**
     * Using field type
     */
    enum class UsingType {
        /**
         * Read field
         * ----------------
         * 读取了字段
         */
        Get,

        /**
         * Write field
         * ----------------
         * 写入了字段
         */
        Put,
        ;
    }
}