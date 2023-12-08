package org.luckypray.dexkit.demo.hook

import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

public class AppHooker {
    companion object {
        init {
            System.loadLibrary("dexkit")
        }
    }

    private var hostClassLoader: ClassLoader

    public constructor(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        this.hostClassLoader = loadPackageParam.classLoader
        val apkPath = loadPackageParam.appInfo.sourceDir
        DexKitBridge.create(apkPath).use { bridge ->
            findPlayActivity(bridge)
            findActivity(bridge)
            // Other use cases
        }
    }

    private fun findPlayActivity(bridge: DexKitBridge) {
        bridge.findClass {
            searchPackages("org.luckypray.dexkit.demo")
            excludePackages("org.luckypray.dexkit.demo.annotations")
            matcher {
                fields {
                    add {
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                        type = "java.lang.String"
                        name = "TAG"
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    count = 3
                }
                methods {
                    add {
                        modifiers = Modifier.PROTECTED
                        name = "onCreate"
                        returnType = "void"
                        paramTypes("android.os.Bundle")
                        usingStrings("onCreate")
                    }
                    add {
                        paramTypes("android.view.View")
                        usingNumbers(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        paramTypes("boolean")
                        invokeMethods {
                            add {
                                modifiers = Modifier.PUBLIC or Modifier.STATIC
                                returnType = "int"
                                usingStrings(listOf("RandomUtil", "getRandomDice: "), StringMatchType.Equals)
                            }
                        }
                    }
                    count(1..10)
                }
                annotations {
                    add {
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        addElement {
                            name = "path"
                            stringValue("/play")
                        }
                    }
                }
            }
        }.single().let {
            // org.luckypray.dexkit.demo.PlayActivity
            println(it.name)
        }
    }

    private fun findActivity(bridge: DexKitBridge) {
        bridge.findClass {
            matcher {
                // androidx.appcompat.app.AppCompatActivity
                superClass {
                    // androidx.fragment.app.FragmentActivity
                    superClass {
                        // androidx.activity.ComponentActivity
                        superClass {
                            // androidx.core.app.ComponentActivity
                            superClass {
                                superClass = "android.app.Activity"
                            }
                        }
                    }
                }
            }
        }.forEach {
            // org.luckypray.dexkit.demo.MainActivity
            // org.luckypray.dexkit.demo.PlayActivity
            println(it.name)
        }
    }

    // ...
}