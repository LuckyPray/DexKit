<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

English | [简体中文](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

</div>

A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes,
methods, or properties.

---

# DexKit 2.0

At present, the 2.0 API is basically stable and needs to undergo final testing before it can be officially released.
Documentation and comments will be gradually improved in the future.

## Supported APIs

Basic Features:

- [x] Multi-condition class search
- [x] Multi-condition method search
- [x] Multi-condition field search
- [x] Extract `Annotation` of classes/methods/fields/parameters

⭐️ Distinctive Features (Recommended):

- [x] Batch search of classes using strings
- [x] Batch search of methods using strings

> Note: Optimizations have been implemented for string search scenarios, significantly enhancing 
> search speed. Increasing query groups will not lead to a linear increase in time consumption.

### Dependencies

Add `dexkit` dependency in `build.gradle`. 

```gradle
repositories {
    mavenCentral()
}
dependencies {
    implementation 'org.luckypray:dexkit:<version>'
}
```

> **Note**
> Starting with **DexKit 2.0**, the new ArtifactId has been changed from `DexKit` to `dexkit`.

DexKit current version: [![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)

## Usage Example

Here's a simple usage example.

Suppose this class is what we want to obtain, with most of its names obfuscated and changing in each version.

> Sample app:


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
        Log.d(TAG, "onClick: rollButton");
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
        Log.d(TAG, "rollDice: " + a);
    }

    protected void onCreate(Bundle bundle) {
        super/*androidx.fragment.app.FragmentActivity*/.onCreate(bundle);
        setContentView(0x7f0b001d);
        Log.d(TAG, "onCreate");
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.b = new PlayActivity$1(this, handlerThread.getLooper());
        this.a = (TextView) findViewById(0x7f080134);
        ((Button) findViewById(0x7f08013a)).setOnClickListener(new a(this));
    }
}
```

At this point, to obtain this class, you can use the following code:

> This is just an example, in actual usage, there's no need for such an extensive set of matching 
> conditions. Choose and use as needed to avoid unnecessary complexity in matching due to an 
> excessive number of conditions.

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
        // !!! Remember to call bridge.close() after use to release the memory !!!
        // 
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        bridge.findClass(FindClass.create()
            // Search within the specified package name range
            .searchPackages("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher for class matching
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher for matching properties within the class
                .fields(FieldsMatcher.create()
                    // Add a matcher for properties
                    .add(FieldMatcher.create()
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        .type("java.lang.String")
                        .name("TAG")
                    )
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // Specify the number of properties in the class
                    .count(3)
                )
                // MethodsMatcher for matching methods within the class
                .methods(MethodsMatcher.create()
                    // Add a matcher for methods
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
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    .count(1, 10)
                )
                // AnnotationsMatcher for matching interfaces within the class
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
                // Strings used by all methods in the class
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).forEach(classData -> {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            System.out.println(classData.getClassName());
            // Get the corresponding class instance
            Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
        });
        //
        // The native cache must be released, avoid memory leaks !!!
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
        if (packageName != "org.luckypray.dexkit.demo") {
            return
        }
        // need minSdkVersion >= 23
        System.loadLibrary("dexkit")
        //
        // !!! Remember to call bridge.close() after use to release the memory !!!
        // 
        val bridge = DexKitBridge.create(apkPath) 
            ?: throw NullPointerException("DexKitBridge.create() failed")
        bridge.findClass {
            // Search within the specified package name range
            searchPackages = listOf("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            excludePackages = listOf("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher for class matching
            matcher {
                className = "org.luckypray.dexkit.demo.PlayActivity"
                // FieldsMatcher for matching properties within the class
                fields {
                    // Add a matcher for properties
                    add {
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                        type = "java.lang.String"
                        name = "TAG"
                    }
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // Specify the number of properties in the class
                    count = 3
                }
                // MethodsMatcher for matching methods within the class
                methods {
                    // Add a matcher for methods
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
                        modifiers = Modifier.PUBLIC
                        paramTypes = listOf("boolean")
                    }
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    count(1..10)
                }
                // AnnotationsMatcher for matching interfaces within the class
                annotations {
                    add {
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        addElement {
                            name = "path"
                            stringValue("/play")
                        }
                    }
                }
                // Strings used by all methods in the class
                usingStrings = listOf("PlayActivity", "onClick", "onCreate")
            }
        }.forEach {
            // Print the found class: org.luckypray.dexkit.demo.PlayActivity
            println(it.className)
            // Get the corresponding class instance
            val clazz = it.getInstance(loadPackageParam.classLoader)
        }
        //
        // The native cache must be released, avoid memory leaks !!!
        //
        bridge.close()
    }
}
```

</p></details>

### Documentation

- [Click here]() Documentation is currently being written and might take some time.
- [DexKit API KDoc]() Documentation is currently being written and might take some time.

## Third-Party Open Source References

- [slicer](https://cs.android.com/android/platform/superproject/+/main:tools/dexter/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## License

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
