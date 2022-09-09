DexKit
--

[README](https://github.com/LuckyPray/DexKit/blob/master/README.md)|[中文文档](https://github.com/LuckyPray/DexKit/blob/master/README_zh.md)

一个高性能的 dex 反混淆工具。

## API说明

这两个 API 可以满足你大部分的使用场景：

- **`DexKit::LocationClasses`**
- **`DexKit::LocationMethods`**

以及其他 API：

- `DexKit::FindMethodInvoked`: 查找指定方法的调用者(invoke-kind类别的opcode)
- `DexKit::FindMethodUsedString`: 查找指定字符串的调用者(`const-string`、`const-string/jumbo`)
- `DexKit::FindMethod`: 多条件查找方法
- `DexKit::FindSubClasses`: 查找直系子类
- `DexKit::FindMethodOpPrefixSeq`: 查找满足特定op前缀序列的方法(使用`0x00`-`0xff`)

## 使用示例

- [main.cpp](https://github.com/LuckyPray/DexKit/blob/master/main.cpp)
- [qq-example.cpp](https://github.com/LuckyPray/DexKit/blob/master/qq-example.cpp)

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

