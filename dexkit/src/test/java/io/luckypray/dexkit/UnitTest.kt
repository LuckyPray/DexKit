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

    @Test
    fun testClassFieldsSearch() {
        val res = bridge.findClass {
            matcher {
                fields {
                    addForType("java.lang.String")
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    countRange(3)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        val playActivity = res.first()
        val fields = playActivity.getFields()
        assert(fields.size == 3)
        assert(playActivity.className == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassUsingString() {
        val res = bridge.findClass {
            matcher {
                useStrings("PlayActivity")
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassUsingStringArray() {
        val res = bridge.findClass {
            matcher {
                useStrings("PlayActivity", "onClick")
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.PlayActivity")
    }

    @Test
    fun testFindClassSuper() {
        val res = bridge.findClass {
            matcher {
                superClass("androidx.appcompat.app.AppCompatActivity")
            }
        }
        println(res)
        assert(res.size == 2)
        res.map { it.className }.forEach {
            assert(it.startsWith("org.luckypray.dexkit.demo"))
            assert(it.endsWith("Activity"))
        }
    }

    @Test
    fun testFindClassImpl() {
        val res = bridge.findClass {
            searchPackage("org.luckypray.dexkit.demo")
            matcher {
                superClass("AppCompatActivity", StringMatchType.EndWith)
                interfaces {
                    add("android.view.View\$OnClickListener")
                    countRange(1)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.MainActivity")
    }

    @Test
    fun testFindClassImplIgnoreCase() {
        val res = bridge.findClass {
            searchPackage("org.luckypray.dexkit.demo")
            matcher {
                superClass("appcompatactivity", StringMatchType.EndWith, true)
                interfaces {
                    add("android.view.View\$OnClicklistener", StringMatchType.Equal, true)
                    countRange(1)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.MainActivity")
    }

}