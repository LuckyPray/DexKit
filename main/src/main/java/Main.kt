import org.luckypray.dexkit.DexKitBridge
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
    val file = File("apk/demo.apk")
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
            searchPackages = listOf("org.luckypray.dexkit.demo")
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
                    // Specify the number of methods in the class, a minimum of 4, and a maximum of 10
                    count(1..10)
                }
                // AnnotationsMatcher for matching interfaces within the class
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
            println(it.className)
            // Get the corresponding class instance
        }
        println("find use time: ${System.currentTimeMillis() - startTime}ms")
    }
}
