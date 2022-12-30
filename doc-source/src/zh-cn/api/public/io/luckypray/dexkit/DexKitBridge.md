---
pageClass: code-page
---

# DexKitBridge <span class="symbol">- class</span>

```kotlin:no-line-numbers
class DexKitBridge : Closeable
```

**变更记录**

`v1.0.0` `添加`

**描述**
这是 DexKit 的核心类，它提供了一系列的方法来访问 dex 文件中的数据。

## DexKitBridge.Companion <span class="symbol">- companion object</span>

**变更记录**

`v1.0.0` `添加`

**描述**
这是 DexKitBridge 的伴生对象，它提供了工厂方法来创建 DexKitBridge 对象。

### DexKitBridge.Companion.create <span class="symbol">- function</span>

**变更记录**

`v1.0.0` `添加`

**描述**
这是一个工厂方法，它可以读取指定的apk文件并创建一个 DexKitBridge 对象。

```kotlin:no-line-numbers
@JvmStatic
fun create(apkPath: String): DexKitBridge?
```

### DexKitBridge.Companion.create <span class="symbol">- function</span>

**变更记录**

`v1.0.0` `添加`

**描述**
这是一个工厂方法，它可以根据 classloader 创建一个 DexKitBridge 对象。

```kotlin:no-line-numbers
@JvmStatic
fun create(classLoader: ClassLoader): DexKitBridge?
```

**变更记录**

`v1.0.0` `添加`

`v1.0.2` `删除`

