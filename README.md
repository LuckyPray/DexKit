 DexKit
--

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文文档](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

A high performance dex deobfuscator library.

> **Warning**: The current project has been refactored, and all previous APIs have been deprecated. Please refer to the latest documentation for use.

## API introduction

These two APIs can meet most of your usage scenarios:

- **`DexKit::BatchFindClassesUsingStrings`**
- **`DexKit::BatchFindMethodsUsingStrings`**

> **Note**: In all cases you should avoid searching for keywords that contain duplicate content, eg: {"key_word", "word"}, as this will cause tags to be overwritten, resulting in inaccurate search results.
> If there is such a need, open the advanced search mode as much as possible, and use the string to match the content exactly, for example, modify it to this: {"^key_word$", "^word$"}

And there are many other APIs:

- `DexKit::FindMethodCaller`: find caller for specified method.
- `DexKit::FindMethodInvoking`: find the called method
- `DexKit::FindMethodUsingField`: Find method to get/set specified field
- `DexKit::FindMethodUsingString`: find method used utf8 string
- `DexKit::FindMethod`: find method by multiple conditions
- `DexKit::FindSubClasses`: find all direct subclasses of the specified class
- `DexKit::FindMethodOpPrefixSeq`:  find all method using opcode prefix sequence(op range: `0x00`-`0xFF`)
- `DexKit::FindMethodUsingOpCodeSeq`: find all method using opcode sequence(op range: `0x00`-`0xFF`)
- `DexKit::GetMethodOpCodeSeq`: get method opcode sequence(op range: `0x00`-`0xFF`)

For more detailed instructions, please refer to [dex_kit.h](https://github.com/LuckyPray/DexKit/blob/master/Core/include/dex_kit.h).

## Example

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/Core/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/Core/qq-example.cpp)

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

Please see [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/main.cpp) for usage examples.
