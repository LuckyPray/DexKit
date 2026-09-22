package org.luckypray.dexkit.result

import org.luckypray.dexkit.annotations.DexKitExperimentalApi

/**
 * An immutable numeric literal from a method's instructions, without source-type inference.
 *
 * [rawBits] contains the decoded bit pattern, not the instruction's encoded bytes. For
 * 32-bit values its upper 32 bits are zero; 64-bit values retain every bit, including
 * the sign bit. Arithmetic lit8/lit16 operands are sign-extended to 32 bits.
 *
 * Integer and floating views interpret the same bits differently. This class is not
 * [Number]: its accessors do not perform arbitrary numeric conversions. No matching
 * tolerance is applied. Exact NaN payloads are preserved by [rawBits]; floating
 * operations or promotion may quiet a signaling NaN on some platforms.
 *
 * Equality compares [opCode] and [rawBits] exactly, independently of the containing
 * method or occurrence. A returned instance remains usable after the bridge closes.
 */
@DexKitExperimentalApi
class UsingNumberData private constructor(
    val rawBits: Long,
    /** Source DEX opcode (0..255). Only const* and integer lit8/lit16 instructions occur. */
    val opCode: Int
) {
    internal companion object `-Companion` {
        fun from(rawBits: Long, opCode: Int): UsingNumberData {
            require(opCode in 0x12..0x19 || opCode in 0xd0..0xe2) {
                "Invalid numeric literal opcode: $opCode"
            }
            require(opCode in 0x16..0x19 || rawBits ushr 32 == 0L) {
                "Nonzero upper bits in a 32-bit numeric literal: opcode=$opCode"
            }
            return UsingNumberData(rawBits, opCode)
        }
    }

    /** Decoded value width: 32 or 64. This is not the encoded immediate's bit count. */
    val bitWidth: Int get() = if (opCode in 0x16..0x19) 64 else 32

    /** Signed 32-bit view. Throws [IllegalStateException] for a 64-bit literal. */
    fun intValue(): Int {
        checkView(bitWidth == 32, "int")
        return rawBits.toInt()
    }

    /** Signed integer view; a 32-bit value is sign-extended without loss. */
    fun longValue(): Long = if (bitWidth == 64) rawBits else rawBits.toInt().toLong()

    /** Float bit view of a 32-bit const. Other opcodes throw [IllegalStateException]. */
    fun floatValue(): Float {
        checkView(opCode in 0x12..0x15, "float")
        return Float.fromBits(rawBits.toInt())
    }

    /**
     * Double bit view of a 64-bit const, or the decoded float view promoted to double.
     * Arithmetic lit8/lit16 operands throw [IllegalStateException].
     */
    fun doubleValue(): Double {
        checkView(opCode in 0x12..0x19, "double")
        return if (bitWidth == 64) Double.fromBits(rawBits) else Float.fromBits(rawBits.toInt()).toDouble()
    }

    private fun checkView(allowed: Boolean, view: String) {
        check(allowed) { "Cannot read $view view of opcode 0x${opCode.toString(16)} ($bitWidth-bit literal)" }
    }

    override fun equals(other: Any?): Boolean =
        other is UsingNumberData && opCode == other.opCode && rawBits == other.rawBits

    override fun hashCode(): Int = 31 * rawBits.hashCode() + opCode

    override fun toString(): String =
        "UsingNumberData(opCode=0x${opCode.toString(16)}, bitWidth=$bitWidth, " +
            "rawBits=0x${java.lang.Long.toHexString(rawBits).padStart(bitWidth / 4, '0')})"
}
