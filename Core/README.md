# Upstream sync notes

## slicer

Upstream: AOSP `platform/tools/dexter`, directory `slicer/`.
Last imported revision: `93caac6` (DexKit commit `0297423`).

When updating `third_party/slicer/`, retain these local changes or verify that
the new upstream revision provides equivalent behavior:

| Maintenance point | Changes to preserve | Reference |
| --- | --- | --- |
| Binary size | Keep `printf`-style diagnostics, variadic `SLICER_FATAL`, direct string assembly, and disabled stream operators across `common`, bytecode, encoder, and IR helpers. Avoid restoring `iostream`/`stringstream` dependencies. | `7fb6739` |
| Worker cache lifetime | In `common.cc`, keep the weak-failure cache registered with `dexkit/include/matcher_thread_cache_registry.h`, released on its owning worker before exit, and its TLS pointer reset during cleanup. Keep this coordinated with `third_party/thread_helper/ThreadPool.h`. | `5226b73`, `1d936bd` |
| Reader bounds | In `export/slicer/reader.h` and `reader.cc`, keep `size_t` offsets, overflow-safe object/array bounds, DEX 041 data-offset rules, container-offset validation, and full map-array bounds. | `34ca4b8` |
| Parameter annotations | In `Reader::ExtractAnnotationSetRefList()`, retain empty annotation slots so parameter positions remain correct, and validate the full reference array. | `34ca4b8` |
| Register decoding | In `code_ir.cc`, retain the first `vC` register when `vA == 1` (`vA > 0`, not `vA > 1`). | `34ca4b8` |
| Non-empty link data | In `Reader::ValidateHeader()`, keep support for non-empty `link_data`, offset/size consistency checks, container-relative bounds, and subtraction-based length checks. Retain `SLICER_CHECK` failures rather than introducing C++ exceptions. | [#60](https://github.com/LuckyPray/DexKit/issues/60) |

From the repository root, inspect the accumulated local changes with:

```sh
git diff 0297423 -- Core/third_party/slicer
```

After syncing, update the imported revision, comparison commit, and this list.
