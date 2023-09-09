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
    fun testPackages() {
        bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            excludePackages("org.luckypray.dexkit.demo.annotations")
        }.forEach {
            println(it.className)
        }
    }

    @Test
    fun testGetParameterNames() {
        bridge.findMethod {
            matcher {
                declaredClass("org.luckypray.dexkit.demo.PlayActivity")
            }
        }.forEach {
            println("${it.dexId} ${it.id} ${it.dexDescriptor}")
            val paramNames = it.getParameterNames()
            println("paramNames: ${paramNames?.joinToString(",")}")
        }
    }

    @Test
    fun testAnnotationSearch() {
        val res = bridge.findClass {
            matcher {
                annotations {
                    add {
                        this.type("Router", StringMatchType.EndsWith)
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
            assert(annotation.first().className == "org.luckypray.dexkit.demo.annotations.Router")
        }
    }

    @Test
    fun testAnnotationForValue() {
        val res = bridge.findClass {
            matcher {
                addAnnotation {
                    type("Router", StringMatchType.EndsWith)
                    addElement {
                        name = "path"
                        value {
                            stringValue("/main", StringMatchType.Equals)
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
                    count(3)
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
                usingStrings("PlayActivity")
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
                usingStrings("PlayActivity", "onClick")
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
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                superClass("AppCompatActivity", StringMatchType.EndsWith)
                interfaces {
                    add("android.view.View\$OnClickListener")
                    count(1)
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
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                superClass("appcompatactivity", StringMatchType.EndsWith, true)
                interfaces {
                    add("android.view.View\$OnClicklistener", StringMatchType.Equals, true)
                    count(1)
                }
            }
        }
        println(res)
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.MainActivity")
    }

    @Test
    fun testIntNumberSearch() {
        val res = bridge.findMethod {
            matcher {
                usingNumbers {
                    add {
                        intValue(114514)
                    }
                }
            }
        }
        println(res)
        assert(res.size == 2)
    }

    @Test
    fun testIntAndFloatNumberSearch() {
        val res = bridge.findMethod {
            matcher {
                usingNumbers {
                    add {
                        floatValue(0.987f)
                    }
                    add {
                        intValue(114514)
                    }
                }
            }
        }
        println(res)
        assert(res.size == 1)
    }

    @Test
    fun testGetClassData() {
        val res = bridge.getClassData("Lorg/luckypray/dexkit/demo/MainActivity;")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        res.getMethods().forEach {
            println(it.dexDescriptor)
        }
    }

    @Test
    fun testGetConstructorData() {
        val res = bridge.getMethodData("Lorg/luckypray/dexkit/demo/MainActivity;-><init>()V")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.methodName == "<init>")
        assert(res.methodSign == "()V")
        assert(res.isConstructor)
    }

    @Test
    fun testGetMethodData() {
        val res = bridge.getMethodData("Lorg/luckypray/dexkit/demo/MainActivity;->onClick(Landroid/view/View;)V")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.methodName == "onClick")
        assert(res.methodSign == "(Landroid/view/View;)V")
        assert(res.isMethod)
    }

    @Test
    fun testGetFieldData() {
        val res = bridge.getFieldData("Lorg/luckypray/dexkit/demo/MainActivity;->TAG:Ljava/lang/String;")
        assert(res != null)
        assert(res!!.className == "org.luckypray.dexkit.demo.MainActivity")
        assert(res.fieldName == "TAG")
        assert(res.typeName == "java.lang.String")
    }

    @Test
    fun testMethodUsingNumbers() {
        val res = bridge.findMethod {
            matcher {
                usingNumbers(0, -1, 0.01, 0.987, 114514)
            }
        }
        assert(res.size == 1)
        assert(res.first().className == "org.luckypray.dexkit.demo.PlayActivity")
    }
}