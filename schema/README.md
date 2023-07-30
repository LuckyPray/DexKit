# DexKit FlatBuffer schema

## kotlin

```shell
./flatc --kotlin --gen-mutable -o gen/kotlin fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```

## c++

```shell
./flatc -c --cpp-std c++17 -o gen/cpp fbs/encode_value.fbs fbs/enums.fbs fbs/matchers.fbs fbs/querys.fbs fbs/ranges.fbs fbs/results.fbs
```