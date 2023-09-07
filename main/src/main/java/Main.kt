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
    val file = File("apk/wyy_8.10.61.apk")
    if (!file.exists()) {
        println("apk not found")
        return
    }
    doSearch(file.absolutePath)
}

fun doSearch(path: String) {
    DexKitBridge.create(path)?.use { bridge ->
        val startTime = System.currentTimeMillis()
        for (i in 1..10)
            bridge.findClass {
//                searchPackages("com/netease/cloudmusic")
                findFirst = true
                matcher {
                    fields {
                        addForType("java.util.concurrent.ConcurrentHashMap")
                        addForType("android.content.SharedPreferences")
                        addForType("long")
                    }
//                    methods {
//                        add {
//                            modifiers = Modifier.PRIVATE
//                            parameterTypes = listOf("java.lang.String")
//                            returnType = "okhttp3.Cookie"
//                        }
//                        add {
//                            modifiers = Modifier.PUBLIC
//                            returnType = "java.lang.String"
//                        }
//                    }
//                    usingStrings = listOf("MUSIC_U", "MUSIC_A")
                }
            }.forEach {
                println("find class: ${it.dexDescriptor}")
            }
        println("find use time: ${System.currentTimeMillis() - startTime}ms")
    }
}
