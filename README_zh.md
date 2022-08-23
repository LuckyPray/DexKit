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
- **`DexKit::LocationClasses`**: 多字符串综和定位类
- **`DexKit::LocationMethods`**: 多字符串综和定位方法
- `DexKit::FindMethodInvoked`: 查找所有调用指定方法的方法(invoke-kind类别的opcode)
- `DexKit::FindMethodUsedString`: 查找调用了指定字符串的方法(`const-string`、`const-string/jumbo`)
- `DexKit::FindMethod`: 多条件查找方法
- `DexKit::FindSubClasses`: 查找直系子类
- `DexKit::FindMethodOpPrefixSeq`: 查找满足特定op前缀序列的方法(使用`0x00`-`0xff`)


## 使用示例

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/qq-example.cpp)

qq-example.cpp 在MacPro M1环境下对 `qq-8.9.3.apk` 执行结果如下所示:
```text
findClass count: 47
findMethod count: 29
used time: 207 ms
```
