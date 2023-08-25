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
> - 如果需您需要稳定版本，请使用 [1.1.8](https://github.com/LuckyPray/DexKit/tree/1.1.x) 版本

## 支持的 API

基础功能：

- [x] 多条件查找类
- [x] 多条件查找方法
- [x] 多条件查找属性
- [x] 获取类/方法/属性/参数的注解

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
    implementation 'org.luckypray:dexkit:<version>'
}
```

DexKit 当前版本: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

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
    private final String a = "PlayActivity";
    private TextView b;
    private Handler c;

    public void d(View view) {
        Handler handler;
        int i;
        Log.d("PlayActivity", "onClick: rollButton");
        if (new Random().nextFloat() < 0.987f) {
            handler = this.c;
            i = 0;
        } else {
            handler = this.c;
            i = 114514;
        }
        handler.sendEmptyMessage(i);
    }

    public void e(boolean z) {
        int i;
        if (!z) {
            i = g.a();
        } else {
            i = 6;
        }
        String a = h.a("You rolled a ", i);
        this.b.setText(a);
        Log.d("PlayActivity", "rollDice: " + a);
    }

    protected void onCreate(Bundle bundle) {
        super/*androidx.fragment.app.FragmentActivity*/.onCreate(bundle);
        setContentView(0x7f0b001d);
        Log.d("PlayActivity", "onCreate");
        HandlerThread handlerThread = new HandlerThread("PlayActivity");
        handlerThread.start();
        this.c = new e(this, handlerThread.getLooper());
        this.b = (TextView) findViewById(0x7f080134);
        ((Button) findViewById(0x7f08013a)).setOnClickListener(new b(this));
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
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            bridge.findClass(FindClass.create()
                    // 从指定的包名范围内进行查找
                    .searchPackage("org.luckypray.dexkit.demo")
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
                                    .countRange(3)
                            )
                            // MethodsMatcher 针对类中包含方法的匹配器
                            .methods(MethodsMatcher.create()
                                    // 添加对于方法的匹配器
                                    .methods(List.of(
                                            MethodMatcher.create()
                                                    .modifiers(Modifier.PROTECTED)
                                                    .name("onCreate")
                                                    .returnType("void")
                                                    .parameterTypes("android.os.Bundle")
                                                    .usingStrings("onCreate"),
                                            MethodMatcher.create()
                                                    .parameterTypes("android.view.View")
                                                    .usingNumbers(
                                                            List.of(
                                                                    createInt(114514),
                                                                    createFloat(0.987f)
                                                            )
                                                    ),
                                            MethodMatcher.create()
                                                    .modifiers(Modifier.PUBLIC)
                                                    .parameterTypes("boolean")
                                    ))
                                    // 指定类中方法的数量，最少不少于1个，最多不超过10个
                                    .countRange(1, 10)
                            )
                            // AnnotationsMatcher 针对类中包含注解的匹配器
                            .annotations(AnnotationsMatcher.create()
                                    .add(AnnotationMatcher.create()
                                            .typeName("Router", StringMatchType.EndWith)
                                            .addElement(
                                                    AnnotationElementMatcher.create()
                                                            .name("path")
                                                            .matcher(createString("/play"))
                                            )
                                    )
                            )
                            // 类中所有方法使用的字符串
                            .usingStrings("PlayActivity", "onClick", "onCreate")
                    )
            ).forEach(classData -> {
                // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
                System.out.println(classData.getClassName());
                // 获取对应的类实例
                Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
            });
        }
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
        DexKitBridge.create(apkPath)?.use { bridge ->
            bridge.findClass {
                // 从指定的包名范围内进行查找
                searchPackage("org.luckypray.dexkit.demo")
                // ClassMatcher 针对类的匹配器
                matcher {
                    className("org.luckypray.dexkit.demo.PlayActivity")
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
                        countRange(count = 3)
                    }
                    // MethodsMatcher 针对类中包含方法的匹配器
                    methods {
                        // 添加对于方法的匹配器
                        add {
                            modifiers(Modifier.PROTECTED)
                            name("onCreate")
                            returnType("void")
                            parameterTypes("android.os.Bundle")
                            usingStrings("onCreate")
                        }
                        add {
                            parameterTypes("android.view.View")
                            usingNumbers {
                                add {
                                    intValue(114514)
                                }
                                add {
                                    floatValue(0.987f)
                                }
                            }
                        }
                        add {
                            parameterTypes("boolean")
                        }
                        // 指定类中方法的数量，最少不少于1个，最多不超过10个
                        countRange(1..10)
                    }
                    // AnnotationsMatcher 针对类中包含注解的匹配器
                    annotations {
                        add {
                            typeName("org.luckypray.dexkit.demo.annotations.Router")
                            elements {
                                add {
                                    name("path")
                                    matcher {
                                        stringValue("/play")
                                    }
                                }
                            }
                        }
                    }
                    // 类中所有方法使用的字符串
                    usingStrings("PlayActivity", "onClick", "onCreate")
                }
            }.forEach {
                // 打印查找到的类: org.luckypray.dexkit.demo.PlayActivity
                println(it.className)
                // 获取对应的类实例
                val clazz = it.getInstance(loadPackageParam.classLoader)
            }
        }
    }
}
```

</p></details>

### 使用文档

- [点击此处]() 文档正在编写中，可能需要一段时间
- [DexKit API KDoc]() 文档正在编写中，可能需要一段时间

## 第三方开源引用

- [slicer](https://cs.android.com/android/platform/superproject/+/master:tools/dexter/slicer/export/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## 许可证

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
