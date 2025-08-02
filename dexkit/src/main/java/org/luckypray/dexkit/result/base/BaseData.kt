/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
package org.luckypray.dexkit.result.base

import org.luckypray.dexkit.DexKitBridge

abstract class BaseData(
    protected val bridge: DexKitBridge,
    protected val id: Int = -1,
    protected val dexId: Int = -1,
) {
    @JvmSynthetic
    internal fun getBridge() = bridge

    protected fun getEncodeId(dexId: Int, id: Int) = ((dexId.toLong() shl 32) or id.toLong())

    fun getEncodeId(): Long {
        if (id < 0) error("not has id")
        return getEncodeId(dexId, id)
    }
}