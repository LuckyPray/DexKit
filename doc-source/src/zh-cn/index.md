---
home: true
title: 首页
actions:
- text: 快速上手
  link: /zh-cn/guide/home
  type: primary
- text: API KDoc
  link: https://luckypray.org/DexKit-Doc
  type: secondary
features:
- title: 优雅简洁
  details: 完全使用 Kotlin DSL 打造的人性化 API，可以支持嵌套复杂查询，同时对 Java 也提供了良好的支持。
- title: 性能卓越
  details: 底层使用 C++ 实现，性能卓越，同时在多线程的基础上使用多种算法对进行优化，能够在极短时间内完成复杂的搜索。
- title: 可跨平台
  details: 提供多平台支持，例如在 Windows、Linux 或者 MacOS 中测试后，代码可以直接迁移至 Android 平台。
footer: LGPL-3.0 License | Copyright © 2022 LuckyPray
---

### 极致体验，拒绝繁琐

#### 样例 APP 代码

> 假设这是一个宿主 APP 的被混淆后的代码，我们需要对这个方法的 hook 进行动态适配，由于混淆的存在，可能每个版本方法名以及类名都会发生变化。

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

DexKit 可以轻松解决这个问题！

#### Hook 代码

> 通过创建 `DexKitBridge` 实例，我们可以对 APP 的 dex 进行特定的查找

::: warning 注意
对于 `DexKitBridge` 只需要创建一个实例。 如果您不希望使用 try-with-resources 或者 kotlin .use 
自动关闭 `DexKitBridge` 实例，需要自行维护其生命周期。请确保在不需要时调用 `.close()` 方法以防止内存泄漏。
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
        // DexKit 创建是一项耗时操作，请不要重复创建。如果需要全局使用，
        // 请自行管理生命周期，确保在不需要时调用 .close() 方法以防止内存泄漏。
        // 这里使用 `Closable.use` 语法糖自动关闭 DexKitBridge 实例
        DexKitBridge.create(apkPath)?.use { bridge ->
            isVipHook(bridge)
            // Other hook ...
        }
    }
    
    private fun isVipHook(bridge: DexKitBridge) {
        val methodData = bridge.findMethod {
            matcher {
                // 一切条件都是可选的，您可以自由的组合
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
        // DexKit 创建是一项耗时操作，请不要重复创建。如果需要全局使用，
        // 请自行管理生命周期，确保在不需要时调用 .close() 方法以防止内存泄漏。
        // 这里使用 `try-with-resources` 语法糖自动关闭 DexKitBridge 实例
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            isVipHook(bridge);
            // Other hook ...
        }
    }
    
    private void isVipHook(DexKitBridge bridge) {
        MethodData methodData = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                // 一切条件都是可选的，您可以自由的组合
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

就是如此简单！
