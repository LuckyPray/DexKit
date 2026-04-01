package org.luckypray.dexkit

import org.luckypray.dexkit.query.base.BaseFinder

internal object DexKitCacheBridgeKeys {
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
