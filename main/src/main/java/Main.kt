import org.luckypray.dexkit.DexKitBridge
import java.io.File

val isWindows = System.getProperty("os.name")
    .lowercase()
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
    val createTime = System.currentTimeMillis()
    var releaseTime = 0L
    DexKitBridge.create(path).use { bridge ->
        println("create use time: ${System.currentTimeMillis() - createTime}ms")
        val startTime = System.currentTimeMillis()
        search(bridge)
        println("find use time: ${System.currentTimeMillis() - startTime}ms")
        releaseTime = System.currentTimeMillis()
    }
    println("release use time: ${System.currentTimeMillis() - releaseTime}ms")
}

fun search(bridge: DexKitBridge) {
    // TODO your search code
}


