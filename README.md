<h1 align="center">DexKit</h1>

<p align="center">
    <a href="https://www.gnu.org/licenses/lgpl-3.0.html"><img loading="lazy" src="https://img.shields.io/github/license/LuckyPray/DexKit.svg?logo=github&label=License"/></a>
    <a href="https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray"><img loading="lazy" src="https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?logo=apachemaven&20Version&label=Maven%20Central"/></a>
    <a href="https://t.me/LuckyPray_DexKit"><img loading="lazy" src="https://img.shields.io/badge/Telegram-blue.svg?logo=telegram&label=Discussion%20group"/></a>
</p>

<p align="center">
    🇬🇧 <strong><ins>English</ins></strong> | <a href="./README_es.md">🇪🇸 <strong>Español</strong></a> | <a href="./README_zh.md">🇨🇳 <strong>简体中文</strong></a>
</p>

<p align="center"><strong>A high-performance runtime parsing library for dex implemented in C++, used for lookup of obfuscated classes, methods, or properties.</strong></p>

---

# DexKit 2.0

Currently 2.0 has been officially released, please refer to Release Notes for related improvements.

## Supported APIs

Basic Features:

- [x] Multi-condition class search
- [x] Multi-condition method search
- [x] Multi-condition field search
- [x] Provides multiple metadata APIs to obtain field/method/class related data

⭐️ Distinctive Features (Recommended):

- [x] Batch search of classes using strings
- [x] Batch search of methods using strings

> [!NOTE]
> Optimizations have been implemented for string search scenarios, significantly enhancing 
> search speed. Increasing query groups will not lead to a linear increase in time consumption.

### Documentation

