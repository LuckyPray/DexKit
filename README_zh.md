DexKit
--

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文文档](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

一个致力于更好用的dex解析(反混淆)工具。

slicer目录下内容是从 [AOSP](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/startop/view_compiler) 拷贝的.

修改部分归 LuckyPray 所有。如果您想在开源项目中使用，请将其模块化。

## API说明

DexKit应该是目前最快的反混淆工具。

自带多线程执行，配上**双数组Trie树AC自动机**带来的字符串匹配优化，单独增加的搜索复杂度可以忽略不计。

在正常情况下使用 **`DexKit::LocationClasses`**、 **`DexKit::LocationMethods`** 这两个方法即可满足日常的反混淆定位需求。

目前支持以下API:
- **`DexKit::LocationClasses`**: 多字符串综合定位类
- **`DexKit::LocationMethods`**: 多字符串综合定位方法
- `DexKit::FindMethodInvoked`: 查找所有调用指定方法的方法(invoke-kind类别的opcode)
- `DexKit::FindMethodUsedString`: 查找调用了指定字符串的方法(`const-string`、`const-string/jumbo`)
- `DexKit::FindMethod`: 多条件查找方法
- `DexKit::FindSubClasses`: 查找直系子类
- `DexKit::FindMethodOpPrefixSeq`: 查找满足特定op前缀序列的方法(使用`0x00`-`0xff`)

## 集成

Gradle:

`implementation: io.github.LuckyPray:DexKit:<version>`

这个库使用了 [prefab](https://google.github.io/prefab/)，你需要在 gradle (Android Gradle Plugin 4.1+ 版本以上才支持)中开启此特性：

```
android {
    buildFeatures {
        prefab true
    }
}
```

## 使用

### CMake

你可以直接在 `CMakeLists.txt` 中使用 `find_package` 来使用 DexKit:

```
# 假设你的 library 名字为 mylib
add_library(mylib SHARED main.cpp)

# 添加如下两行，注意必须添加 libz，如果你有其他依赖可以放在后面
find_package(dexkit REQUIRED CONFIG)
target_link_libraries(app dexkit::dex_kit_static z)
```

## 使用示例

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/qq-example.cpp)

qq-example.cpp 在MacPro M1环境下对 `qq-8.9.3.apk` 执行结果如下所示:
```text
findClass count: 47
findMethod count: 29
used time: 207 ms
```
