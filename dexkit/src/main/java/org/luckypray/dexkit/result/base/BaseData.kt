package org.luckypray.dexkit.result.base

import org.luckypray.dexkit.DexKitBridge

abstract class BaseData(
    protected val bridge: DexKitBridge
) {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun getBridge() = bridge
}