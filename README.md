 DexKit
--

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文文档](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

A high performance dex deobfuscator library.

## API introduction

These two APIs can meet most of your usage scenarios:

- **`DexKit::LocationClasses`**
- **`DexKit::LocationMethods`**

And there are many other APIs:

- `DexKit::FindMethodInvoked`: Find caller for specified method.
- `DexKit::FindMethodUsedString`
- `DexKit::FindMethod`: Find method with various conditions
- `DexKit::FindSubClasses`: Find sub class of specified class
- `DexKit::FindMethodOpPrefixSeq`: Find method with op prefix

## Integration

Gradle:

`implementation: io.github.LuckyPray:DexKit:<version>`

This library uses [prefab](https://google.github.io/prefab/), you should enable it in gradle (Android Gradle Plugin 4.1+):

```
android {
    buildFeatures {
        prefab true
    }
}
```

## Usage

### CMake

You can use `find_package` in `CMakeLists.txt`:

```
add_library(mylib SHARED main.cpp)

# Add two lines below
find_package(dexkit REQUIRED CONFIG)
target_link_libraries(app dexkit::dex_kit_static z)
```

## Example

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/qq-example.cpp)

## Benchmark

qq-example.cpp in MacPro M1 to deobfuscate `qq-8.9.3.apk`, the result is:

```txt
findClass count: 47
findMethod count: 29
used time: 207 ms
```

## License

The slicer directory is partially copied from [AOSP](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/startop/view_compiler).

Modified parts are owed by LuckyPray Developers. If you would like to use it in an open source project, please submodule it.