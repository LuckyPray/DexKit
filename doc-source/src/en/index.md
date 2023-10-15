---
home: true
title: Home
actions:
- text: QuickStart
  link: /en/guide/home
  type: primary
- text: API KDoc
  link: https://luckypray.org/DexKit-Doc
  type: secondary
features:
- title: Elegant and Simple
  details: DexKit is built with a user-friendly API entirely using Kotlin DSL, supporting nested complex queries and providing good support for Java as well.
- title: Superior Performance
  details: Implemented using C++ at its core, DexKit delivers superior performance. It utilizes multiple algorithms on top of multithreading, allowing it to complete complex searches in an extremely short time.
- title: Cross-platform
  details: It offers multi-platform support. After testing on Windows, Linux, or MacOS, the code can be directly migrated to the Android platform.
footer: LGPL-3.0 License | Copyright © 2022 LuckyPray
---

### Ultimate Experience, Say No to Tediousness

#### Example Code

> Assume this is obfuscated code from a host app. We need to dynamically adapt the hook for this method. 
> Due to obfuscation, method names and class names may change with each version.

```java
public class abc {

    public boolean cvc() {
        boolean b = false;
        // ...
        Log.d("VipCheckUtil", "userInfo: xxxx");
        // ...
        return b;
    }
}
```

DexKit can easily solve this problem!

#### Xposed hook code

> By creating an instance of `DexKitBridge`, we can perform specific searches on the app's dex file.

::: warning warning
Only one instance of `DexKitBridge` needs to be created. If you do not wish to use 
try-with-resources or Kotlin's .use for automatic closing of the `DexKitBridge` instance, 
you need to manage its lifecycle manually. Ensure to call `.close()` when it's no longer 
needed to prevent memory leaks.
:::

:::: code-group
::: code-group-item kotlin
```kotlin
class AppHooker {
    companion object {
        init { System.loadLibrary("dexkit") }
    }

    private var hostClassLoader: ClassLoader

    public constructor(loadPackageParam: LoadPackageParam) {
        this.hostClassLoader = loadPackageParam.classLoader
        val apkPath = loadPackageParam.appInfo.sourceDir
        DexKitBridge.create(apkPath)?.use { bridge ->
            isVipHook(bridge)
            // Other hook ...
        }
    }

    private fun isVipHook(bridge: DexKitBridge) {
        val methodData = bridge.findMethod {
            matcher {
                // All conditions are optional, you can freely combine them
                modifiers = Modifier.PUBLIC
                returnType = "boolean"
                paramCount = 0
                usingStrings("VipCheckUtil", "userInfo:")
            }
        }.firstOrNull() ?: error("method not found")
        val method: Method = methodData.getMethodInstance(hostClassLoader)
        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
    }

    // ...
}
```
:::
::: code-group-item java
```java
class AppHooker {
    static {
        System.loadLibrary("dexkit");
    }

    private ClassLoader hostClassLoader;

    public AppHooker(LoadPackageParam loadPackageParam) {
        this.hostClassLoader = loadPackageParam.classLoader;
        String apkPath = loadPackageParam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            isVipHook(bridge);
            // Other hook ...
        }
    }

    private void isVipHook(DexKitBridge bridge) {
        MethodData methodData = bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        // All conditions are optional, you can freely combine them
                        .modifiers(Modifier.PUBLIC)
                        .paramCount(0)
                        .returnType("boolean")
                        .usingStrings("VipCheckUtil", "userInfo:")
                )
        ).firstOrThrow(() -> new RuntimeException("Method not found"));
        Method method = methodData.getMethodInstance(hostClassLoader);
        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
    }

    // ...
}
```
:::
::::

It's that simple!
