package org.luckypray.dexkit

import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import org.junit.Assert.assertTrue
import java.nio.file.Files

object UsingNumbersFixture {
    // Assemble exact encodings: D8 may otherwise replace a const with a shorter form.
    private val bytes by lazy {
        assemble(UsingNumbersFixture::class.java.getResourceAsStream("/UsingNumbers.smali")!!.use {
            it.bufferedReader().readText()
        })
    }

    @JvmStatic
    fun dexBytes(): ByteArray = bytes

    fun assemble(source: String): ByteArray {
        val directory = Files.createTempDirectory("dexkit-using-numbers").toFile()
        try {
            val input = directory.resolve("UsingNumbers.smali").apply { writeText(source) }
            val dex = directory.resolve("classes.dex")
            val options = SmaliOptions().apply {
                apiLevel = 21
                jobs = 1
                outputDexFile = dex.absolutePath
            }
            assertTrue("Assemble numeric instruction fixture", Smali.assemble(options, input.absolutePath))
            return dex.readBytes()
        } finally {
            directory.deleteRecursively()
        }
    }
}
