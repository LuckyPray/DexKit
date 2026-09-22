package org.luckypray.dexkit

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.result.UsingNumberData
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(DexKitExperimentalApi::class)
@RunWith(Parameterized::class)
class UsingNumbersDataTest(private val fullCache: Boolean) {
    companion object {
        init { loadLibrary("dexkit") }

        @JvmStatic
        @Parameterized.Parameters(name = "fullCache={0}")
        fun cacheModes() = listOf(arrayOf(false), arrayOf(true))
    }

    private fun bridge(): DexKitBridge = DexKitBridge.create(arrayOf(UsingNumbersFixture.dexBytes())).also {
        if (fullCache) it.initFullCache()
    }

    private fun DexKitBridge.method(methodName: String): MethodData = findMethod {
        matcher { declaredClass = "org.luckypray.dexkit.fixture.UsingNumbers"; name = methodName }
    }.single()

    private data class Expected(val name: String, val op: Int, val bits: Long, val width: Int = 32)

    @Test
    fun decodedBitsAndViewsAreExactAcrossEncodings() {
        val cases = listOf(
            Expected("int4Min", 0x12, 0xfffffff8L), Expected("int4Max", 0x12, 7),
            Expected("int16Min", 0x13, 0xffff8000L), Expected("int16Max", 0x13, 32767),
            Expected("intMin", 0x14, 0x80000000L), Expected("intMax", 0x14, 0x7fffffff),
            Expected("intHigh16Min", 0x15, 0x80000000L),
            Expected("wide16Min", 0x16, -32768, 64), Expected("wide16Max", 0x16, 32767, 64),
            Expected("wide32Min", 0x17, Int.MIN_VALUE.toLong(), 64),
            Expected("wide32Max", 0x17, Int.MAX_VALUE.toLong(), 64),
            Expected("wideMin", 0x18, Long.MIN_VALUE, 64), Expected("wideMax", 0x18, Long.MAX_VALUE, 64),
            Expected("wideHigh16Min", 0x19, Long.MIN_VALUE, 64),
            Expected("lit8Min", 0xd8, 0xffffff80L), Expected("lit16Min", 0xd0, 0xffff8000L),
            Expected("negative4", 0x12, 0xffffffffL), Expected("negative16", 0x13, 0xffffffffL),
            Expected("negativeWide16", 0x16, -1, 64), Expected("negativeWide32", 0x17, -1, 64),
            Expected("zero4", 0x12, 0), Expected("zero16", 0x13, 0),
            Expected("zero32", 0x14, 0), Expected("zeroHigh16", 0x15, 0),
            Expected("zeroWide16", 0x16, 0, 64), Expected("zeroWide32", 0x17, 0, 64),
            Expected("zeroWide", 0x18, 0, 64), Expected("zeroWideHigh16", 0x19, 0, 64)
        )
        bridge().use { bridge ->
            for (expected in cases) {
                val value = bridge.method(expected.name).usingNumbers.single()
                assertEquals(expected.name, expected.op, value.opCode)
                assertEquals(expected.name, expected.width, value.bitWidth)
                assertEquals(expected.name, expected.bits, value.rawBits)
                if (expected.width == 32) {
                    assertEquals(expected.bits.toInt(), value.intValue())
                    assertEquals(expected.bits.toInt().toLong(), value.longValue())
                } else {
                    assertEquals(expected.bits, value.longValue())
                    assertThrows(IllegalStateException::class.java) { value.intValue() }
                    assertThrows(IllegalStateException::class.java) { value.floatValue() }
                }
            }
            for (name in listOf("floatOne", "floatHigh16One")) {
                val value = bridge.method(name).usingNumbers.single()
                assertEquals(0x3f800000L, value.rawBits)
                assertEquals(1065353216, value.intValue())
                assertEquals(1.0f, value.floatValue(), 0.0f)
                assertEquals(1.0, value.doubleValue(), 0.0)
            }
            for (name in listOf("doubleOne", "doubleHigh16One")) {
                val value = bridge.method(name).usingNumbers.single()
                assertEquals(0x3ff0000000000000L, value.rawBits)
                assertEquals(1.0, value.doubleValue(), 0.0)
            }
            val wide32 = bridge.method("wide32FloatBits").usingNumbers.single()
            assertEquals(64, wide32.bitWidth)
            assertEquals(0x3f800000L, wide32.rawBits)
            assertEquals(0x3f800000L, wide32.doubleValue().toRawBits())
            for (name in listOf("tiny4", "tiny16")) {
                val value = bridge.method(name).usingNumbers.single()
                assertEquals(1L, value.rawBits)
                assertEquals(1, value.floatValue().toRawBits())
                assertEquals(Float.MIN_VALUE.toDouble(), value.doubleValue(), 0.0)
            }
            val tinyWide = bridge.method("tinyWide16").usingNumbers.single()
            assertEquals(1L, tinyWide.rawBits)
            assertEquals(1L, tinyWide.doubleValue().toRawBits())
            for (name in listOf("lit8Zero", "lit16Zero", "lit8Min", "lit16Min")) {
                val value = bridge.method(name).usingNumbers.single()
                assertThrows(IllegalStateException::class.java) { value.floatValue() }
                assertThrows(IllegalStateException::class.java) { value.doubleValue() }
            }
        }
    }

