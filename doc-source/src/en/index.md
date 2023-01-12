---
home: true
title: Home
actions:
- text: quick started
  link: /en/guide/home
  type: primary
- text: API KDoc
  link: https://luckypray.org/DexKit-Doc
  type: secondary
features:
- title: Easy to use
  details: It is dependent on IDE prompts, and can be quickly mastered without documentation.
- title: Efficient
  details: Implementing using C++ and defaulting to multithreaded lazy loading of resources, its speed is ten to even hundreds of times faster than other tools of the same type.
- title: Cross-platform
  details: It is able to be used on various platforms, such as testing in Windows, Linux, or MacOS, and then moving the logic into the Android platform after testing is completed.
footer: LGPL-3.0 License | Copyright © 2022 LuckyPray
---

### Refuse tediousness

#### Demo App code

> This is a host app's obfuscated code, and we need to dynamically adapt the hook for this method. Due to obfuscation, the method name and class name may change with each version.
```java
public class abc {
    
    public boolean cvc() {
        boolean b = false;
        // ...
        Log.d("VipCheckUtil", "info: xxxx");
        // ...
        return b;
    }
}
```

dexkit can quickly meet our needs at this point.

#### Xposed hook code

> By creating an instance of `DexKitBridge`, we can search for specific dex in the APP, 
> but remember to only instantiate it once and avoid repeating the creation. And after using it, 
> we need to call the `DexKitBridge.close()` method to release the memory, otherwise it will cause memory leakage.

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
