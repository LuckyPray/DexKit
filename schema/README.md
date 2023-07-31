# DexKit FlatBuffer schema

## python generate kotlin && c++ code

```shell
python gen_code.py
```

## kotlin

```shell
./flatc --kotlin -o ../dexkit/src/main/java/org/luckypray \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```

macos change package name
```shell
sed -i '' 's/package dexkit.schema/package org.luckypray.dexkit.schema/g' ../dexkit/src/main/java/org/luckypray/dexkit/schema/*.kt
sed -i '' 's/dexkit.schema.//g' ../dexkit/src/main/java/org/luckypray/dexkit/schema/*.kt
```

## c++

```shell
./flatc -c --cpp-std c++17 --scoped-enums --no-emit-min-max-enum-values -o ../Core/dexkit/schema \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```