    @Test
    fun orderDuplicatesAndRecordEqualityArePreserved() = bridge().use { bridge ->
        val method = bridge.method("mixed")
        val values = method.usingNumbers
        assertEquals(listOf(0x12, 0xd8, 0x12, 0x13, 0x16, 0x13), values.map { it.opCode })
        assertEquals(listOf(1L, 0xffffffffL, 1L, 1L, -1L, 0L), values.map { it.rawBits })
        assertEquals(listOf(32, 32, 32, 32, 64, 32), values.map { it.bitWidth })
        assertEquals(values[0], values[2])
        assertEquals(values[0].hashCode(), values[2].hashCode())
        assertNotEquals(values[0], values[3])
        assertSame(values, method.usingNumbers)
        bridge.initFullCache()
        assertEquals(values, bridge.method("mixed").usingNumbers)
    }

    @Test
    fun specialValuesRetainTheirBits() = bridge().use { bridge ->
        val zeroFloat = bridge.method("negativeZeroFloat").usingNumbers.single()
        val zeroDouble = bridge.method("negativeZeroDouble").usingNumbers.single()
        assertEquals(0x80000000L, zeroFloat.rawBits)
        assertEquals(Int.MIN_VALUE, zeroFloat.floatValue().toRawBits())
        assertEquals(Long.MIN_VALUE, zeroFloat.doubleValue().toRawBits())
        assertEquals(Long.MIN_VALUE, zeroDouble.rawBits)
        assertEquals(Long.MIN_VALUE, zeroDouble.doubleValue().toRawBits())
        assertNotEquals(zeroFloat, bridge.method("zeroHigh16").usingNumbers.single())
        assertEquals(Double.POSITIVE_INFINITY,
            bridge.method("positiveInfinityFloat").usingNumbers.single().doubleValue(), 0.0)
        assertEquals(Double.NEGATIVE_INFINITY,
            bridge.method("negativeInfinityDouble").usingNumbers.single().doubleValue(), 0.0)
        for ((prefix, bits) in listOf("nanFloat" to 0x7fc00001L, "nanDouble" to 0x7ff8000000000001L)) {
            val one = bridge.method(prefix + "One").usingNumbers.single()
            val two = bridge.method(prefix + "Two").usingNumbers.single()
            assertEquals(bits, one.rawBits)
            assertEquals(bits + 1, two.rawBits)
            assertTrue(one.doubleValue().isNaN())
            assertTrue(two.doubleValue().isNaN())
            assertNotEquals(one, two)
            assertEquals(one, UsingNumberData.from(bits, one.opCode))
            assertEquals(one.hashCode(), UsingNumberData.from(bits, one.opCode).hashCode())
        }
    }

    @Test
    fun emptyMethodsAndPayloadsHaveNoNumericOperands() = bridge().use { bridge ->
        for (name in listOf("emptyAbstract", "emptyNative", "noNumbers", "payloads")) {
            assertTrue(name, bridge.method(name).usingNumbers.isEmpty())
        }
    }

