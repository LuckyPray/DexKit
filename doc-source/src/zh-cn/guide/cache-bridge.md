# DexKitCacheBridge 指南

> `DexKitCacheBridge` 在 `DexKitBridge` 之上提供 bridge 复用、空闲释放和查询结果缓存，用于减少宿主重复启动时的重复查询开销，避免把查找成本反复堆到启动阶段。

::: warning 实验性 API
`DexKitCacheBridge` 当前被标记为 `@DexKitExperimentalApi`。这意味着它仍属于实验性 API，后续版本中其 API 形态或行为仍可能调整。

对于 Kotlin 调用方，需要显式 opt-in：

```kotlin
import org.luckypray.dexkit.annotations.DexKitExperimentalApi

@OptIn(DexKitExperimentalApi::class)
fun useCacheBridge() {
    // 在这里使用 DexKitCacheBridge
}
```

如果你准备在自己的公共封装层里继续暴露 `DexKitCacheBridge`，建议在自身侧保留一层兼容封装，以降低后续调整带来的影响。
:::

## 为什么需要它

直接使用 `DexKitBridge` 时，常见的成本主要有两类：

- `DexKitBridge` 更适合按需创建和及时释放；如果为了复用而长期持有，资源管理会变得粗放，如果每次都重新创建，重复开销又会被不断放大。
- 同一宿主、同一版本下，很多查询结果其实是稳定的；如果每次启动都重新查找，本质上只是重复消耗时间。

`DexKitCacheBridge` 的目标就是把这两件事统一处理掉：

- 对同一个 `appTag` 复用同一个 `RecyclableBridge` 包装对象，在未销毁前避免重复创建包装层。
- 在空闲时自动释放底层 native bridge，减少为了复用而长期持有 `DexKitBridge` 带来的资源占用风险。
- 将可序列化的查询结果缓存到你自己的存储实现中。
- 提供查询结果与 bridge 生命周期回调，方便观测执行过程。

## 适用场景

- 同一宿主版本的查询结果希望跨进程重启复用。
- 希望复用 bridge，但又不想长期持有底层 `DexKitBridge`。
- 需要通过回调观察查询结果或 bridge 生命周期。

如果你的查询只会执行一次，或者完全不需要持久化缓存，直接使用 `DexKitBridge` 会更简单。

## 快速开始

### 1. 实现一个 `Cache`

`DexKitCacheBridge` 不限制底层存储，你只需要实现 `Cache` 接口即可。

下面是一个最小的内存实现：

```kotlin
import org.luckypray.dexkit.DexKitCacheBridge
import java.util.concurrent.ConcurrentHashMap

object MemoryCache : DexKitCacheBridge.Cache {
    private val values = ConcurrentHashMap<String, String>()
    private val lists = ConcurrentHashMap<String, List<String>>()

    override fun getString(key: String, default: String?): String? =
        values[key] ?: default

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getStringList(key: String, default: List<String>?): List<String>? =
        lists[key] ?: default

    override fun putStringList(key: String, value: List<String>) {
        lists[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
        lists.remove(key)
    }

    override fun getAllKeys(): Collection<String> =
        values.keys + lists.keys

    override fun clearAll() {
        values.clear()
        lists.clear()
    }
}
```

### 2. 在应用初始化阶段只调用一次 `init(cache)`

```kotlin
fun initDexKitCache() {
    // 可选：设置空闲多久后自动释放底层 bridge
    DexKitCacheBridge.idleTimeoutMillis = 5_000L
    // 可选：配置成功/失败结果是否写入缓存
    DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
        cacheSuccess = true,
        failurePolicy = DexKitCacheBridge.CacheFailurePolicy.NONE
    )
    DexKitCacheBridge.init(MemoryCache)
}
```

`init(cache)` 是全局初始化，只允许调用一次，并且必须先于任何 `create(...)`。

`idleTimeoutMillis` 和 `cachePolicy` 都是可选的全局配置；如果不设置，会使用默认值。通常建议在 `init(cache)` 前一并完成配置。

### 3. 创建并使用 `RecyclableBridge`

```kotlin
fun findPlayActivity(apkPath: String) {
    DexKitCacheBridge.create(
        appTag = "demo:1.0.0",
        path = apkPath
    ).use { bridge ->
        val playActivity = bridge.getClass("play_activity") {
            matcher {
                className("org.luckypray.dexkit.demo.PlayActivity")
            }
        }
        println(playActivity.typeName)
    }
}
```

