package io.luckypray.dexkit

import io.luckypray.dexkit.util.loadLibrary
import org.junit.Test
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.lang.reflect.Modifier


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
                        this.typeName("Router", StringMatchType.EndsWith)
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
                    typeName("Router", StringMatchType.EndsWith)
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
            searchPackages("org.luckypray.dexkit.demo")
            matcher {
                superClass("appcompatactivity", StringMatchType.EndsWith, true)
                interfaces {
                    add("android.view.View\$OnClicklistener", StringMatchType.Equals, true)
                    countRange(1)
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
    fun test() {
        bridge.findClass {
            // Search within the specified package name range
            searchPackages("org.luckypray.dexkit.demo")
            // ClassMatcher for class matching
            matcher {
                className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher for matching properties within the class
                fields {
                    // Add a matcher for properties
                    add {
                        modifiers(Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL)
                        type("java.lang.String")
                        name("TAG")
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // Specify the number of properties in the class
                    countRange(count = 3)
                }
                // MethodsMatcher for matching methods within the class
                methods {
                    // Add a matcher for methods
                    add {
                        modifiers(Modifier.PROTECTED)
                        name("onCreate")
                        returnType("void")
                        parameterTypes("android.os.Bundle")
                        usingStrings("onCreate")
                    }
                    add {
                        parameterTypes("android.view.View")
                        usingNumbers {
                            add {
                                intValue(114514)
                            }
                            add {
                                floatValue(0.987f)
                            }
                        }
                    }
                    add {
                        modifiers(Modifier.PUBLIC)
                        parameterTypes("boolean")
                    }
                    // Specify the number of methods in the class, a minimum of 4, and a maximum of 10
                    countRange(1..10)
                }
                // AnnotationsMatcher for matching interfaces within the class
                annotations {
                    add {
                        typeName("Router", StringMatchType.EndsWith)
                        elements {
                            add {
                                name("path")
                                matcher {
                                    stringValue("/play")
                                }
                            }
                        }
                    }
                }
                // Strings used by all methods in the class
                usingStrings("PlayActivity", "onClick", "onCreate")
            }
        }.forEach {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            println(it.className)
            // Get the corresponding class instance
//            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
    }

}