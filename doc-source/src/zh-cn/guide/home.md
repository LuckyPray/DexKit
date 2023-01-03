# 介绍

> 这是一个使用 C++ 实现的高性能 dex 解析库，用于运行时查找被混淆的类、方法或者属性。

## 开发背景

最开始是因为模块开发需求，需要查找对于某些特定字符串的调用，咕咕华写的工具类看不懂（~~咕咕华全责！~~），于是自己去学习了一轮 Dalvik 字节码，
然后使用 Java 实现了一个功能较为简单的 Dex 解析库，但是由于支持的 API 过少，所以仅限于模块内部使用并没有独立出来。

后来由于某段时间`太极`对于 JIT 的特殊处理，导致 Dex 解析过程中的代码未被 JIT 优化编译成本地代码。所以执行速度极慢，在 Android12 上甚至需要
2分钟左右才能完成全部流程，于是就有了使用 C++ 重写的想法（~~虽然这个想法在很久以前就有了，咕咕咕~~）。

## 支持的功能

- 批量搜索指定字符串的方法/类
- 查找使用了指定字符串的方法/类
- 方法调用/被调用搜索
- 直系子类搜索
- 方法多条件搜索
- op序列搜索(仅支持标准dex指令)
- 注解搜索（目前仅支持搜索value为字符串的查找）

## 使用示例

### 样例 APP 代码

> 假设这是一个宿主 APP 的被混淆后的代码，我们需要对这个方法的 hook 进行动态适配，由于混淆的存在，可能每个版本方法名以及类名都会发生变化
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

### Xposed Hook 代码

> 通过创建 `DexKitBridge` 实例，我们可以对 APP 的 dex 进行特定的查找，但是切记实例化只需要进行一次，请自行存储，不要重复创建。
> 且在使用完毕后，需要调用 `DexKitBridge.close()` 方法，释放内存，否则会造成内存泄漏。

:::: code-group
::: code-group-item kotlin
```kotlin
@Throws(NoSuchMethodException::class)
fun vipHook() {
    val apkPath = hostApp.applicationInfo.sourceDir
    DexKitBridge.create(apkPath)?.use { bridge ->
        val resultMap = bridge.batchFindMethodsUsingStrings(
            BatchFindArgs.build { 
                addQuery("VipCheckUtil_isVip", setOf("VipCheckUtil", "userInfo:"))
            }
        )
        val result = resultMap["VipCheckUtil_isVip"]!!
        assert(result.size == 1)

        val method: Method = methodDescriptor[0]
            .getMethodInstance(hostClassLoader)
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
                new BatchFindArgs.Builder()
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

怎么样？是不是很简单！只需短短几行代码就完成了动态混淆的适配。

现在，借助性能强劲的 `DexKit`，可以快速的完成混淆的定位。

下面我们来学习一下 `DexKit` 的使用方法。