这里的 `.use { ... }` 与常见的 `try-with-resources` / Kotlin `use` 语义一致，作用域结束后会自动调用 `close()`。

`create(...)` 目前支持三种数据来源：

- `create(appTag, path)`
- `create(appTag, dexArray)`
- `create(appTag, classLoader)`

## `Cache` 接口的实现建议

`DexKitCacheBridge` 只要求底层存储能处理两类值：

- `String`
- `List<String>`

这是因为 `DexClass`、`DexMethod`、`DexField` 等结果本质上都会序列化成字符串；多结果查询和批量查询则会写成字符串列表。

实现时建议注意：

- `remove(key)` 最好同时删除字符串值和字符串列表值。
- `getAllKeys()` 需要返回当前命名空间下的所有 key，`clearCache(appTag)` 会依赖它做前缀清理。
- 如果你的存储本身不是线程安全的，请自行保证并发安全。

::: warning
`SharedPreferences` 本身不支持 `List<String>` 或 blob。如果你选择它作为底层存储，就需要自行对 `List<String>` 做编码和解码。

由于 DexKit 的缓存内容本质上是序列化字符串集合，数据量大时把它们编码后塞进 `SharedPreferences` 并不是一个好的实践。更推荐使用 `MMKV`、`DataStore`、`SQLite/Room` 或你自己的文件型存储实现。
:::

## `appTag` 的作用

`appTag` 是 `DexKitCacheBridge` 的核心标识，它同时决定了：

- bridge 包装对象的复用范围
- 缓存 key 的命名空间
- `clearCache(appTag)` 的清理范围

同一个 `appTag` 下：

- `DexKitCacheBridge.create(appTag, ...)` 采用基于 `appTag` 的单例复用模式：在当前进程内，只要对应 `RecyclableBridge` 还没有被 `destroy()`，后续再次 `create(appTag, ...)` 就会返回同一个包装对象
- 查询缓存也会落在同一命名空间下

因此，`appTag` 必须能够稳定标识“当前这份 dex 来源”。常见做法是把这些信息拼进去：

- 宿主包名
- 宿主版本号 / versionCode

::: tip
如果你把版本信息纳入 `appTag`，那么宿主升级和降级都会切换到对应的缓存命名空间：新版本不会误用旧缓存，回退到已缓存的旧版本时也能直接复用旧结果。反之，如果宿主内容已经变化却仍沿用旧 `appTag`，就可能错误复用旧缓存。
:::

::: warning Android 多进程说明
`DexKitCacheBridge` 的 bridge 复用是**进程内**行为：每个进程都会维护自己独立的 `RecyclableBridge` 实例和复用池。  
如果你在多进程环境下使用，例如主进程写缓存、插件进程读缓存，那么真正跨进程共享的是你实现的持久化 `Cache` 数据，而不是内存中的 bridge 对象。

因此，多进程场景下底层 `Cache` 实现必须具备多进程可见性和一致性，例如：

- `MMKV` 的多进程模式
- 通过 `ContentProvider` 封装的数据库/存储

如果只是使用普通的进程内内存缓存，那么它只能在当前进程生效。
:::

## 生命周期：`close()` 与 `destroy()`

`DexKitCacheBridge.create(...)` 返回的是 `RecyclableBridge`，它有两种常用的结束方式。

### `close()`

- 释放当前底层 `DexKitBridge`
- 当前包装对象仍然可以继续使用
- 之后再次访问时，会按需重新创建底层 bridge
- 同一个 `appTag` 再次 `create(...)`，通常还能拿到同一个包装对象

这也是 `use { ... }` 默认执行的行为，适合“调用完立即释放资源”。

### `destroy()`

- 永久销毁当前 `RecyclableBridge`
- 销毁后继续访问这个对象会抛出异常
- 同一个 `appTag` 再次 `create(...)` 时，会得到一个新的包装对象

`destroy()` 只处理 bridge 生命周期，不会自动清理持久化缓存；如果还想删除缓存，需要再调用 `clearCache(appTag)` 或 `clearAllCache()`。

## 查询方式与 key 设计

`DexKitCacheBridge` 支持三类常见用法。

### 1. 结构化查询 + 自动 key

```kotlin
val clazz = bridge.getClass {
    matcher {
        className("org.luckypray.dexkit.demo.PlayActivity")
    }
}
```

