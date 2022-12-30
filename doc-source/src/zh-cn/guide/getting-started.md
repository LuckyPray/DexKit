# 快速开始

> 集成 `DexKit` 到你的项目中，你可以使用 `Gradle` 或者 `Maven` 来管理依赖。

## 集成依赖

> 在你的项目中的 `app/build.gradle` 或者 `app/build.gradle.kts` 添加 `DexKit` 的依赖。

::: warning 注意
从 1.0.3 版本开始，`DexKit` 已经从 jitpack.io 迁移到了 mavenCentral，如果你的项目中还在使用 jitpack.io 的话，请尽快迁移。
:::

:::: code-group
::: code-group-item gradle
```groovy
dependencies {
    implementation 'org.luckypray:DexKit:<version>'
}
```
:::
::: code-group-item gradle-kts
```kotlin
dependencies {
    implementation("org.luckypray:DexKit:<version>")
}
```
:::
::::

::: warning 注意
由于项目处于初期阶段，可能在版本升级中会出现 API 变动，所以请在使用时，注意查看版本更新日志。
:::

现在你已经成功集成了 `DexKit` 到你的项目中，接下来我们将会介绍如何使用 `DexKit` 来完成一些常见的需求。
