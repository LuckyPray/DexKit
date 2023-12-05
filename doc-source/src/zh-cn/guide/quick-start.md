# 快速开始

> 集成 `DexKit` 到您的项目中。

## 环境要求

确保您的开发环境满足以下要求：

- JDK 8 及以上
- Kotlin 1.5 及以上
- AGP 4.2 及以上
- minSdkVersion 21 及以上（推荐 23 及以上）

::: warning
如果您的项目的 minSdkVersion 小于 23，在 Xposed 模块内使用 `System.loadLibrary("dexkit")` 时可能会抛出
`java.lang.UnsatisfiedLinkError: xxx couldn't find "libdexkit.so"` 的异常。这是因为打包时默认会压缩
`lib/` 目录下的 so 文件，导致无法通过 `System.loadLibrary` 加载 so 文件。解决方案是在 `app/build.gradle`
中添加以下配置：
```groovy
android {
    packagingOptions {
        jniLibs {
            useLegacyPackaging true
        }
    }
}
```
或者手动解压 apk 内的 lib/ 目录下的 libdexkit.so 文件至任意可读写目录，然后通过 
System.load("/path/to/libdexkit.so") 加载。
:::

## 集成依赖

> 在您的项目中的 `app/build.gradle` 或者 `app/build.gradle.kts` 添加 `dexkit` 的依赖。

DexKit 当前版本: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

:::: code-group
::: code-group-item gradle
```groovy
dependencies {
    // 将 <version> 替换为您需要的版本，例如 '2.0.0-rc1'
    implementation 'org.luckypray:dexkit:<version>'
}
```
:::
::: code-group-item gradle-kts
```kotlin
dependencies {
    // 将 <version> 替换为您需要的版本，例如 '2.0.0-rc1'
    implementation("org.luckypray:dexkit:<version>")
}
```
:::
::::

::: tip
从 **DexKit 2.0** 开始，新的 ArtifactId 已从 `DexKit` 更改为 `dexkit`。
:::

现在您已经成功集成了 `DexKit` 到您的项目中，接下来我们将会介绍如何使用 `DexKit` 来完成一些常见的需求。

