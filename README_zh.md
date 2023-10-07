<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

[English](https://github.com/LuckyPray/DexKit/blob/master/README.md) | 简体中文

</div>

一个使用 C++ 实现的 dex 高性能运行时解析库，用于查找被混淆的类、方法或者属性。

---

# DexKit 2.0

目前 2.0 API 基本稳定，需要经过最终测试才能正式发布。 文档和注释今后会逐步完善。

## 支持的 API

基础功能：

- [x] 多条件查找类
- [x] 多条件查找方法
- [x] 多条件查找属性
- [x] 提取类/方法/属性/参数的注解

⭐️ 特色功能（推荐）：

- [x] 批量查找使用字符串的类
- [x] 批量查找使用字符串的方法

> 备注：对于字符串搜索场景进行了优化，可以大幅度提升搜索速度，增加查询分组不会导致耗时成倍增长

### 依赖

添加 `dexkit` 依赖进 `build.gradle`.

```gradle
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.luckypray:dexkit:<version>'
}
```

> **Note**
> 从 **DexKit 2.0** 开始，新的 ArtifactId 已从 `DexKit` 更改为 `dexkit`。

DexKit 当前版本: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

## 使用样例

下面是一个简单的用法示例。

假设这个 Class 是我们想得到的，其中大部分名称经过混淆，且每个版本都会发生变化。

> 样例 APP 如下

```java
package org.luckypray.dexkit.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.h;
import java.util.Random;
import org.luckypray.dexkit.demo.annotations.Router;

@Router(path = "/play")
public class PlayActivity extends AppCompatActivity {
    private static final String TAG = "PlayActivity";
    private TextView a;
    private Handler b;

    public void d(View view) {
        Handler handler;
        int i;
        Log.d("PlayActivity", "onClick: rollButton");
        float nextFloat = new Random().nextFloat();
        if (nextFloat < 0.01d) {
            handler = this.b;
            i = -1;
        } else if (nextFloat < 0.987f) {
            handler = this.b;
            i = 0;
        } else {
            handler = this.b;
            i = 114514;
        }
        handler.sendEmptyMessage(i);
    }

    public void e(boolean z) {
        int i;
        if (!z) {
            i = RandomUtil.a();
        } else {
            i = 6;
        }
        String a = h.a("You rolled a ", i);
        this.a.setText(a);
        Log.d("PlayActivity", "rollDice: " + a);
    }

    protected void onCreate(Bundle bundle) {
        super/*androidx.fragment.app.FragmentActivity*/.onCreate(bundle);
        setContentView(0x7f0b001d);
        Log.d("PlayActivity", "onCreate");
        HandlerThread handlerThread = new HandlerThread("PlayActivity");
        handlerThread.start();
        this.b = new PlayActivity$1(this, handlerThread.getLooper());
        this.a = (TextView) findViewById(0x7f080134);
        ((Button) findViewById(0x7f08013a)).setOnClickListener(new a(this));
    }
}
```

此时我们想得到这个类可以使用如下代码：

> 这仅仅是个样例，实际使用中并不需要这么多条件进行匹配，按需选用即可，避免条件过多带来的匹配复杂度增长

<details><summary>Java Example</summary>
<p>

```java
public class MainHook implements IXposedHookLoadPackage {
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String packageName = loadPackageParam.packageName;
        String apkPath = loadPackageParam.appInfo.sourceDir;
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return;
        }
        // need minSdkVersion >= 23
        System.loadLibrary("dexkit");
        //
        // !!! 切记使用完成后调用 bridge.close() 释放内存 !!!
        //
        // 或者使用 try-with-resources 语法
        //
        // Example:
        //     try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
        //         // bridge.findClass(...)
        //         // bridge.findMethod(...)
        //         // bridge.findField(...)
        //     }
        // 
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        bridge.findClass(FindClass.create()
            // 从指定的包名范围内进行查找
            .searchPackages("org.luckypray.dexkit.demo")
            // 排除指定的包名范围
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher 针对类的匹配器
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher 针对类中包含属性的匹配器
                .fields(FieldsMatcher.create()
                    // 添加对于属性的匹配器
                    .add(FieldMatcher.create()
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        .type("java.lang.String")
                        .name("TAG")
                    )
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // 指定类中属性的数量
                    .count(3)
                )
                // MethodsMatcher 针对类中包含方法的匹配器
                .methods(MethodsMatcher.create()
                    // 添加对于方法的匹配器
                    .methods(List.of(
                        MethodMatcher.create()
                            .modifiers(Modifier.PROTECTED)
                            .name("onCreate")
                            .returnType("void")
                            .paramTypes("android.os.Bundle")
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                    ))
                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                    .count(1, 10)
                )
                // AnnotationsMatcher 针对类中包含注解的匹配器
                .annotations(AnnotationsMatcher.create()
                    .add(AnnotationMatcher.create()
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        .addElement(
                            AnnotationElementMatcher.create()
                                .name("path")
                                .stringValue("/play")
                        )
                    )
                )
                // 类中所有方法使用的字符串
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).forEach(classData -> {
            // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
            System.out.println(classData.getName());
            // 获取对应的类实例
            Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
        });
        //
        // !!! native 缓存一定要释放，避免内存泄漏 !!!
        //
        bridge.close();
    }
}
```

</p></details>

<details open><summary>Kotlin Example</summary>
<p>

```kotlin
class MainHook : IXposedHookLoadPackage {
    
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        val packageName = loadPackageParam.packageName
        val apkPath = loadPackageParam.appInfo.sourceDir
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return
        }
        // need minSdkVersion >= 23
        System.loadLibrary("dexkit")
        //
        // !!! 切记使用完成后调用 bridge.close() 释放内存 !!!
        //
        // 或者使用 Kotlin 的扩展函数 `Closeable.use {}` 自动释放内存
        //
        // Example:
        //     DexKitBridge.create(apkPath).use { bridge ->
        //         // bridge.findClass { ... }
        //         // bridge.findMethod { ... }
        //         // bridge.findField { ... }
        //     }
        // 
        val bridge = DexKitBridge.create(apkPath)
            ?: throw NullPointerException("DexKitBridge.create() failed")
        bridge.findClass {
            // 从指定的包名范围内进行查找
            searchPackages = listOf("org.luckypray.dexkit.demo")
            // 排除指定的包名范围
            excludePackages = listOf("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher 针对类的匹配器
            matcher {
                className = "org.luckypray.dexkit.demo.PlayActivity"
                // FieldsMatcher 针对类中包含属性的匹配器
                fields {
                    // 添加对于属性的匹配器
                    add {
                        modifiers(Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL)
                        type("java.lang.String")
                        name("TAG")
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // 指定类中属性的数量
                    count = 3
                }
                // MethodsMatcher 针对类中包含方法的匹配器
                methods {
                    // 添加对于方法的匹配器
                    add {
                        modifiers = Modifier.PROTECTED
                        name = "onCreate"
                        returnType = "void"
                        paramTypes = listOf("android.os.Bundle")
                        usingStrings = listOf("onCreate")
                    }
                    add {
                        paramTypes = listOf("android.view.View")
                        usingNumbers = listOf(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        paramTypes = listOf("boolean")
                    }
                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                    count(1..10)
                }
                // AnnotationsMatcher 针对类中包含注解的匹配器
                annotations {
                    add {
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        addElement {
                            name = "path"
                            stringValue("/play")
                        }
                    }
                }
                // 类中所有方法使用的字符串
                usingStrings = listOf("PlayActivity", "onClick", "onCreate")
            }
        }.forEach {
            // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
            println(it.name)
            // 获取对应的类实例
            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
        //
        // !!! native 缓存一定要释放，避免内存泄漏 !!!
        //
        bridge.close()
    }
}
```

</p></details>

### 使用文档

- [点击此处]() 文档正在编写中，可能需要一段时间
- [DexKit API KDoc]() 文档正在编写中，可能需要一段时间

## 第三方开源引用

- [slicer](https://cs.android.com/android/platform/superproject/+/main:tools/dexter/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=luckypray/dexkit&type=Date)](https://star-history.com/#luckypray/dexkit&Date)

## 许可证

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
