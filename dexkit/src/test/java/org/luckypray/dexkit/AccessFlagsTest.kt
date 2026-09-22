package org.luckypray.dexkit

import org.junit.Assert.*
import org.junit.Test
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.testflags.AccessFlagsFixture
import org.luckypray.dexkit.util.DexSignUtil
import java.io.File
import java.lang.reflect.Modifier

class AccessFlagsTest {
    companion object {
        init { loadLibrary("dexkit") }
        private val dexPath = System.getProperty("access.flags.dex.path")
        private const val fixture = "org.luckypray.dexkit.testflags.AccessFlagsFixture"
    }

    @Test
    fun resultsAndExactMatchersAgreeWithReflection() {
        DexKitBridge.create(arrayOf(File(dexPath).readBytes())).use { bridge ->
            for (type in AccessFlagsFixture.types()) {
                val clazz = bridge.getClassData(type)!!
                assertEquals(type.name, type.modifiers, clazz.modifiers)
                assertEquals(listOf(clazz.descriptor), bridge.findClass {
                    matcher {
                        descriptor = clazz.descriptor
                        if (type.modifiers != 0) modifiers(type.modifiers, MatchType.Equals)
                        if (clazz.accessFlags != 0) accessFlags(clazz.accessFlags, MatchType.Equals)
                    }
                }.map { it.descriptor })
                for (method in type.declaredMethods) {
                    val data = bridge.getMethodData(method)!!
                    // D8 emits only DEX SYNCHRONIZED for this native method. Android 15
                    // reflection returns PUBLIC | NATIVE; the JVM class file keeps SYNCHRONIZED.
                    val reflectedModifiers = if (method.name == "nativeSynchronizedMethod") {
                        Modifier.PUBLIC or Modifier.NATIVE
                    } else method.modifiers
                    assertEquals(method.toString(), reflectedModifiers, data.modifiers)
                    assertEquals(listOf(data.descriptor), bridge.findMethod {
                        matcher {
                            descriptor = data.descriptor
                            if (reflectedModifiers != 0) modifiers(reflectedModifiers, MatchType.Equals)
                            if (data.accessFlags != 0) accessFlags(data.accessFlags, MatchType.Equals)
                        }
                    }.map { it.descriptor })
                }
                for (constructor in type.declaredConstructors) {
                    val data = bridge.getMethodData(constructor)!!
                    assertEquals(constructor.toString(), constructor.modifiers, data.modifiers)
                    assertTrue(DexAccessFlags.isConstructor(data.accessFlags))
                    assertFalse(DexAccessFlags.isConstructor(data.modifiers))
                    assertEquals(listOf(data.descriptor), bridge.findMethod {
                        matcher {
                            descriptor = data.descriptor
                            if (constructor.modifiers != 0) modifiers(constructor.modifiers, MatchType.Equals)
                            accessFlags(DexAccessFlags.CONSTRUCTOR)
                        }
                    }.map { it.descriptor })
                }
                for (field in type.declaredFields) {
                    val data = bridge.getFieldData(field)!!
                    assertEquals(field.toString(), field.modifiers, data.modifiers)
                    assertEquals(listOf(data.descriptor), bridge.findField {
                        matcher {
                            descriptor = data.descriptor
                            if (field.modifiers != 0) modifiers(field.modifiers, MatchType.Equals)
                            if (data.accessFlags != 0) accessFlags(data.accessFlags, MatchType.Equals)
                        }
                    }.map { it.descriptor })
                }
            }
        }
    }

    @Test
    fun synchronizedFlagsRemainIndependent() {
        DexKitBridge.create(arrayOf(File(dexPath).readBytes())).use { bridge ->
            val normal = bridge.getMethodData(AccessFlagsFixture::class.java.getDeclaredMethod("synchronizedMethod"))!!
            val native = bridge.getMethodData(AccessFlagsFixture::class.java.getDeclaredMethod("nativeSynchronizedMethod"))!!
            assertTrue(Modifier.isSynchronized(normal.modifiers))
            assertEquals(Modifier.PUBLIC or Modifier.NATIVE, native.modifiers)
            assertTrue(DexAccessFlags.isDeclaredSynchronized(normal.accessFlags))
            assertFalse(DexAccessFlags.isDeclaredSynchronized(native.accessFlags))
            assertFalse(DexAccessFlags.isSynchronized(normal.accessFlags))
            assertTrue(DexAccessFlags.isSynchronized(native.accessFlags))
            val javaMatches = bridge.findMethod {
                matcher { declaredClass = fixture; modifiers = Modifier.SYNCHRONIZED }
            }
            assertEquals(setOf("synchronizedMethod", "staticSynchronizedMethod"), javaMatches.map { it.name }.toSet())
            assertEquals(listOf("nativeSynchronizedMethod"), bridge.findMethod {
                matcher { declaredClass = fixture; accessFlags = DexAccessFlags.SYNCHRONIZED }
            }.map { it.name })
            assertTrue(bridge.findMethod {
                matcher {
                    descriptor = normal.descriptor
                    modifiers = Modifier.SYNCHRONIZED
                    accessFlags = DexAccessFlags.SYNCHRONIZED
                }
            }.isEmpty())
        }
    }