这种写法不需要你手动提供 key，内部会根据 query 的 `hashKey()` 自动生成缓存 key。  
适合查询条件本身稳定、且不需要手动管理 key 的场景。

### 2. 结构化查询 + 显式 key

```kotlin
val clazz = bridge.getClass("play_activity") {
    matcher {
        className("org.luckypray.dexkit.demo.PlayActivity")
    }
}
```

适合这些场景：

- 你希望缓存 key 有明确语义，便于排查和迁移
- 你希望后续不重新构造 query，就能直接按 key 读取缓存
- 你希望由常量、配置文件或服务端下发值来决定读取哪一份缓存

如果缓存已经存在，也可以只传 key：

```kotlin
val clazz = bridge.getClass("play_activity")
```

这表示“只读缓存，不再执行查询”。这种方式很适合结合本地配置或服务端动态下发的 key，直接决定读取哪一份已缓存结果。  
如果这个 key 当前并没有缓存，会抛出 `NoSuchElementException`。

### 3. Direct API

有些场景下无法使用结构化 query，而是想直接拿原始 `DexKitBridge` 写查询逻辑，这时可以使用 `*Direct` 系列 API：

```kotlin
val clazz = bridge.getClassDirect("play_activity_direct") {
    findClass {
        matcher {
            className("org.luckypray.dexkit.demo.PlayActivity")
        }
    }.single()
}
```

Direct API 必须提供显式 key，因为它无法像结构化 query 那样自动从查询语义中推导稳定 key。

## 支持缓存的查询形式

目前常用的缓存查询主要包括：

- 单结果：`getClass` / `getMethod` / `getField`
- 多结果：`getClasses` / `getMethods` / `getFields`
- 批量字符串查询：`getBatchUsingStringsClasses` / `getBatchUsingStringsMethods`
- 直接查询版本：`getClassDirect` / `getMethodDirect` / `getFieldDirect` 等

如果你只是想临时拿到底层 `DexKitBridge` 做一些未封装操作，可以使用：

```kotlin
bridge.withBridge { rawBridge ->
    // 在这里直接使用 DexKitBridge
}
```

`withBridge` 只负责一次安全借用：进入时获取可用 bridge，结束后归还引用并参与空闲释放计时；它不会自动帮你做结果缓存。

## 缓存策略

全局缓存策略由 `DexKitCacheBridge.cachePolicy` 控制：

默认情况下，`DexKitCacheBridge` 只缓存成功结果，不缓存失败状态。  
这意味着某个单结果查询如果稳定失败，例如“确实找不到”或“结果不唯一”，那么每次启动仍然会重新执行一遍。

如果你的场景里这类失败本身就是确定性的，可以通过 `failurePolicy` 把失败状态也缓存下来，减少重复创建 bridge 和重复查询的成本。  
但要注意：当宿主更新、查询条件修正或适配逻辑变更后，你需要主动清理旧的失败缓存，否则后续仍会命中旧失败状态。

```kotlin
DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
    cacheSuccess = true,
    failurePolicy = DexKitCacheBridge.CacheFailurePolicy.NONE
)
```

### `cacheSuccess`

- `true`：缓存成功结果
- `false`：不缓存成功结果，但 bridge 复用和监听机制仍然可用

### `CacheFailurePolicy`

| 策略 | 行为 |
| --- | --- |
| `NONE` | 不缓存失败结果 |
| `QUERY_ONLY` | 仅缓存“结构化自动 key 的单结果查询”的失败结果 |
| `ALL` | 缓存所有单结果查询的失败结果，包括显式 key 和 Direct API |

这里的“失败结果”主要指单结果查询中的：

- `NoResultException`
- `NonUniqueResultException`

多结果查询和批量查询不会使用这套失败缓存策略，因为空列表或空分组本身就可以作为合法结果表达，不存在单结果查询那种语义歧义。

::: tip
运行期修改 `cachePolicy` 只会影响后续查询行为，不会回写或重解释已经存在的缓存内容。实际项目中更推荐在初始化阶段一次性配置好。
:::

## 空闲释放：`idleTimeoutMillis`

```kotlin
DexKitCacheBridge.idleTimeoutMillis = 5_000L
```

默认值是 `5000` 毫秒。

它表示：当一个 `RecyclableBridge` 在空闲状态下停留多久后，底层 native bridge 可以被自动释放。  
这是一种兜底释放机制，主要用于避免调用方忘记释放时长期占用资源，而不是鼓励“用完不关”。日常使用仍应优先：

