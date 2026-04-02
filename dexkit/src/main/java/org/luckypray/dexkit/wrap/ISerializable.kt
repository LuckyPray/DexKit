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

package org.luckypray.dexkit.wrap

interface ISerializable {

    companion object {

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T : ISerializable> deserializeAs(descriptor: String): T {
            return deserialize(descriptor) as T
        }

        @JvmStatic
        fun deserialize(descriptor: String): ISerializable {
            val idx = descriptor.indexOf("->")
            return when {
                idx == -1 -> DexClass(descriptor)
                descriptor.indexOf(":", idx + 1) == -1 -> DexMethod(descriptor)
                else -> DexField(descriptor)
            }
        }
    }

    fun serialize(): String {
        return this.toString()
    }
}