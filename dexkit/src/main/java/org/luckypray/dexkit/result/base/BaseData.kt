@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.result.base

import org.luckypray.dexkit.DexKitBridge

abstract class BaseData(
    protected val bridge: DexKitBridge
) {
    @kotlin.internal.InlineOnly
    internal fun getBridge() = bridge

    protected fun getEncodeId(dexId: Int, id: Int) = ((dexId.toLong() shl 32) or id.toLong())
}