- 使用 `use { ... }` / `try-with-resources`
- 或在合适时机主动调用 `close()`

这样做的好处是：

- 短时间内频繁使用时，可以复用底层 bridge
- 长时间不用时，又不会一直占着资源

可以把它理解成下面这条生命周期：

```text
create/apply query
    ↓
bridge 正在被使用
    ↓  （此时不会开始空闲倒计时）
最后一个使用方结束 / use 块退出
    ↓
开始 idleTimeoutMillis 倒计时
    ↓
超时前再次使用      超时且无人再使用
    ↓               ↓
取消本次计时        自动释放底层 DexKitBridge
```

需要特别注意两点：

- 这里判断的是 bridge 是否仍被占用，而不是 `QuerySuccessEvent.matchCount` 这类“查询结果数量”字段。
- 如果查询还在 `use { ... }`、`withBridge { ... }` 或某个缓存 API 内部执行，空闲计时器不会生效；只有在没有任何地方继续占用该 bridge 后，才会开始倒计时。

这和手动 `close()` 并不冲突：

- `close()` 是显式释放，优先级更高。
- 如果调用 `close()` 时刚好还有查询在执行，会等当前占用结束后再释放，不会中断正在进行的查询。

和 `cachePolicy` 一样，建议在初始化阶段统一设置，而不是在业务运行过程中频繁切换。

## 监听事件

你可以通过 `CacheBridgeListener` 观察查询结果以及 bridge 生命周期：

```kotlin
val listener = object : DexKitCacheBridge.CacheBridgeListener() {
    override fun onQuerySuccess(info: DexKitCacheBridge.QuerySuccessEvent) {
        println("success: source=${info.source}, key=${info.requestKey}, count=${info.matchCount}")
    }

    override fun onQueryFailure(info: DexKitCacheBridge.QueryFailureEvent) {
        println("failure: source=${info.source}, key=${info.requestKey}, error=${info.error}")
    }

    override fun onBridgeCreated(appTag: String) {
        println("created: $appTag")
    }

    override fun onBridgeReleased(appTag: String) {
        println("released: $appTag")
    }

    override fun onBridgeDestroyed(appTag: String) {
        println("destroyed: $appTag")
    }
}

DexKitCacheBridge.addListener(listener)
```

用完后记得移除：

```kotlin
DexKitCacheBridge.removeListener(listener)
```

### `QuerySuccessEvent`

- `appTag`：当前宿主标识
- `queryKind`：查询类型，例如单结果、列表或批量查询
- `requestKey`：显式 key；如果是自动 key 查询，这里为 `null`
- `source`：结果来源，`CACHE` 或 `QUERY`
- `matchCount`：命中的结果数量；批量查询时是所有分组结果数之和

### `QueryFailureEvent`

- `appTag`
- `queryKind`
- `requestKey`
- `source`
- `error`：具体异常

监听器非常适合做这些事情：

- 验证缓存是否生效
- 统计查询命中率/进度

## 缓存清理

### 清理单个 `appTag`

```kotlin
DexKitCacheBridge.clearCache(appTag)
```

适合这些情况：

- 宿主升级，但你暂时还不想切换 `appTag`
- 某个 query 的 key 语义改了，想清掉这一批缓存
- 调试阶段想主动清空历史缓存，重新验证查询逻辑是否符合预期

### 清理全部缓存

```kotlin
DexKitCacheBridge.clearAllCache()
```

这个调用只会清缓存，不会自动销毁已经存在的 `RecyclableBridge`。

## 最佳实践

- 在应用启动阶段调用一次 `DexKitCacheBridge.init(cache)`。
- 通常把 `idleTimeoutMillis` 和 `cachePolicy` 也放在同一个初始化阶段配置；只有在明确知道自己需要什么行为时，再考虑运行期调整。
- 让 `appTag` 真实反映宿主版本或者 dex 来源，不要把不同宿主内容混用同一个 tag。
- 显式 key 要保持“同名即同义”；如果查询语义变了，请换 key 或清理旧缓存。
- 日常使用仍建议在用完后及时 `use { ... }` 或手动 `close()`；自动空闲释放更适合作为兜底，而不是替代显式释放。
- 持久化缓存优先选择适合大内容的存储实现，不要把大批量序列化数据硬塞进 `SharedPreferences`。
- 批量查询缓存如果出现局部缺失或损坏，会被当作缓存未命中，下一次查询会重新写回。
