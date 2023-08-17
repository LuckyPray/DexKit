package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge

abstract class BaseData(
    private val bridge: DexKitBridge
) {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun getBridge() = bridge
}