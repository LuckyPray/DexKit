# Introduction

> A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes,
> methods, and properties.

## Supported functions

- Batch search for methods/classes with specified string
- Find methods/classes that use a specified string
- Method call/called search
- Direct subclass search
- Method multi-condition search
- Opcode sequence search (standard dex instructions only)
- Annotation search (currently only supports search for string values)

## Usage Example

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

Now, with the powerful performance of `DexKit`, you can quickly locate obfuscated methods, classes and co.

Next, let's learn how to use `DexKit`.
