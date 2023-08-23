<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

English | [简体中文](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

</div>

A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes,
methods, or properties.

---

# Welcome to DexKit 2.0

> Special Note:
> - The current version is actively under development, and there may be instability and missing features.
> - Due to a project rewrite, comprehensive testing and validation haven't been performed, leading to potential issues.
> - If you need a stable version, please use version 1.1.8.

## Supported APIs

Basic Features:

- [x] Multi-condition class search
- [x] Multi-condition method search
- [x] Multi-condition property search

⭐️ Distinctive Features (Recommended):

- [x] Batch search of classes using strings
- [x] Batch search of methods using strings

> Note: Optimizations have been implemented for string search scenarios, significantly enhancing 
> search speed. Increasing query groups will not lead to a linear increase in time consumption.

### Dependencies

Add `DexKit` dependency in `build.gradle`. 

```gradle
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.luckypray:DexKit:<version>'
}
```

DexKit current version: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

## Usage Example

Here's a simple usage example.

Suppose this class is what we want to obtain, with most of its names obfuscated and changing in each version.

> Sample app:


```java
package com.test.demo;

public class a extends Activity implements Serializable {
    
    public a(String var1) {
        super();
        // ...
    }
    
    private static final String TAG = "SplashActivity";
    
    private String a;
    
    private boolean b;
    
    protected void onCreate(Bundle var1) {
        super.onCreate(var1);
        Log.d("SplashActivity", "onCreate");
        // ...
    }
    
    private static void a(String var1, String var2) {
        // ...
    }
    
    private String a(String var1) {
        Log.d("SplashActivity", "load data");
        // ...
    }
    
    private void a() {
        // ...
    }
    
    // ...
    
}
```

At this point, to obtain this class, you can use the following code:

> This is just an example, in actual usage, there's no need for such an extensive set of matching 
> conditions. Choose and use as needed to avoid unnecessary complexity in matching due to an 
> excessive number of conditions.

```kotlin
class MainHook : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        val packageName = loadPackageParam.packageName
        val apkPath = loadPackageParam.appInfo.sourceDir
        if (packageName != "com.test.demo") {
            return
        }
        // need minSdkVersion >= 23
        System.loadLibrary("dexkit")
        DexKitBridge.create(apkPath)?.use { bridge ->
            bridge.findClass {
                // Search within the specified package name range
                searchPackage("com.test.demo")
                // ClassMatcher for class matching
                matcher {
                    // FieldsMatcher for matching properties within the class
                    fields {
                        // Add a matcher for properties
                        add {
                            type("java.lang.String")
                            name("TAG")
                        }
                        addForType("java.lang.String")
                        addForType("boolean")
                        // Specify the number of properties in the class
                        countRange(count = 3)
                    }
                    // MethodsMatcher for matching methods within the class
                    methods {
                        // Add a matcher for methods
                        add {
                            name("onCreate")
                            returnType("void")
                            parameterTypes("android.os.Bundle")
                            usingStrings("onCreate")
                        }
                        add {
                            modifiers(Modifier.PRIVATE or Modifier.STATIC)
                            returnType("void")
                            parameterTypes("java.lang.String", "java.lang.String")
                        }
                        // Specify the number of methods in the class, a minimum of 4, and a maximum of 10
                        countRange(min = 4, max = 10)
                    }
                    // Strings used by all methods in the class
                    useStrings("SplashActivity", "load data", "onCreate")
                }
            }.forEach {
                // Print the found class: com.test.demo.a
                println(it.className)
                // Get the corresponding class instance
                val clazz = it.getInstance(loadPackageParam.classLoader)
            }
        }
    }
}
```

### Documentation

- [Click here]() Documentation is currently being written and might take some time.
- [DexKit API KDoc]() Documentation is currently being written and might take some time.

## Third-Party Open Source References

- [slicer](https://cs.android.com/android/platform/superproject/+/master:tools/dexter/slicer/export/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## License

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
