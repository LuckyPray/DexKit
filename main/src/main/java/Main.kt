import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.lang.reflect.Modifier
import java.util.Locale

val isWindows
    get() = System.getProperty("os.name")
        .lowercase(Locale.getDefault())
        .contains("windows")

@Suppress("UnsafeDynamicallyLoadedCode")
fun loadLibrary(name: String) {
    try {
        System.loadLibrary(if (isWindows) "lib$name" else name)
    } catch (e: UnsatisfiedLinkError) {
        val libraryPath = File("main/build/library")
        libraryPath.listFiles()?.forEach {
            if (it.name.startsWith("lib$name")) {
                System.load(it.absolutePath)
            }
        }
    }
}

fun main() {
    loadLibrary("dexkit")
    println("current work dir: ${File("").absolutePath}")
    val file = File("main/apk/QQ_8.9.2-3072.apk")
    if (!file.exists()) {
        println("apk not found")
        return
    }
    doSearch(file.absolutePath)
}

fun doSearch(path: String) {
    DexKitBridge.create(path)?.use { bridge ->
        val startTime = System.currentTimeMillis()
        bridge.findClass {
            // Search within the specified package name range
            searchPackage("org.luckypray.dexkit.demo")
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
                    countRange(min = 1, max = 10)
                }
                // InterfacesMatcher for matching interfaces within the class
                annotations {
                    add {
                        typeName("Router", StringMatchType.EndWith)
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
            // Print the found class: com.test.demo.a
            println(it.className)
            // Get the corresponding class instance
//            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
        println("find use time: ${System.currentTimeMillis() - startTime}ms")
    }
}
