# Run on desktop platform

> Starting from version `1.1.0`, DexKit supports running on desktop platforms without 
> the need for packaging as an APK for testing on Android.

## Install environment

The basic runtime environment requires gcc/clang, cmake, and ninja/make.

### Windows

`Windows` users can use [MSYS2](https://www.msys2.org/) to set up the runtime environment.
Since all Windows systems are currently 64-bit, we use `mingw64.exe` for dependency installation:

```shell
pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-cmake mingw-w64-x86_64-ninja
```

After installation, we need to add the `mingw64/bin` directory to the environment variables 
for future use.

::: warning warning
DexKit will use `ninja` as the default build system by default. If you need to use `make` 
in mingw for building, you need to execute `pacman -S mingw-w64-x86_64-make`, after installation,
you need to rename `msys64\mingw64\bin\mingw32-make.exe` to `make.exe` or add it as a shortcut, 
otherwise the build will fail due to `gradle-cmake-plugin` not finding the make command. 
At the same time, delete `generator.set(generators.ninja)` in `:dexkit/build.gradle`, 
or modify it to `generator.set(generators.unixMakefiles)`.
:::

### Linux

For Linux users, normally only need to install `ninja` to use it.

### MacOS

It's recommended to use [HomeBrew](https://brew.sh/) for dependency management.

```shell
brew install cmake ninja
```

## Clone DexKit

```shell
git clone https://github.com/LuckyPray/DexKit.git
```

## To begin using

Execute the submodule `:main` to perform testing.

```shell
gradle :main:run
```