    @Test
    fun snapshotsOutliveCloseButUninitializedPropertiesFail() {
        val bridge = bridge()
        try {
            val read = bridge.method("mixed")
            val unread = bridge.method("floatOne")
            val values = read.usingNumbers
            bridge.close()
            assertSame(values, read.usingNumbers)
            assertEquals(-1L, values[4].longValue())
            assertThrows(UnsupportedOperationException::class.java) {
                (values as MutableList<UsingNumberData>).clear()
            }
            val error = assertThrows(IllegalStateException::class.java) { unread.usingNumbers }
            assertEquals("DexKitBridge is not valid", error.message)
        } finally {
            bridge.close()
        }
    }

    @Test
    fun concurrentFirstReadsAndFullWarmupReturnCompleteSnapshots() = bridge().use { bridge ->
        // Distinct MethodData instances also exercise the native lazy slot, beyond JVM lazy.
        val methods = List(8) { bridge.method("mixed") }
        val pool = Executors.newFixedThreadPool(9)
        val start = CountDownLatch(1)
        try {
            val reads = methods.map { method ->
                pool.submit<List<UsingNumberData>> { start.await(); method.usingNumbers }
            }
            val warmup = pool.submit { start.await(); bridge.initFullCache() }
            start.countDown()
            val results = reads.map { it.get(10, TimeUnit.SECONDS) }
            warmup.get(10, TimeUnit.SECONDS)
            assertEquals(6, results.first().size)
            results.forEach { assertEquals(results.first(), it) }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun concurrentCloseEitherReturnsSnapshotOrReportsClosedBridge() {
        val bridge = bridge()
        val method = bridge.method("mixed")
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val read = pool.submit<List<UsingNumberData>?> {
                start.await()
                try { method.usingNumbers } catch (error: IllegalStateException) {
                    assertEquals("DexKitBridge is not valid", error.message)
                    null
                }
            }
            val close = pool.submit { start.await(); bridge.close() }
            start.countDown()
            val values = read.get(10, TimeUnit.SECONDS)
            close.get(10, TimeUnit.SECONDS)
            if (values != null) {
                assertEquals(6, values.size)
                assertEquals(-1L, values[4].longValue())
            }
        } finally {
            pool.shutdownNow()
            bridge.close()
        }
    }

    @Test
    fun matchingLocalIdsInDifferentDexFilesStaySeparate() {
        fun dex(name: String, value: Int) = UsingNumbersFixture.assemble("""
            .class public Lfixture/$name;
            .super Ljava/lang/Object;
            .method public static number()I
                .registers 1
                const/16 v0, $value
                return v0
            .end method
        """.trimIndent())
        DexKitBridge.create(arrayOf(dex("First", 123), dex("Second", 456))).use { bridge ->
            if (fullCache) bridge.initFullCache()
            val first = bridge.getMethodData("Lfixture/First;->number()I")!!
            val second = bridge.getMethodData("Lfixture/Second;->number()I")!!
            assertEquals(first.getEncodeId() and 0xffffffffL, second.getEncodeId() and 0xffffffffL)
            assertNotEquals(first.getEncodeId(), second.getEncodeId())
            assertEquals(123, first.usingNumbers.single().intValue())
            assertEquals(456, second.usingNumbers.single().intValue())
        }
    }

    @Test
    fun longListsSerializeWithoutLosingOrder() {
        val source = buildString {
            append(".class public Lfixture/LongList;\n.super Ljava/lang/Object;\n")
            append(".method public static numbers()V\n.registers 1\n")
            repeat(4096) { append("const v0, $it\n") }
            append("return-void\n.end method\n")
        }
        DexKitBridge.create(arrayOf(UsingNumbersFixture.assemble(source))).use { bridge ->
            if (fullCache) bridge.initFullCache()
            val numbers = bridge.getMethodData("Lfixture/LongList;->numbers()V")!!.usingNumbers
            assertEquals((0L until 4096L).toList(), numbers.map { it.rawBits })
            assertTrue(numbers.all { it.opCode == 0x14 && it.bitWidth == 32 })
        }
    }

    @Test
    fun invalidWireRecordsAreRejected() {
        for (opcode in listOf(-1, 0, 0x1a, 0xcf, 0xe3, 256)) {
            assertThrows(IllegalArgumentException::class.java) { UsingNumberData.from(0, opcode) }
        }
        assertThrows(IllegalArgumentException::class.java) { UsingNumberData.from(-1, 0x12) }
        assertThrows(IllegalArgumentException::class.java) { UsingNumberData.from(1L shl 32, 0xd8) }
    }
}
