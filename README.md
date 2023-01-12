<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.luckypray%22%20AND%20a:%22DexKit%22)

English | [简体中文](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

</div>

A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes,
methods, or properties.

---

## Background

For `Xposed` modules, we often need to `Hook` specific methods, but due to obfuscation, we need to use means 
to find the methods we need. However, for `JVM` languages, the information we can obtain at runtime is limited. 
In the past, we looked up obfuscated methods by traversing all classes in the ClassLoader, filtering by 
package name, the number of methods contained in the class, and method signature. This approach is not only 
very inefficient, but also helpless in the case of completely obfuscated package names.

So, do we have other ways? The answer is yes. `ProGuard` obfuscation rules only obscure class names, method names, 
and property names, but they do not modify code logic, and there are usually no major code changes during minor updates. 
Therefore, we can reverse search for the information we need by parsing bytecode.

Currently, there are many libraries for parsing Dex files, but most are implemented based on `dexlib2`.
If the host application has a large number of Dex files, the search time can take several minutes, 
which is a poor user experience. That's where `DexKit` comes in. It is implemented in C++ and uses 
multi-threading acceleration to complete searches in a short amount of time. It has extremely high performance, 
with search times in the milliseconds range and support for multi-threaded concurrent searches. 
Even for large applications with 30+ Dex files, `DexKit` can complete a single search in about 100 milliseconds. 
In addition, it is also optimized for string search scenarios, even if you want to search hundreds of strings, 
it only takes no more than twice the time to complete.

## Supported features

- Batch search methods/classes with a specific string
- Find methods/classes using a specific string
- Method call/called search
- Direct subclass search
- Method multi-condition search
- Op sequence search (only supports standard dex instructions)
- Annotation search (currently only supports searching for values that are strings)

> **Note**:
> This is the early stage of the project, and we cannot guarantee that the API will not change in the future.
> If you have any suggestions or opinions, please let us know.

## Usage

### Library

Add `DexKit` dependency in `build.gradle`.

```gradle
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.luckypray:DexKit:<version>'
}
```

### Usage Sample

Sample App:

```java
public class abc {
    
    public boolean cvc() {
        boolean b = false;
        // ...
        Log.d("VipCheckUtil", "userInfo: xxxx");
        // ...
        return b;
    }
}
```

Sample Hook:

```kotlin
class MainHook : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        val packageName = loadPackageParam.packageName
        val apkPath = loadPackageParam.appInfo.sourceDir
        if (packageName != "com.test.demo") {
            return
        }
        System.loadLibrary("dexkit")
        DexKitBridge.create(apkPath)?.use { bridge ->
            val resultMap = bridge.batchFindMethodsUsingStrings {
                addQuery("VipCheckUtil_isVip", setOf("VipCheckUtil", "userInfo:"))
            }.firstOrNull()?.let {
                val classDescriptor = it.value.first()
                val method: Method = classDescriptor.getMethodInstance(hostClassLoader)
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
            } ?: Log.e("DexKit", "search result empty")
        }
    }
}
```

### Usage Document

- [Click here](https://luckypray.org/DexKit/en/) to go to the documentation page to view more detailed tutorials.
- [DexKit API KDoc](https://luckypray.org/DexKit-Doc) is a KDoc (similar to JavaDoc) generated from source code comments.
However, it is recommended to use an IDE such as IDEA to view source code comments during development.

## Open source reference

- [slicer](https://cs.android.com/android/platform/superproject/+/master:tools/dexter/slicer/export/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [BiliRoaming](https://github.com/yujincheng08/BiliRoaming)

## License

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
