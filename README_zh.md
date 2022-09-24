DexKit
--

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文文档](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

一个高性能的 dex 反混淆工具。

> **Warning**: 当前项目已经进行重构，以往的API全被弃用，请参考最新的文档进行使用。

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

更详细的API说明请参考 [dex_kit.h](https://github.com/LuckyPray/DexKit/blob/master/Core/include/dex_kit.h).

## 使用示例

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

slicer目录下内容是从 [AOSP](https://cs.android.com/android/platform/superproject/+/master:frameworks/base/startop/view_compiler) 拷贝的.

修改部分归 LuckyPray 所有。如果您想在开源项目中使用，请将其子模块化。

