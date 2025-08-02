package org.luckypray.dexkit.query.base

import com.google.flatbuffers.FlatBufferBuilder

abstract class BaseFinder : BaseMatcher() {

    @JvmSynthetic
    internal fun serializedBytes(): ByteArray {
        val fbb = FlatBufferBuilder()
        build(fbb)
        return fbb.sizedByteArray()
    }

    override fun hashCode(): Int {
        val data = serializedBytes()
        var hash = 0x811C9DC5.toInt()
        for (b in data) {
            hash = hash xor (b.toInt() and 0xFF)
            hash *= 0x01000193
        }
        return hash
    }

    fun hashKey(): String {
        val data = serializedBytes()
        var hash = 0xcbf29ce484222325uL
        for (b in data) {
            hash = hash xor (b.toUByte().toULong())
            hash *= 0x100000001b3uL
        }
        return hash.toString(16).padStart(16, '0')
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as BaseFinder
        return serializedBytes().contentEquals(other.serializedBytes())
    }
}