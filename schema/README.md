# DexKit FlatBuffer schema

## python generate

```shell
python gen_code.py
```

## kotlin

```shell
./flatc --kotlin -o ../dexkit/src/main/java/org/luckypray/dexkit \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```

macos change package name
```shell
sed -i '' 's/package schema/package org.luckypray.dexkit.schema/g' ../dexkit/src/main/java/org/luckypray/dexkit/schema/*.kt
sed -i '' 's/schema.//g' ../dexkit/src/main/java/org/luckypray/dexkit/schema/*.kt
```

## c++

```shell
./flatc -c --cpp-std c++17 -o ../Core/dexkit/schema \
fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```