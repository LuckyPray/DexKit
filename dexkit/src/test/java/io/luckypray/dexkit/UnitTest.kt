package io.luckypray.dexkit

import io.luckypray.dexkit.util.loadLibrary
import org.junit.Test
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File


class UnitTest {

    companion object {

        private var bridge: DexKitBridge

        init {
            loadLibrary("dexkit")
            val path = System.getProperty("apk.path")
            val demoApk = File(path, "demo.apk")
            bridge = DexKitBridge.create(demoApk.absolutePath)!!
        }
    }

    @Test
    fun testGetDexNum() {
        assert(bridge.getDexNum() > 0)
    }

    @Test
    fun testAnnotationSearch() {
        val res = bridge.findClass {
            matcher {
                annotations {
                    add {
                        this.typeName("Router", StringMatchType.EndWith)
                    }
                }
            }
        }
        println(res.map { it.className })
        assert(res.size == 2)
        res.forEach {
            assert(it.className.startsWith("org.luckypray.dexkit.demo"))
            assert(it.className.endsWith("Activity"))
            val annotation = it.getAnnotations()
            assert(annotation.size == 1)
            assert(annotation.first().typeDescriptor == "Lorg/luckypray/dexkit/demo/annotations/Router;")
        }
    }

    @Test
    fun testAnnotationForValue() {
        val res = bridge.findClass {
            matcher {
                addAnnotation {
                    typeName("Router", StringMatchType.EndWith)
                    elements {
                        add {
                            name("path")
                            matcher {
                                stringValue("/main")
                            }
                        }
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val mainActivity = res.first()
        assert(mainActivity.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(mainActivity.getSuperClass()!!.className == "androidx.appcompat.app.AppCompatActivity")
        assert(mainActivity.getInterfaces().size == 1)
    }

}