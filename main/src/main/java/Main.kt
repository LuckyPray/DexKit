import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
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
    val file = File("apk/qq-8.9.2.apk")
    if (!file.exists()) {
        println("apk not found")
        return
    }
    doSearch(file.absolutePath)
}

fun doSearch(path: String) {
    DexKitBridge.create(path)?.use { bridge ->
        bridge.findClass {
            searchPackage("com.tencent.mobileqq")
            matcher {
                className("com/tencent/mobileqq/antiphing/a")
                annotations {
                    add {
                        typeName("dalvik/annotation/MemberClasses")
                        elements {
                            elementCount(1)
                            add {
                                name("value")
                                matcher {
                                    arrayValue {
                                        add {
                                            classValue {
                                                className("com/tencent/mobileqq/antiphing/a\$c")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.forEach {
            println("find -> $it")
        }
//        bridge.findMethod {
//            matcher {
//                invokingMethods {
//                    add {
////                        declaredClass("com/tencent/mobileqq/app/QQAppInterface")
//                        name("<init>")
//                    }
//                }
//            }
//        }.forEach {
//            println(it)
//        }
    }
}
