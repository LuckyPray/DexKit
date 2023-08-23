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
            matcher {
                className("com.tencent.mobileqq.kandian.biz.publisher.e.a")
            }
        }.forEach {
            println()
            it.getAnnotations().forEach {
                println(it)
            }
            println(it)
            println()

            it.getFields().forEach {
                println(it)
                println()
            }

            it.getMethods().forEach {
                println(it)
                println()
            }
        }
        println("find use time: ${System.currentTimeMillis() - startTime}ms")
    }
}