    @Test
    fun hiddenJavaBitsArePreserved() {
        DexKitBridge.create(arrayOf(File(dexPath).readBytes())).use { bridge ->
            val bridgeMethods = bridge.findMethod {
                matcher {
                    declaredClass = "$fixture\$Bridge"
                    modifiers = DexAccessFlags.BRIDGE or DexAccessFlags.SYNTHETIC
                    accessFlags = DexAccessFlags.BRIDGE or DexAccessFlags.SYNTHETIC
                }
            }
            assertEquals(1, bridgeMethods.size)
            val method = bridgeMethods.single()
            assertTrue(DexAccessFlags.isBridge(method.modifiers))
            assertTrue(DexAccessFlags.isSynthetic(method.modifiers))
            val varargs = bridge.findMethod {
                matcher { declaredClass = fixture; modifiers = DexAccessFlags.VARARGS }
            }.single()
            assertEquals("varargsMethod", varargs.name)
            assertTrue(DexAccessFlags.isVarArgs(varargs.accessFlags))
            val syntheticField = bridge.findField {
                matcher { declaredClass = "$fixture\$Inner"; modifiers = DexAccessFlags.SYNTHETIC }
            }.single()
            assertTrue(DexAccessFlags.isSynthetic(syntheticField.accessFlags))
        }
    }

    @Test
    fun innerClassModifiersUseAnnotationWithoutChangingRawFlags() {
        DexKitBridge.create(arrayOf(File(dexPath).readBytes())).use { bridge ->
            val type = AccessFlagsFixture.types().single { it.simpleName == "PrivateNested" }
            val data = bridge.getClassData(type)!!
            assertTrue(Modifier.isPrivate(data.modifiers))
            assertTrue(Modifier.isStatic(data.modifiers))
            assertFalse(Modifier.isPrivate(data.accessFlags))
            assertFalse(Modifier.isStatic(data.accessFlags))
            assertEquals(listOf(data.descriptor), bridge.findClass {
                matcher {
                    descriptor = data.descriptor
                    modifiers = Modifier.PRIVATE or Modifier.STATIC
                    accessFlags = Modifier.FINAL
                }
            }.map { it.descriptor })
            assertTrue(bridge.findClass {
                matcher { descriptor = data.descriptor; accessFlags = Modifier.PRIVATE }
            }.isEmpty())
            bridge.initFullCache()
            assertEquals(data.modifiers, bridge.getClassData(type)!!.modifiers)
        }
    }

    @Test
    fun compositeAndNestedMatchersHonorBothFlagKinds() {
        DexKitBridge.create(arrayOf(File(dexPath).readBytes())).use { bridge ->
            assertEquals(setOf("synchronizedMethod", "staticSynchronizedMethod"), bridge.findMethod {
                matcher {
                    declaredClass = fixture
                    allOf { match { modifiers = Modifier.SYNCHRONIZED } }
                    noneOf { match { accessFlags = DexAccessFlags.NATIVE } }
                }
            }.map { it.name }.toSet())
            val fieldDescriptor = DexSignUtil.getFieldDescriptor(AccessFlagsFixture::class.java.getDeclaredField("visible"))
            val using = bridge.findMethod {
                matcher {
                    declaredClass = fixture
                    name = "readVisible"
                    addUsingField {
                        descriptor = fieldDescriptor
                        modifiers = Modifier.VOLATILE
                        accessFlags = DexAccessFlags.VOLATILE
                    }
                }
            }
            assertEquals(listOf("readVisible"), using.map { it.name })
        }
    }
}
