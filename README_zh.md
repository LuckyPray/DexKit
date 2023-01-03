<div align="center">
    <h1> DexKit </h1>

[![license](https://img.shields.io/github/license/LuckyPray/DexKit.svg)](https://www.gnu.org/licenses/lgpl-3.0.html)
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/DexKit.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.luckypray%22%20AND%20a:%22DexKit%22)

[English](https://github.com/LuckyPray/DexKit/blob/master/README.md) | 简体中文

</div>

一个使用 C++ 实现的 dex 高性能运行时解析库，用于查找被混淆的类、方法或者属性。

## 背景

对于 `Xposed` 模块来说，我们总是需要对某些特定的方法进行 `Hook` ，但是由于混淆的存在，我们需要使用一些手段来找到我们需要的方法。
但是对于 `JVM` 语言来说，我们在运行时能获取到的信息是有限的，在以往我们查找被混淆后的方法是遍历 ClassLoader 中所有的类，通过包名、
类中包含的方法数量以及方法签名过滤。这种方式的效率不仅十分低下，且对于包名彻底混淆的情况下，我们基本束手无策。

是否还有其他方法？答案是肯定的，`ProGuard` 混淆规则只会混淆类名、方法名和属性名，但是不会修改代码逻辑，
并且在小版本更新时通常不会出现大规模的代码修改。因此，我们可以通过解析字节码来反向查找我们需要的信息。

目前对于 Dex 文件的解析库有很多，但是基本上都是基于 `dexlib2` 实现的。如果宿主应用程序内有很多 Dex 文件，则搜索时间可能长达数分钟，
这对用户来说是一种不好的体验。因此，`DexKit` 应运而生。它使用 C++ 实现，并使用多线程加速，可以在短时间内完成搜索。它具有非常高的性能，
单次搜索的时间在毫秒级别，且支持多线程并发搜索。就算是拥有着 30+ dex 文件的大型应用，使用 `DexKit` 也能在 100 毫秒左右完成单次搜索。
此外，它还针对字符串搜索场景进行了优化，即使要搜索数以百计的字符串，也只需要在两倍的时间内即可完成。

## 支持的功能

- 批量搜索指定字符串的方法/类
- 查找使用了指定字符串的方法/类
- 方法调用/被调用搜索
- 直系子类搜索
- 方法多条件搜索
- op序列搜索(仅支持标准dex指令)
- 注解搜索（目前仅支持搜索value为字符串的查找）

> **Note:**
> 目前为项目初期阶段，不保证未来API不会发生改动，如果你有什么好的建议或者意见，欢迎提出。

## 开始使用

- [点击这里](https://luckypray.org/DexKit/zh-cn/) 前往文档页面查看更多详细教程

## 合作项目

以下是经过合作并稳定使用 `DexKit` 的项目。

| Repository                                                                | Developer                                                                                 |
|:--------------------------------------------------------------------------|:------------------------------------------------------------------------------------------|
| [XAutoDaily](https://github.com/LuckyPray/XAutoDaily)                     | [韵の祈](https://github.com/teble)                                                           |
| [QAuxiliary](https://github.com/cinit/QAuxiliary)                         | [ACh Sulfate](https://github.com/cinit)                                                   |
| [QTool](https://github.com/Hicores/QTool)                                 | [Hicores](https://github.com/Hicores)                                                     |
| [PPHelper](https://github.com/lliioollcn/PPHelper)                        | [lliiooll](https://github.com/lliioollcn)                                                 |
| [MIUI QOL](https://github.com/chsbuffer/MIUIQOL)                          | [ChsBuffer](https://github.com/chsbuffer)                                                 |
| [TwiFucker](https://github.com/Dr-TSNG/TwiFucker)                         | [Js0n](https://github.com/JasonKhew96)、[Nullptr](https://github.com/Dr-TSNG)              |
| [FuckCoolApk R](https://github.com/Xposed-Modules-Repo/org.hello.coolapk) | [QQ little ice](https://github.com/qqlittleice233)、[lz差不多是条咸鱼了](https://github.com/lz233) |

你也在使用 `DexKit` 吗？欢迎 **PR** 将你的仓库添加到上方的列表(私有仓库不需要注明网页链接)。

## 第三方开源引用

- [slicer](https://cs.android.com/android/platform/superproject/+/master:tools/dexter/slicer/export/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [BiliRoaming](https://github.com/yujincheng08/BiliRoaming)

## 许可证

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
