package org.luckypray.dexkit

import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

class KtReadMeTest {

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
    fun readmeEnTest() {
        bridge.findClass {
            // Search within the specified package name range
            searchPackages = listOf("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            excludePackages = listOf("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher for class matching
            matcher {
                className = "org.luckypray.dexkit.demo.PlayActivity"
                // FieldsMatcher for matching properties within the class
                fields {
                    // Add a matcher for properties
                    add {
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                        type = "java.lang.String"
                        name = "TAG"
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // Specify the number of properties in the class
                    count = 3
                }
                // MethodsMatcher for matching methods within the class
                methods {
                    // Add a matcher for methods
                    add {
                        modifiers = Modifier.PROTECTED
                        name = "onCreate"
                        returnType = "void"
                        paramTypes = listOf("android.os.Bundle")
                        usingStrings = listOf("onCreate")
                    }
                    add {
                        paramTypes = listOf("android.view.View")
                        usingNumbers = listOf(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        modifiers = Modifier.PUBLIC
                        paramTypes = listOf("boolean")
                    }
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    count(1..10)
                }
                // AnnotationsMatcher for matching annotations within the class
                annotations {
                    add {
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        addElement {
                            name = "path"
                            stringValue("/play")
                        }
                    }
                }
                // Strings used by all methods in the class
                usingStrings = listOf("PlayActivity", "onClick", "onCreate")
            }
        }.forEach {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            println(it.name)
            // Get the corresponding class instance
//            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
    }

    @Test
    fun readmeZhTest() {
        bridge.findClass {
            // 从指定的包名范围内进行查找
            searchPackages = listOf("org.luckypray.dexkit.demo")
            // 排除指定的包名范围
            excludePackages = listOf("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher 针对类的匹配器
            matcher {
                className = "org.luckypray.dexkit.demo.PlayActivity"
                // FieldsMatcher 针对类中包含属性的匹配器
                fields {
                    // 添加对于属性的匹配器
                    add {
                        modifiers(Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL)
                        type("java.lang.String")
                        name("TAG")
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // 指定类中属性的数量
                    count = 3
                }
                // MethodsMatcher 针对类中包含方法的匹配器
                methods {
                    // 添加对于方法的匹配器
                    add {
                        modifiers = Modifier.PROTECTED
                        name = "onCreate"
                        returnType = "void"
                        paramTypes = listOf("android.os.Bundle")
                        usingStrings = listOf("onCreate")
                    }
                    add {
                        paramTypes = listOf("android.view.View")
                        usingNumbers = listOf(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        paramTypes = listOf("boolean")
                    }
                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                    count(1..10)
                }
                // AnnotationsMatcher 针对类中包含注解的匹配器
                annotations {
                    add {
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        addElement {
                            name = "path"
                            stringValue("/play")
                        }
                    }
                }
                // 类中所有方法使用的字符串
                usingStrings = listOf("PlayActivity", "onClick", "onCreate")
            }
        }.forEach {
            // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
            println(it.name)
            // 获取对应的类实例
//            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
    }
}