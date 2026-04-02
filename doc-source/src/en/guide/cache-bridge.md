# DexKitCacheBridge Guide

> `DexKitCacheBridge` adds bridge reuse, idle release, and query-result caching on top of `DexKitBridge`, helping reduce repeated query cost across repeated host startups instead of paying the same lookup cost again and again during startup.

::: warning Experimental API
`DexKitCacheBridge` is currently marked with `@DexKitExperimentalApi`. This means it is still experimental, and its API surface or behavior may change in future versions.

Kotlin callers need to opt in explicitly:

```kotlin
import org.luckypray.dexkit.annotations.DexKitExperimentalApi

@OptIn(DexKitExperimentalApi::class)
fun useCacheBridge() {
    // use DexKitCacheBridge here
}
```

If you expose `DexKitCacheBridge` through your own public abstraction, it is recommended to keep a compatibility layer on your side to reduce the impact of future changes.
:::

## Why use it

When using `DexKitBridge` directly, there are usually two kinds of overhead:

- `DexKitBridge` is better suited for on-demand creation and timely release. If you keep it around just for reuse, resource management becomes coarse; if you recreate it every time, the repeated setup cost keeps coming back.
- Under the same host and the same version, many query results are stable. Re-running the same queries on every startup is often just repeated work.

`DexKitCacheBridge` is designed to solve both problems together:

- Reuse the same `RecyclableBridge` wrapper for the same `appTag`, avoiding repeated wrapper creation before it is destroyed.
- Automatically release the underlying native bridge when idle, reducing the resource risk of keeping `DexKitBridge` alive for too long just for reuse.
- Persist serializable query results into your own storage implementation.
- Provide callbacks for query results and bridge lifecycle events so you can observe what is happening.

## Typical use cases

- You want to reuse query results across process restarts for the same host version.
- You want bridge reuse without keeping the underlying `DexKitBridge` alive for too long.
- You want callbacks to observe query results or bridge lifecycle events.

If your query runs only once, or you do not need persistent caching at all, using `DexKitBridge` directly is simpler.

## Quick Start

### 1. Implement a `Cache`

`DexKitCacheBridge` does not restrict the storage backend. You only need to implement the `Cache` interface.

Here is a minimal in-memory implementation:

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

### 2. Call `init(cache)` once during app initialization

```kotlin
fun initDexKitCache() {
    // Optional: configure how long an idle bridge can stay alive
    DexKitCacheBridge.idleTimeoutMillis = 5_000L
    // Optional: configure whether successful / failed results are cached
    DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
        cacheSuccess = true,
        failurePolicy = DexKitCacheBridge.CacheFailurePolicy.NONE
    )
    DexKitCacheBridge.init(MemoryCache)
}
```

`init(cache)` is global initialization. It can only be called once, and it must happen before any `create(...)`.

`idleTimeoutMillis` and `cachePolicy` are both optional global settings. If you do not set them, the defaults will be used. In most cases, it is best to configure them together before `init(cache)`.

### 3. Create and use a `RecyclableBridge`

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

Here, `.use { ... }` follows normal Kotlin `use` / Java try-with-resources semantics: when the scope ends, `close()` is called automatically.

`create(...)` currently supports three data sources:

- `create(appTag, path)`
- `create(appTag, dexArray)`
- `create(appTag, classLoader)`

## Notes for implementing `Cache`

`DexKitCacheBridge` only requires the backend storage to support two kinds of values:

- `String`
- `List<String>`

This is because `DexClass`, `DexMethod`, `DexField`, and similar results are serialized as strings, while multi-result and batch queries are stored as lists of strings.

Implementation notes:

- `remove(key)` should ideally delete both the string value and the string-list value.
- `getAllKeys()` must return all keys in the current storage namespace because `clearCache(appTag)` relies on it for prefix-based cleanup.
- If your storage implementation itself is not thread-safe, you must handle synchronization on your own.

::: warning
`SharedPreferences` does not support `List<String>` or blob values directly. If you choose it as the backend, you need to encode and decode `List<String>` yourself.

Since DexKit cache content is essentially serialized string collections, stuffing large encoded payloads into `SharedPreferences` is usually not a good practice. `MMKV`, `DataStore`, `SQLite/Room`, or your own file-based storage is generally a better choice.
:::

## What `appTag` means

`appTag` is the core identity of `DexKitCacheBridge`. It determines:

- the reuse scope of the bridge wrapper
- the namespace of cache keys
- the cleanup scope of `clearCache(appTag)`

Under the same `appTag`:

- `DexKitCacheBridge.create(appTag, ...)` uses an `appTag`-scoped singleton reuse pattern inside the current process: as long as the corresponding `RecyclableBridge` has not been `destroy()`ed, later `create(appTag, ...)` calls return the same wrapper object
- query cache entries are also written under the same namespace

