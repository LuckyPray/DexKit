package org.luckypray.dexkit

import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ParameterAnnotationsTest {
    companion object {
        init { loadLibrary("dexkit") }

        private val fixture by lazy {
            val directory = Files.createTempDirectory("dexkit-parameter-annotations").toFile()
            try {
                val source = directory.resolve("ParameterAnnotations.smali")
                ParameterAnnotationsTest::class.java
                    .getResourceAsStream("/ParameterAnnotations.smali")!!.use {
                        source.writeBytes(it.readBytes())
                    }
                val output = directory.resolve("classes.dex")
                val options = SmaliOptions().apply {
                    apiLevel = 21
                    jobs = 1
                    outputDexFile = output.absolutePath
                }
                assertTrue("Assemble parameter annotation fixture", Smali.assemble(options, source.absolutePath))
                output.readBytes()
            } finally {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun emptySlotsStayInPlaceForLazyAndPreloadedAnnotations() {
        val empty = emptyList<String>()
        val marker = listOf("test.Marker")
        val cases = mapOf(
            "leadingAndTrailing(IIII)V" to listOf(empty, empty, marker, empty),
            "middle(IIII)V" to listOf(marker, empty, empty, marker),
            "wide(JID)V" to listOf(empty, marker, empty),
            "abstractMethod(IIII)V" to listOf(empty, empty, marker, empty)
        )
        for (preload in listOf(false, true)) {
            DexKitBridge.create(arrayOf(fixture)).use { bridge ->
                if (preload) bridge.initFullCache()
                for ((method, expected) in cases) {
                    val data = bridge.getMethodData("Ltest/ParameterAnnotations;->$method")!!
                    assertEquals("$method, preload=$preload", expected,
                        data.paramAnnotations.map { slot -> slot.map { it.typeName } })
                }
            }
        }
    }
}
