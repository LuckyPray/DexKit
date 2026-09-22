# DexKit FlatBuffer schema

## Generate Kotlin and C++ together

Use `flatc` **23.5.26**, matching the native and JVM runtimes. Download the executable for your
platform from the [official release](https://github.com/google/flatbuffers/releases/tag/v23.5.26)
and place it in this directory as `flatc` (`flatc.exe` on Windows). The local binary is gitignored.
Alternatively, set `FLATC` to its path or install the same version on `PATH`.

From the repository root:

```shell
python3 schema/gen_code.py
```

The script verifies the compiler version and regenerates both languages, including Kotlin
package/type aliases. Do not hand-edit the generated files.

## Access flag fields

The Kotlin and native schema code are generated and released together. Matchers and result
metadata use the same adjacent fields: `modifiers` for Android Java reflection semantics and
`access_flags` (`accessFlags` in Kotlin) for raw DEX flags.

## kotlin

```shell
./flatc --kotlin -o ../dexkit/src/main/java/org/luckypray \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```

## c++

```shell
./flatc -c --cpp-std c++17 --scoped-enums --no-emit-min-max-enum-values -o ../Core/dexkit/include/schema \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```
