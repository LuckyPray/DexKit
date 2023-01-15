import io.luckypray.dexkit.DexKitBridge
import java.io.File
import java.util.Locale

val isWindows
    get() = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

@Suppress("UnsafeDynamicallyLoadedCode")
fun loadLibrary(name: String) {
    val libName = if (isWindows) "lib$name" else name
    try {
        System.loadLibrary(libName)
    } catch (e: UnsatisfiedLinkError) {
        val libraryPath = File("main/build/library")
        libraryPath.listFiles()?.forEach {
            if (it.name.startsWith(libName)) {
                System.load(it.absolutePath)
            }
        }
    }
}

fun main() {
    loadLibrary("dexkit")
    println("current work dir: ${File("").absolutePath}")
    val file = File("apk/QQ-play-8.2.11.apk")
    if (!file.exists()) {
        println("apk not found")
        return
    }
    find(file.absolutePath)
}

fun find(path: String) {
    DexKitBridge.create(path)?.use { kit ->
        kit.findMethodUsingString {
            usingString = "imei"
            findPackage = "com/tencent"
        }.forEach {
            println(it.descriptor)
        }
        kit.findClass {
            findPackage = "AvatarInfo"
            sourceFile = "P"
        }.forEach {
            println(it.descriptor)
        }
    }
}