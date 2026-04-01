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
package org.luckypray.dexkit.cache

import org.luckypray.dexkit.query.base.BaseFinder

internal object CacheBridgeKeys {
    private val hexDigits = "0123456789ABCDEF".toCharArray()

    private fun isReserved(ch: Char): Boolean {
        return ch == ':' || ch == '%'
    }

    fun encodeSegment(raw: String): String {
        val bytes = raw.toByteArray(Charsets.UTF_8)
        val out = StringBuilder(bytes.size)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            val ch = value.toChar()
            if (isReserved(ch)) {
                out.append('%')
                out.append(hexDigits[value ushr 4])
                out.append(hexDigits[value and 0x0F])
            } else {
                out.append(ch)
            }
        }
        return out.toString()
    }

    fun cachePrefixOf(appTag: String): String {
        return "dkcb:${encodeSegment(appTag)}"
    }

    fun mapGroupsKey(cacheKey: String): String = "$cacheKey:meta:groups"

    fun mapGroupKey(cacheKey: String, groupKey: String): String {
        return "$cacheKey:group:${encodeSegment(groupKey)}"
    }

    fun cacheKeyOf(appTag: String, kind: String, key: String?, query: BaseFinder? = null): String {
        val prefix = "${cachePrefixOf(appTag)}:$kind"
        if (key != null) {
            return "$prefix:user:${encodeSegment(key)}"
        }
        requireNotNull(query) {
            "Either key or query must be provided for auto-generated cache key."
        }
        return "$prefix:auto:${encodeSegment(query.hashKey())}"
    }
}
