package org.luckypray.dexkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import dalvik.system.PathClassLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class DexMemoryContainerTest {
    @Before
    fun loadLibrary() {
        System.loadLibrary("dexkit")
    }

    @Test
    @SdkSuppress(minSdkVersion = 36)
    fun sharedContainerIsLoadedOnce() {
        assertMemoryDex("dex041-container.dex", 2)
    }

    @Test
    fun standardDexIsLoaded() {
        assertMemoryDex("dex039.dex", 1)
    }

    private fun assertMemoryDex(asset: String, expectedDexCount: Int) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bytes = instrumentation.context.assets.open(asset).use { it.readBytes() }
        val fixture = File.createTempFile("dexkit-container-", ".jar", instrumentation.targetContext.codeCacheDir)
        try {
            ZipOutputStream(fixture.outputStream()).use {
                it.putNextEntry(ZipEntry("classes.dex"))
                it.write(bytes)
                it.closeEntry()
            }
            assertTrue(fixture.setReadOnly())
            val loader = PathClassLoader(fixture.path, javaClass.classLoader)
            assertTrue(fixture.delete())
            DexKitBridge.create(loader, true).use { bridge ->
                assertEquals(expectedDexCount, bridge.getDexNum())
                val classes = bridge.findClass { searchPackages("dex041") }
                assertEquals(setOf("dex041.First", "dex041.Second"), classes.map { it.name }.toSet())
                for ((name, result) in listOf("first" to 1, "second" to 2)) {
                    val methods = bridge.findMethod { matcher { name(name) } }
                    assertEquals(1, methods.size)
                    assertEquals(result, methods.single().getMethodInstance(loader).invoke(null))
                }
            }
        } finally {
            fixture.delete()
        }
    }
}