So `appTag` must stably identify the current dex source. Common choices include:

- host package name
- host version name / versionCode

::: tip
If version information is part of `appTag`, then both host upgrades and downgrades naturally switch to different cache namespaces: a new version will not accidentally reuse old cache, and switching back to a previously cached old version can directly reuse its old results. If the host content has changed but you still keep the old `appTag`, stale cache may be reused incorrectly.
:::

::: warning Android multi-process note
Bridge reuse in `DexKitCacheBridge` is **process-local**: each process keeps its own `RecyclableBridge` instances and reuse pool.  
In a multi-process setup, such as one process writing cache and another process reading it, what can actually be shared across processes is your persistent `Cache` data, not the in-memory bridge instance itself.

So in multi-process scenarios, the underlying `Cache` implementation must support cross-process visibility and consistency, for example:

- `MMKV` in multi-process mode
- a database or storage layer exposed via `ContentProvider`

If you only use a normal in-memory cache, it will work only inside the current process.
:::

## Lifecycle: `close()` vs `destroy()`

`DexKitCacheBridge.create(...)` returns a `RecyclableBridge`, which has two common ways to end its lifecycle.

### `close()`

- Releases the current underlying `DexKitBridge`
- The wrapper object itself remains usable
- The next access recreates the underlying bridge on demand
- Calling `create(...)` again with the same `appTag` usually returns the same wrapper object

This is also what `use { ... }` does by default, so it is suitable when you want to release resources right after use.

### `destroy()`

- Permanently retires the current `RecyclableBridge`
- Any later access to that wrapper throws an exception
- Calling `create(...)` again with the same `appTag` returns a new wrapper object

`destroy()` only handles bridge lifecycle. It does not automatically clear persistent cache. If you also want to remove cached data, call `clearCache(appTag)` or `clearAllCache()`.

## Query modes and key design

`DexKitCacheBridge` commonly supports three styles of usage.

### 1. Structured query + automatic key

```kotlin
val clazz = bridge.getClass {
    matcher {
        className("org.luckypray.dexkit.demo.PlayActivity")
    }
}
```

This form does not require you to provide a key manually. Internally, it generates a cache key from the query's `hashKey()`.  
It is suitable when the query condition itself is stable and you do not need to manage keys manually.

### 2. Structured query + explicit key

```kotlin
val clazz = bridge.getClass("play_activity") {
    matcher {
        className("org.luckypray.dexkit.demo.PlayActivity")
    }
}
```

This is useful when:

- you want cache keys with clear meaning for debugging or migration
- you want to read cache by key later without rebuilding the query
- you want constants, config files, or server-provided values to decide which cached result to read

If the cache already exists, you can also pass only the key:

```kotlin
val clazz = bridge.getClass("play_activity")
```

This means "read from cache only; do not run the query again". This is especially useful when a local config or a server-provided key decides which cached result should be used.  
If that key does not exist in cache, a `NoSuchElementException` is thrown.

### 3. Direct API

In some cases you cannot use a structured query and would rather write the lookup logic directly against `DexKitBridge`. For that, use the `*Direct` APIs:

```kotlin
val clazz = bridge.getClassDirect("play_activity_direct") {
    findClass {
        matcher {
            className("org.luckypray.dexkit.demo.PlayActivity")
        }
    }.single()
}
```

Direct APIs require an explicit key because they cannot infer a stable key from structured query semantics.

## Cached query forms

Common cached query forms include:

- single-result: `getClass` / `getMethod` / `getField`
- multi-result: `getClasses` / `getMethods` / `getFields`
- batch string queries: `getBatchUsingStringsClasses` / `getBatchUsingStringsMethods`
- direct variants: `getClassDirect` / `getMethodDirect` / `getFieldDirect` and others

If you just want temporary direct access to the underlying `DexKitBridge` for an operation that is not wrapped by cache APIs, use:

```kotlin
bridge.withBridge { rawBridge ->
    // Use DexKitBridge directly here
}
```

`withBridge` only provides a safe borrow for one operation: it acquires an available bridge on entry, returns the reference on exit, and participates in idle-release timing. It does not cache results for you automatically.

## Cache policy

Global cache behavior is controlled by `DexKitCacheBridge.cachePolicy`:

By default, `DexKitCacheBridge` caches successful results only, not failed states.  
That means if a single-result query fails deterministically, for example "not found" or "non-unique", the same lookup will still run again on the next startup.

If failures in your case are deterministic and worth remembering, you can cache failed states through `failurePolicy` to avoid repeating bridge creation and repeated failed lookups.  
But note that after host updates, query fixes, or adaptation logic changes, you need to clear stale failure cache yourself. Otherwise, the old failure state will keep being reused.

