import io.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.schema.AccessFlagsMatcher
import org.luckypray.dexkit.schema.ClassMatcher
import org.luckypray.dexkit.schema.IntRange
import org.luckypray.dexkit.schema.MatchType
import org.luckypray.dexkit.schema.StringMatchType
import org.luckypray.dexkit.schema.StringMatcher
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
    find(file.absolutePath)
}

fun find(path: String) {
    DexKitBridge.create(path)?.use { dexkit ->
        dexkit.searchClass {
            packageName = StringMatcher(
                type = StringMatchType.StartWith,
                ignoreCase = false,
                value = "com.tencent"
            )
            accessFlags = AccessFlagsMatcher(
                flags = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL,
                matchType = MatchType.Equal
            )
            superClass = classMatcher {}
            interfacesMatcher {}
            contain {
                methodMatcher {
                    methodMatcher {
                        parametersMatcher {
                            matchType = MatchType.Equal
                            parameters = arrayOf(
                                classMatcher {
                                    usingStrings {
                                        value = "Util"
                                    }
                                },
                                // match any class
                                null,
                            )
                        }
                    }
                }
                fieldMatcher {
                    annotationsMatcher {
                        typeName = classMatcher {
                            typeName = "com.alibaba.fastjson.annotation.JSONField"
                        }
                        annotationElementsMatcher {
                            elementCount = IntRange(max = 3)
                            matchType = MatchType.Contain
                            elements = arrayOf(
                                annotationElementMatcher {
                                    name = "name"
                                    value = "user_name"
                                }
                            )
                        }
                    }
                }
            }
            // 在上一步搜索中返回的类中匹配方法
        }.searchMethod {
            methodMatcher {
                usingField {
                    type = classMatcher {
                        typeName = "com.tencent.mobileqq.activity.aio.photo.PhotoListPanel"
                    }
                    name = "a"
                }
            }
        }.first().hookAfter {
            // TODO do something
        }
    }
}