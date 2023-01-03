# Quick Start

> Integrate `DexKit` into your project, you can use `Gradle` or `Maven` to manage dependencies.

## Integration dependencies

> Add `DexKit` dependency in `app/build.gradle` or `app/build.gradle.kts` in your project.

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
Because the project is in the early stage, there may be API changes during version upgrades, 
so please check the release notes when using.
:::

Now that you have successfully integrated `DexKit` into your project, 
we will introduce how to use `DexKit` to fulfill some common requirements.