```kotlin
DexKitCacheBridge.cachePolicy = DexKitCacheBridge.CachePolicy(
    cacheSuccess = true,
    failurePolicy = DexKitCacheBridge.CacheFailurePolicy.NONE
)
```

### `cacheSuccess`

- `true`: cache successful results
- `false`: do not cache successful results, but bridge reuse and listeners still work

### `CacheFailurePolicy`

| Policy | Behavior |
| --- | --- |
| `NONE` | Do not cache failed results |
| `QUERY_ONLY` | Cache failed results only for single-result structured queries with automatic keys |
| `ALL` | Cache failed results for all single-result queries, including explicit-key and Direct APIs |

Here, "failed results" mainly refers to single-result queries throwing:

- `NoResultException`
- `NonUniqueResultException`

Multi-result and batch queries do not use this failed-result caching policy, because an empty list or empty group is already a valid way to express the result without the ambiguity of a single-result query.

::: tip
Changing `cachePolicy` at runtime only affects future queries. It does not rewrite or reinterpret cache entries that already exist. In most projects, it is better to configure this once during initialization.
:::

## Idle release: `idleTimeoutMillis`

```kotlin
DexKitCacheBridge.idleTimeoutMillis = 5_000L
```

The default value is `5000` milliseconds.

It controls how long a `RecyclableBridge` may stay idle before the underlying native bridge can be released automatically.  
This is a fallback release mechanism meant to avoid long-lived resource occupation when the caller forgets to release explicitly; it is **not** meant to encourage leaving bridges open. In normal usage, still prefer:

- `use { ... }` / try-with-resources
- or calling `close()` explicitly at the right time

The benefit is:

- frequent short-term accesses can still reuse the underlying bridge
- long idle periods do not keep resources occupied forever

You can think of its lifecycle like this:

```text
create / run query
    ↓
bridge is in use
    ↓  (no idle countdown while in use)
last user finishes / use block exits
    ↓
idleTimeoutMillis countdown starts
    ↓
used again before timeout      timeout reached and still unused
    ↓                          ↓
current countdown canceled     underlying DexKitBridge released automatically
```

Two details are worth keeping in mind:

- The condition here is whether the bridge is still being held by any caller, not a query-result field such as `QuerySuccessEvent.matchCount`.
- If a query is still running inside `use { ... }`, `withBridge { ... }`, or one of the cached APIs, the idle timer does not run. The countdown only starts after nothing is using that bridge anymore.

This does not conflict with manual `close()`:

- `close()` is an explicit release and has higher priority.
- If `close()` is called while a query is still running, release happens after the current use finishes, so ongoing work is not interrupted.

Like `cachePolicy`, this is best configured during initialization rather than toggled frequently during normal business logic.

## Listener events

You can observe query results and bridge lifecycle events through `CacheBridgeListener`:

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

Remember to remove the listener when you no longer need it:

```kotlin
DexKitCacheBridge.removeListener(listener)
```

### `QuerySuccessEvent`

- `appTag`: current host identity
- `queryKind`: query type, such as single-result, list, or batch query
- `requestKey`: explicit key; `null` for automatic-key queries
- `source`: where the result came from, `CACHE` or `QUERY`
- `matchCount`: number of matched results; for batch queries, the sum of all grouped result counts

### `QueryFailureEvent`

- `appTag`
- `queryKind`
- `requestKey`
- `source`
- `error`: the actual exception

Listeners are especially useful for:

- verifying whether cache is taking effect
- collecting cache-hit / query-progress statistics

## Cache cleanup

### Clear one `appTag`

```kotlin
DexKitCacheBridge.clearCache(appTag)
```

This is useful when:

- the host has changed but you do not want to switch `appTag` yet
- the meaning of some cache key has changed and you want to clear that namespace
- you want to wipe historical cache during debugging and verify the lookup flow again

### Clear all cache

```kotlin
DexKitCacheBridge.clearAllCache()
```

This only clears cache data. It does not automatically destroy existing `RecyclableBridge` instances.

## Best practices

- Call `DexKitCacheBridge.init(cache)` once during app startup.
- In most cases, configure `idleTimeoutMillis` and `cachePolicy` during the same initialization phase; only change them at runtime if you clearly understand the behavior you want.
- Make `appTag` reflect the actual host version or dex source; do not mix different host contents under the same tag.
- Keep explicit keys stable in meaning. If the query semantics change, change the key or clear the old cache.
- In daily use, still prefer timely `use { ... }` or manual `close()`; automatic idle release should be treated as a fallback, not a replacement for explicit release.
- Prefer persistent storage that handles larger content well; do not force large serialized payloads into `SharedPreferences`.
- If part of a batch-query cache is missing or corrupted, it is treated as a cache miss and will be rebuilt on the next query.
