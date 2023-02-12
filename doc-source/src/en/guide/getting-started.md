# Quick Start

> To add `DexKit` to your project, you can use `Gradle` or `Maven` to manage dependencies.

## Installing

> Add `DexKit` dependency in `app/build.gradle / app/build.gradle.kts`

::: warning warning
Starting from version 1.1.0, `DexKit` has been migrated from jitpack.io to mavenCentral.
If you are still using jitpack.io in your project, please migrate as soon as possible.
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

::: warning warning
Because DexKit is still in an early stage, the API may change with version upgrades, so please check the release notes before updating.
:::

Now that you have successfully integrated `DexKit` into your project,
we will introduce how to use `DexKit` to fulfill some common requirements.

> You first need to call `System.loadLibrary("dexkit")` to load the dynamic library.
> If you encounter `Java.lang.UnsatisfiedLinkError`, please decompress the `.so` file corresponding
> to your ABI and use `System.load(path)` to load it.
