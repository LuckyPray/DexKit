<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![](https://jitpack.io/v/LuckyPray/DexKit.svg)](https://jitpack.io/#LuckyPray/DexKit)

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文说明](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

</div>

一个简单易用、高性能的dex反混淆库。轻松接入你的 CMAKE/Android 项目。

## API说明

这两个 API 可以满足你大部分的使用场景：

- **`DexKit::BatchFindClassesUsingStrings`**
- **`DexKit::BatchFindMethodsUsingStrings`**

> **Note**：无论什么情况都应当避免搜索关键词包含重复内容， 例如：{"key_word", "word"}，因为这样会导致标记被覆盖，从而导致搜索结果不准确。
> 如果真的有这样的需求，尽可能打开高级搜索模式，同时使用字符串完全匹配内容，例如修改成这样：{"^key_word$", "^word$"}

以及其他 API：

- `DexKit::FindMethodCaller`: 查找指定方法的调用者
- `DexKit::FindMethodInvoking`: 查找指定方法调用的方法
- `DexKit::FindMethodUsingField`: 查找获取/设置指定字段的方法
- `DexKit::FindMethodUsingString`: 查找指定字符串的调用者
- `DexKit::FindMethod`: 多条件查找方法
- `DexKit::FindSubClasses`: 查找直系子类
- `DexKit::FindMethodOpPrefixSeq`: 查找满足特定op前缀序列的方法(op范围: `0x00`-`0xFF`)
- `DexKit::FindMethodUsingOpCodeSeq`: 查找使用了特定op序列的方法(op范围: `0x00`-`0xFF`)
- `DexKit::GetMethodOpCodeSeq`: 获取方法op序列(op范围: `0x00`-`0xFF`)

目前更详细的API说明请参考 [dex_kit.h](https://github.com/LuckyPray/DexKit/blob/master/Core/include/dex_kit.h).

## 快速上手

### 方式一：直接引入（推荐）

但是该方式会额外引入一个so文件，如果你有洁癖需要all in one的话，可以使用方式二或者三。

${project}/build.gradle:
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

${project}/app/build.gradle:
```groovy
dependencies {
    implementation 'com.github.LuckyPray:DexKit:<version>'
}
```

java:
```java 
import io.luckypry.dexkit.DexKitBridge;
// ...

public class DexUtil {

    static {
        System.loadLibrary("dexkit");
    }

    public static void findMethod() {
        // try-with-resources, 结束时自动调用 DexKitBridge.close() 释放资源
        // 如果你不想使用 try-with-resources，请务必手动调用 DexKitBridge.close() 释放jni占用的内存
        try (DexKitBridge dexKitBridge = DexKitBridge.create(hostClassLoader)) {
            if (dexKitBridge == null) {
                Log.e("DexUtil", "DexKitBridge create failed");
                return;
            }
            List<DexClassDescriptor> classes = dexKitBridge.findSubClasses("android.app.Activity", null);
            for (DexClassDescriptor clazz : classes) {
                String name = clazz.getName();
                String simpleName = clazz.getSimpleName();
                Class<?> clz = clazz.getClassInstance(hostClassLoader);
                Log.i("DexUtil", "findSubClasses: " + clz);
            }
        } catch (Throwable e) {
            Log.e("DexUtil", Log.getStackTraceString(e));
        }
    }
}
```

### 方式二：google prefab
${project}/app/build.gradle

```groovy
android {
    buildFeatures {
        prefab true
    }
}
```

> **Note**：DexKit-Android 使用 [prefab package schema v2](https://github.com/google/prefab/releases/tag/v2.0.0)，
它是从 [Android Gradle Plugin 7.1.0](https://developer.android.com/studio/releases/gradle-plugin?buildsystem=cmake#7-1-0) 开始作为默认配置的。
如果你使用的是 Android Gradle Plugin 7.1.0 之前的版本，请在 `gradle.properties` 中加入以下配置：

```
android.prefabVersion=2.0.0
```

同时为了避免`libdexkit.so`被添加到apk中，你可以在`app/build.gradle`添加以下配置：

```groovy
android {
    packagingOptions {
        jniLibs.excludes.add("lib/**/libdexkit.so")
    }
}
```

CMake:

你可以直接在 `CMakeLists.txt` 中使用 `find_package` 来使用 DexKit:
```cmake
add_library(my_lib SHARED native.cpp)

# 添加如下三行，注意必须添加 libz！！如果你有其他依赖可以放在后面
find_package(dexkit REQUIRED CONFIG)
find_library(log-lib log)
target_link_libraries(my_lib dexkit::dex_kit_static z ${log-lib})
```

同时，我们提供了头文件 [dex_kit_jni_helper.h](https://github.com/LuckyPray/DexKit/blob/master/Core/include/dex_kit_jni_helper.h)
便捷转换java/c++数据对象的互转：
```c++
#include <jni.h>
#include <dex_kit.h>
#include "dex_kit_jni_helper.h"

#define DEXKIT_JNI extern "C" JNIEXPORT JNICALL

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingString(JNIEnv *env, jclass clazz,
                                                                  jlong native_ptr,
                                                                  jstring used_string,
                                                                  jboolean advanced_match,
                                                                  jstring method_declare_class,
                                                                  jstring method_name,
                                                                  jstring method_return_type,
                                                                  jobjectArray method_param_types,
                                                                  jintArray dex_priority) {
    return FindMethodUsingString(env, native_ptr, used_string, advanced_match, method_declare_class,
                                 method_name, method_return_type, method_param_types, dex_priority);
}
```

### 方式三：使用Git子模块

较为复杂，参考：https://github.com/LuckyPray/XAutoDaily/tree/master/dexkit


## c++使用示例

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/Core/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/Core/qq-example.cpp)

## 基准测试

qq-example.cpp 在MacPro M1环境下对 `qq-8.9.3.apk` 执行结果如下所示:

```text
findClass count: 47
findMethod count: 29
used time: 207 ms
```

## License

slicer目录下内容是从 [AOSP](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/startop/view_compiler)
拷贝的.

修改部分归 LuckyPray 所有。如果您想在开源项目中使用，请将其子模块化。