- [Click here](https://luckypray.org/DexKit/en/) to go to the documentation page to view more detailed tutorials.

### Dependencies

Add `dexkit` dependency in `build.gradle`. 

```gradle
repositories {
    mavenCentral()
}
dependencies {
    // replace <version> with your desired version, e.g. `2.0.0`
    implementation 'org.luckypray:dexkit:<version>'
}
```

> [!IMPORTANT]
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

At this point, to obtain this class, you can use the following code:

> [!NOTE]
> This is just an example, in actual usage, there's no need for such an extensive set of matching 
> conditions. Choose and use as needed to avoid unnecessary complexity in matching due to an 
> excessive number of conditions.

<details><summary>Java Example</summary>
<p>

```java
public class MainHook implements IXposedHookLoadPackage {
    
    static {
        System.loadLibrary("dexkit");
    }
    
    private ClassLoader hostClassLoader;
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String packageName = loadPackageParam.packageName;
        String apkPath = loadPackageParam.appInfo.sourceDir;
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return;
        }
        this.hostClassLoader = loadPackageParam.classLoader;
        // DexKit creation is a time-consuming operation, please do not create the object repeatedly. 
        // If you need to use it globally, please manage the life cycle yourself and ensure 
        // that the .close() method is called when not needed to prevent memory leaks.
        // Here we use `try-with-resources` to automatically close the DexKitBridge instance.
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            findPlayActivity(bridge);
            // Other use cases
        }
    }
    
    private void findPlayActivity(DexKitBridge bridge) {
        ClassData classData = bridge.findClass(FindClass.create()
            // Search within the specified package name range
            .searchPackages("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher Matcher for classes
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher Matcher for fields in a class
                .fields(FieldsMatcher.create()
                    // Add a matcher for the field
                    .add(FieldMatcher.create()
                        // Specify the modifiers of the field
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        // Specify the type of the field
                        .type("java.lang.String")
                        // Specify the name of the field
                        .name("TAG")
                    )
                    // Add a matcher for the field of the specified type
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // Specify the number of fields in the class
                    .count(3)
                )
                // MethodsMatcher Matcher for methods in a class
                .methods(MethodsMatcher.create()
                    // Add a matcher for the method
                    .methods(List.of(
                        MethodMatcher.create()
                            // Specify the modifiers of the method
                            .modifiers(Modifier.PROTECTED)
                            // Specify the name of the method
                            .name("onCreate")
                            // Specify the return type of the method
                            .returnType("void")
                            // Specify the parameter type of the method
                            .paramTypes("android.os.Bundle")
                            // Specify the strings used by the method
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            // Specify the numbers used in the method, the type is Byte, Short, Int, Long, Float, Double
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                            // Specify the method invoke the methods list
                            .invokeMethods(MethodsMatcher.create()
                                .add(MethodMatcher.create()
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType("int")
                                    // be invoke method using strings
                                    .usingStrings(List.of("getRandomDice: "), StringMatchType.Equals)
                                )
                                // Only need to contain the call to the above method
                                .matchType(MatchType.Contains)
                            )
                    ))
                    // Specify the number of methods in the class, a minimum of 1, and a maximum of 10
                    .count(1, 10)
                )
                // AnnotationsMatcher Matcher for annotations in a class
                .annotations(AnnotationsMatcher.create()
                    // Add a matcher for the annotation
                    .add(AnnotationMatcher.create()
                        // Specify the type of the annotation
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        // The annotation needs to contain the specified element
                        .addElement(AnnotationElementMatcher.create()
                            // Specify the name of the element
                            .name("path")
                            // Specify the value of the element
                            .stringValue("/play")
                        )
                    )
                )
                // Strings used by all methods in the class
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).singleOrThrow(() -> new IllegalStateException("The returned result is not unique"));
        // Print the found class: org.luckypray.dexkit.demo.PlayActivity
        System.out.println(classData.getName());
        // Get the corresponding class instance
        Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
    }
}
```

</p></details>

<details open><summary>Kotlin Example</summary>
<p>

```kotlin
class MainHook : IXposedHookLoadPackage {
    
    companion object {
        init {
            System.loadLibrary("dexkit")
        }
    }

    private lateinit var hostClassLoader: ClassLoader

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        val packageName = loadPackageParam.packageName
        val apkPath = loadPackageParam.appInfo.sourceDir
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return
        }
        this.hostClassLoader = loadPackageParam.classLoader
        // DexKit creation is a time-consuming operation, please do not create the object repeatedly. 
        // If you need to use it globally, please manage the life cycle yourself and ensure 
        // that the .close() method is called when not needed to prevent memory leaks.
        // Here we use `Closable.use` to automatically close the DexKitBridge instance.
        DexKitBridge.create(apkPath).use { bridge ->
            findPlayActivity(bridge)
            // Other use cases
        }
    }

    private fun findPlayActivity(bridge: DexKitBridge) {
        val classData = bridge.findClass {
            // Search within the specified package name range
            searchPackages("org.luckypray.dexkit.demo")
            // Exclude the specified package name range
            excludePackages("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher Matcher for classes
            matcher {
                // FieldsMatcher Matcher for fields in a class
                fields {
                    // Add a matcher for the field
                    add {
                        // Specify the modifiers of the field
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                        // Specify the type of the field
                        type = "java.lang.String"
                        // Specify the name of the field
                        name = "TAG"
                    }
                    // Add a matcher for the field of the specified type
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // Specify the number of fields in the class
                    count = 3
                }
                // MethodsMatcher Matcher for methods in a class
                methods {
                    // Add a matcher for the method
                    add {
                        // Specify the modifiers of the method
                        modifiers = Modifier.PROTECTED
                        // Specify the name of the method
                        name = "onCreate"
                        // Specify the return type of the method
                        returnType = "void"
                        // Specify the parameter types of the method, if the parameter types are uncertain,
                        // use null, and this method will implicitly declare the number of parameters
                        paramTypes("android.os.Bundle")
                        // Specify the strings used in the method
                        usingStrings("onCreate")
                    }
                    add {
                        paramTypes("android.view.View")
                        // Specify the numbers used in the method, the type is Byte, Short, Int, Long, Float, Double
                        usingNumbers(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        paramTypes("boolean")
                        // Specify the methods called in the method list
                        invokeMethods {
                            add {
                                modifiers = Modifier.PUBLIC or Modifier.STATIC
                                returnType = "int"
                                // Specify the strings used in the method called in the method,
                                usingStrings(listOf("getRandomDice: "), StringMatchType.Equals)
                            }
                            // Only need to contain the call to the above method
                            matchType = MatchType.Contains
                        }
                    }
                    count(1..10)
                }
                // AnnotationsMatcher Matcher for annotations in a class
                annotations {
                    // Add a matcher for the annotation
                    add {
                        // Specify the type of the annotation
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        // The annotation needs to contain the specified element
                        addElement {
                            // Specify the name of the element
                            name = "path"
                            // Specify the value of the element
                            stringValue("/play")
                        }
                    }
                }
                // Strings used by all methods in the class
                usingStrings("PlayActivity", "onClick", "onCreate")
            }
        }.singleOrNull() ?: error("The returned result is not unique")
        // Print the found class: org.luckypray.dexkit.demo.PlayActivity
        println(classData.name)
        // Get the corresponding class instance
        val clazz = classData.getInstance(loadPackageParam.classLoader)
    }
}
```

</p></details>

## Third-Party Open Source References

- [slicer](https://cs.android.com/android/platform/superproject/+/main:tools/dexter/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=luckypray/dexkit&type=Date)](https://star-history.com/#luckypray/dexkit&Date)

## License

Except for the `Core/` directory, this project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).  
The code inside the `Core/` directory is licensed under the [GNU Lesser General Public License v3 (LGPL-3.0)](https://www.gnu.org/licenses/lgpl-3.0.html).

For details, please refer to:
- The `LICENSE` file in the project root (Apache 2.0 full text or summary), which applies to everything except `Core/`.
- The `LICENSE` file within `Core/` (LGPL-3.0 full text or summary), which applies only to the files under `Core/`.

Copyright © LuckyPray
