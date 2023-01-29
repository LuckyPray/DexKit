import io.luckypray.dexkit.DexKitBridge
import java.io.File
import java.util.Locale

val isWindows
    get() = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

@Suppress("UnsafeDynamicallyLoadedCode")
fun loadLibrary(name: String) {
    try {
        System.loadLibrary(if (isWindows) "lib$name" else name)
    } catch (e: UnsatisfiedLinkError) {
        val libraryPath = File("main/build/library")
        libraryPath.listFiles()?.forEach {
            if (it.name.startsWith("lib$name")) {
                System.load(it.absolutePath)
                println(it.name)
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
    find(file.absolutePath)
}

fun find(path: String) {
    DexKitBridge.create(path)?.use { kit ->
        kit.findMethod {
            findPackage = "io/luckypray"
        }.forEach {
            println(it.descriptor)
            println(kit.getMethodAccessFlags(it))
        }
//        kit.findClass {
//            findPackage = "AvatarInfo"
//            sourceFile = "P"
//        }.forEach {
//            println(it.descriptor)
//        }
    }
}