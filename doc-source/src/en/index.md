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
- title: Easy to use
  details: No need to learn bytecode! Using your IDE's hints, you can quickly get started without the need for documentation.
- title: Efficient
  details: Implemented in C++ and defaults to multithreaded lazy loading of resources! Up to ten or even hundreds of times faster than other similar tools.
- title: Cross-platform
  details: Usable on various platforms, such as testing on Windows, Linux, or MacOS, and then moving the logic into the Android platform after testing is completed.
footer: LGPL-3.0 License | Copyright © 2022 LuckyPray
---

### Hassle-Free Hooking!

#### Example Code

> This is an example app's obfuscated code, and we want to dynamically adapt the hook for this method. Due to obfuscation, the method and class name may change with each version.
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

> By creating an instance of `DexKitBridge`, you can search for specific dex in the APP

::: warning
Only create one single instance. Once done, you must call `.close()` to prevent memory leaks (or better yet, use try with resources / kotlin .use).
:::

:::: code-group
::: code-group-item kotlin
```kotlin
@Throws(NoSuchMethodException::class)
fun vipHook(loadPackageParam: LoadPackageParam) {
    System.loadLibrary("dexkit")
    val apkPath = loadPackageParam.appInfo.sourceDir
    DexKitBridge.create(apkPath)?.use { bridge ->
        val resultMap = bridge.batchFindMethodsUsingStrings {
            addQuery("VipCheckUtil_isVip", setOf("VipCheckUtil", "userInfo:"))
            matchType = MatchType.CONTAINS
        }
        val result = resultMap["VipCheckUtil_isVip"]!!
        assert(result.size == 1)

        val descriptor = result.first()
        val method: Method = descriptor.getMethodInstance(hostClassLoader)
        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
    }
}
```
:::
::: code-group-item java
```java
public void vipHook(LoadPackageParam loadPackageParam) throws NoSuchMethodException {
    System.loadLibrary("dexkit");
    String apkPath = loadPackageParam.appInfo.sourceDir;
    try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
        if (bridge == null) {
            return;
        }
        Map<String, List<DexMethodDescriptor>> resultMap =
            bridge.batchFindMethodsUsingStrings(
                BatchFindArgs.builder()
                    .addQuery("VipCheckUtil_isVip", List.of("VipCheckUtil", "userInfo:"))
                    .matchType(MatchType.CONTAINS)
                    .build()
            );

        List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("VipCheckUtil_isVip"));
        assert result.size() == 1;

        DexMethodDescriptor descriptor = result.get(0);
        Method isVipMethod = descriptor.get(0)
            .getMethodInstance(HostInfo.getHostClassLoader());
        XposedBridge.hookMethod(isVipMethod, XC_MethodReplacement.returnConstant(true));
    }
}
```
:::
::::

How about that? Isn't it easy!
