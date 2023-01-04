# Introduce

> A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes,
> methods, or properties.

## Supported functions

- Batch search for methods/classes with specified string
- Find methods/classes that use a specified string
- Method call/called search
- Direct subclass search
- Method multi-condition search
- Opcode sequence search (standard dex instructions only)
- Annotation search (currently only supports search for string values)

## Used Demo

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
fun vipHook() {
    val apkPath = hostApp.applicationInfo.sourceDir
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
public void vipHook() throws NoSuchMethodException {
    String apkPath = HostInfo.getHostApp().getApplicationInfo().sourceDir;
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

Now, with the powerful performance of `DexKit`, you can quickly locate obfuscation.

Next, let's learn how to use `DexKit`.
