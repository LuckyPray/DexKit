# Quick Start

> Integrate `DexKit` into your project.

## Environment Requirements

Make sure your development environment meets the following requirements:

- JDK 8 and above
- Kotlin 1.5 and above
- AGP 4.2 and above
- minSdkVersion 21 and above (recommended 23 and above)

::: warning
If your project's minSdkVersion is less than 23, using `System.loadLibrary("dexkit")` within an 
Xposed module may throw a `java.lang.UnsatisfiedLinkError: xxx couldn't find "libdexkit.so"` 
exception. This is because the so files under the `lib/` directory are compressed by default 
during packaging, making it impossible to load the so file via `System.loadLibrary`. 
The solution is to add the following configuration in `app/build.gradle`:
```groovy
android {
    packagingOptions {
        jniLibs {
            useLegacyPackaging true
        }
    }
}
```
or manually extract the libdexkit.so file from the lib/ directory within the apk to any 
readable and writable directory, and then load it using `System.load("/path/to/libdexkit.so")`.
:::

## Integration Dependency

> Add the dependency for dexkit in your project's `app/build.gradle` or `app/build.gradle.kts`.

Current DexKit version: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

:::: code-group
::: code-group-item gradle
```groovy
dependencies {
    implementation 'org.luckypray:dexkit:<version>'
}
```
:::
::: code-group-item gradle-kts
```kotlin
dependencies {
    implementation("org.luckypray:dexkit:<version>")
}
```
:::
::::

::: tip
Starting from **DexKit 2.0**, the new ArtifactId has been changed from `DexKit` to `dexkit`.
:::

Now that you have successfully integrated `DexKit` into your project, next we will introduce 
how to use `DexKit` to achieve some common requirements.

