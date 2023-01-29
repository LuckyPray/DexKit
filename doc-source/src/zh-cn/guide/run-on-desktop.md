# 在桌面平台运行

> 从 `1.1.0` 开始，`DexKit` 支持桌面平台运行，无需打包成 apk 在 Android 进行测试工作。

## 安装环境

需要 gcc/clang、cmake 以及 ninja/make 作为基础运行环境。 

### Windows

`Windows` 用户可以使用 [MSYS2](https://www.msys2.org/) 搭建运行环境。由于目前Windows系统均为64位，
所以我们使用 `mingw64.exe` 进行依赖安装：

```shell
pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-cmake mingw-w64-x86_64-ninja
```

安装完成后，我们需要将 `mingw64/bin` 目录添加进环境变量中便于后续使用。

::: warning warning
DexKit 默认将使用 `ninja` 作为默认构建系统，如果你需要在 mingw 中使用 `make` 进行构建， 需要执行 
`pacman -S mingw-w64-x86_64-make`，安装完成后需要将 `msys64\mingw64\bin\mingw32-make.exe`
重命名为 `make.exe` 或者添加为快捷方式，否则会由于 `gradle-cmake-plugin` 找不到 `make` 命令而构建失败。
同时删除 `:dexkit/build.gradle` 中的 `generator.set(generators.ninja)` ，或者修改为 
`generator.set(generators.unixMakefiles)`
:::

### Linux

对于Linux用户，正常情况只需要安装 `ninja` 即可。

### MacOS

推荐使用 [HomeBrew](https://brew.sh/) 进行依赖管理，

```shell
brew install cmake ninja
```

## 克隆 DexKit

```shell
git clone https://github.com/LuckyPray/DexKit.git
```

## 开始使用

执行子模块 `:main` 即可进行测试。

```shell
gradle :main:run
```
