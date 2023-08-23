<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

[English](https://github.com/LuckyPray/DexKit/blob/master/README.md) | 简体中文

</div>

一个使用 C++ 实现的 dex 高性能运行时解析库，用于查找被混淆的类、方法或者属性。

---

# 欢迎使用 DexKit 2.0

> 特别说明：
> - 当前版本正在积极开发中，可能存在不稳定性和功能缺失
> - 由于项目进行了重写，未进行充分的测试和验证，可能存在潜在问题
> - 如果需您需要稳定版本，请使用 1.1.8 版本

## 支持的 API

基础功能：

- [x] 多条件查找类
- [x] 多条件查找方法
- [x] 多条件查找属性

⭐️ 特色功能（推荐）：

- [x] 批量查找使用字符串的类
- [x] 批量查找使用字符串的方法

> 备注：对于字符串搜索场景进行了优化，可以大幅度提升搜索速度，增加查询分组不会导致耗时成倍增长

### 依赖

添加 `DexKit` 依赖进 `build.gradle`.

```gradle
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.luckypray:DexKit:<version>'
}
```

DexKit 当前版本: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

## 使用样例

下面是一个简单的用法示例。

假设这个 Class 是我们想得到的，其中大部分名称经过混淆，且每个版本都会发生变化。

> 样例 APP 如下

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

此时我们想得到这个类可以使用如下代码：

> 这仅仅是个样例，实际使用中并不需要这么多条件进行匹配，按需选用即可，避免条件过多带来的匹配复杂度增长

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
                // 从指定的包名范围内进行查找
                searchPackage("com.test.demo")
                // ClassMatcher 针对类的匹配器
                matcher {
                    // FieldsMatcher 针对类中包含属性的匹配器
                    fields {
                        // 添加对于属性的匹配器
                        add {
                            type("java.lang.String")
                            name("TAG")
                        }
                        addForType("java.lang.String")
                        addForType("boolean")
                        // 指定类中属性的数量
                        countRange(count = 3)
                    }
                    // MethodsMatcher 针对类中包含方法的匹配器
                    methods {
                        // 添加对于方法的匹配器
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
                        // 指定类中方法的数量，最少不少于4个，最多不超过10个
                        countRange(min = 4, max = 10)
                    }
                    // 类中所有方法使用的字符串
                    useStrings("SplashActivity", "load data", "onCreate")
                }
            }.forEach {
                // 打印查找到的类: com.test.demo.a
                println(it.className)
                // 获取对应的类实例
                val clazz = it.getInstance(loadPackageParam.classLoader)
            }
        }
    }
}
```

### 使用文档

- [点击此处]() 文档正在编写中，可能需要一段时间
- [DexKit API KDoc]() 文档正在编写中，可能需要一段时间

## 第三方开源引用

- [slicer](https://cs.android.com/android/platform/superproject/+/master:tools/dexter/slicer/export/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## 许可证

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
