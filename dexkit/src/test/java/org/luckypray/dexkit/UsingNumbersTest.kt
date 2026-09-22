package org.luckypray.dexkit

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UsingNumbersTest(private val fullCache: Boolean) {
    companion object {
        init { loadLibrary("dexkit") }

        @JvmStatic
        @Parameterized.Parameters(name = "fullCache={0}")
        fun cacheModes() = listOf(arrayOf(false), arrayOf(true))

    }

    private fun withBridge(block: (DexKitBridge) -> Unit) {
        DexKitBridge.create(arrayOf(UsingNumbersFixture.dexBytes())).use { bridge ->
            if (fullCache) bridge.initFullCache()
            block(bridge)
        }
    }

    private fun DexKitBridge.assertMatches(method: String, value: Number, expected: Boolean = true) {
        val names = findMethod {
            matcher {
                declaredClass = "org.luckypray.dexkit.fixture.UsingNumbers"
                name = method
                usingNumbers(value)
            }
        }.map { it.name }
        assertEquals("$method using $value (${value.javaClass.simpleName}), fullCache=$fullCache",
            if (expected) listOf(method) else emptyList<String>(), names)
    }

    @Test
    fun integerValuesSurviveAllConstantEncodings() = withBridge { bridge ->
        val cases = listOf(
            "int4Min" to -8L, "int4Max" to 7L,
            "int16Min" to -32768L, "int16Max" to 32767L,
            "intMin" to Int.MIN_VALUE.toLong(), "intMax" to Int.MAX_VALUE.toLong(),
            "intHigh16Min" to Int.MIN_VALUE.toLong(),
            "wide16Min" to -32768L, "wide16Max" to 32767L,
            "wide32Min" to Int.MIN_VALUE.toLong(), "wide32Max" to Int.MAX_VALUE.toLong(),
            "wideMin" to Long.MIN_VALUE, "wideMax" to Long.MAX_VALUE,
            "wideHigh16Min" to Long.MIN_VALUE,
            "lit8Min" to -128L, "lit16Min" to -32768L
        )
        for ((method, value) in cases) {
            bridge.assertMatches(method, value)
            bridge.assertMatches(method, value xor 1L, false)
            if (value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                bridge.assertMatches(method, value.toInt())
            }
        }
        bridge.assertMatches("int4Max", 7.toByte())
        bridge.assertMatches("wide16Min", (-32768).toShort())
        bridge.assertMatches("floatOne", 0x3f800000)
        bridge.assertMatches("doubleOne", 0x3ff0000000000000L)
    }

    @Test
    fun floatingZeroMatchesEveryConstantEncoding() = withBridge { bridge ->
        for (method in listOf("zero4", "zero16", "zero32", "zeroHigh16",
            "zeroWide16", "zeroWide32", "zeroWide", "zeroWideHigh16")) {
            bridge.assertMatches(method, 0.0f)
            bridge.assertMatches(method, 0.0)
            bridge.assertMatches(method, 0)
            bridge.assertMatches(method, 1.0, false)
        }
    }

    @Test
    fun floatingInterpretationUsesInstructionWidth() = withBridge { bridge ->
        for (method in listOf("floatOne", "floatHigh16One", "doubleOne", "doubleHigh16One")) {
            bridge.assertMatches(method, 1.0f)
            bridge.assertMatches(method, 1.0)
            bridge.assertMatches(method, 1.0 + 5e-7)
            bridge.assertMatches(method, 1.0 + 2e-6, false)
        }
        bridge.assertMatches("wide32FloatBits", Double.fromBits(0x3f800000))
        bridge.assertMatches("wide32FloatBits", 1.0f, false)
        bridge.assertMatches("wide32FloatBits", 1.0, false)
        bridge.assertMatches("wide32FloatBits", 0x3f800000L)
        bridge.assertMatches("wide32FloatBits", 0L, false)
        // Negative narrow literals must sign-extend to NaN bits, not zero-extend to subnormals.
        for (method in listOf("negative4", "negative16", "negativeWide16", "negativeWide32")) {
            bridge.assertMatches(method, -1)
            bridge.assertMatches(method, 0.0, false)
        }
        bridge.assertMatches("tiny4", Float.fromBits(1))
        bridge.assertMatches("tiny16", Float.fromBits(1))
        bridge.assertMatches("tinyWide16", Double.fromBits(1))
        // Floating tolerance alone cannot distinguish these subnormals from zero.
        for (method in listOf("tiny4", "tiny16", "tinyWide16")) {
            bridge.assertMatches(method, 1L)
            bridge.assertMatches(method, 0L, false)
        }
    }

    @Test
    fun arithmeticLiteralsOnlyMatchIntegerQueries() = withBridge { bridge ->
        for (method in listOf("lit8Zero", "lit16Zero")) {
            bridge.assertMatches(method, 0)
            bridge.assertMatches(method, 0L)
            bridge.assertMatches(method, 0.0f, false)
            bridge.assertMatches(method, 0.0, false)
        }
        bridge.assertMatches("lit8Min", -128.0, false)
        bridge.assertMatches("lit16Min", -32768.0, false)
    }

    @Test
    fun floatingSpecialValuesHaveNumericEqualitySemantics() = withBridge { bridge ->
        for (method in listOf("negativeZeroFloat", "negativeZeroDouble")) {
            bridge.assertMatches(method, 0.0)
            bridge.assertMatches(method, -0.0)
        }
        bridge.assertMatches("positiveInfinityFloat", Double.POSITIVE_INFINITY)
        bridge.assertMatches("positiveInfinityFloat", Double.NEGATIVE_INFINITY, false)
        bridge.assertMatches("negativeInfinityDouble", Float.NEGATIVE_INFINITY)
        bridge.assertMatches("negativeInfinityDouble", Double.POSITIVE_INFINITY, false)
        bridge.assertMatches("negative4", Float.NaN, false)
        bridge.assertMatches("negativeWide16", Double.NaN, false)
    }

    @Test
    fun lazyResultsRemainValidAfterFullCacheInitialization() = withBridge { bridge ->
        bridge.assertMatches("zero4", 0.0)
        bridge.assertMatches("zeroWide16", 0.0)
        bridge.initFullCache()
        bridge.assertMatches("zero4", 0.0)
        bridge.assertMatches("zeroWide16", 0.0)
        bridge.assertMatches("wide32FloatBits", 1.0, false)
        bridge.assertMatches("wideMax", Long.MAX_VALUE)
    }
}
