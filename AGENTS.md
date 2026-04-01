# DexKit Agent Guide

## Agent Quick Start

- For significant features, refactors, or cross-module fixes, sketch a short plan first and keep it updated while you work.
- Use `rg` for search, prefer small focused edits, and keep files ASCII unless the target file already uses non-ASCII text.
- Treat `Core/`, `schema/`, `dexkit/`, and `dexkit-android/` as one pipeline: API or schema changes usually require updates in multiple places.
- Run the component-specific checks below before handing work off; do not silently skip failing steps.
- Do not edit generated or local-only outputs unless the task is explicitly about them (`**/build/`, `.cxx/`, `.gradle/`, `doc-source/node_modules/`, `Core/cmake-build-*`, `local.properties`).

## Project Overview

DexKit is a high-performance dex parsing and search library with a shared C++ core, a desktop/JVM package, an Android AAR, a demo APK used as a test fixture, custom Android lint rules, and a VuePress documentation site.

## Repository Structure

```bash
/Core/                        # Shared C++ parsing/search engine and generated C++ schema headers
/dexkit/                      # JVM/desktop library, JNI bridge, query DSL, results, and host-side tests
/dexkit-android/              # Android AAR wrapping the same sources with NDK/CMake packaging
/demo/                        # Android demo app; release APK is reused by dexkit tests
/main/                        # Desktop sample app for quick manual verification
/lint-rules/                  # Android lint checks shipped with the library
/schema/                      # FlatBuffers schema sources and code generation script
/doc-source/                  # VuePress documentation source
/.github/workflows/           # CI workflows for native builds and docs deployment
```

## Core Concepts

- **Core engine**: `Core/dexkit/*.cpp` and `Core/dexkit/include/*` implement the shared parser/search engine. It is built as `dexkit_static` and linked into both desktop and Android targets, so native changes must be validated on both sides.
- **DexKitBridge**: `dexkit/src/main/java/org/luckypray/dexkit/DexKitBridge.kt` is the main entry point. It owns the native token, drives all query execution, and must preserve explicit lifecycle semantics (`create(...)` / `close()` / `use { ... }`).
- **Query/schema pipeline**: classes under `dexkit/.../query/` serialize requests into FlatBuffers defined in `schema/fbs/*.fbs`; generated Kotlin schema classes live in `dexkit/.../schema/`, and generated C++ headers live in `Core/dexkit/include/schema/`.
- **DexKitCacheBridge**: `dexkit/src/main/java/org/luckypray/dexkit/DexKitCacheBridge.kt` adds reusable bridge pooling and caching. Be careful with lifecycle, concurrency, and weak/strong pool behavior when modifying it.
- **Demo APK fixture**: `demo/` builds the APK that Gradle copies to `dexkit/apk/demo.apk`. Host-side integration tests and README example tests depend on that artifact.
- **README-backed tests**: `dexkit/src/test/java/org/luckypray/dexkit/JavaReadMeTest.java` and `KtReadMeTest.kt` mirror public examples. If API examples change, update these tests in the same change.
- **Lint rules**: `lint-rules/` encodes library usage guidance such as avoiding repeated `DexKitBridge.create()` calls and discouraging non-unique result APIs.

## Component Workflows

Use the repo Gradle wrapper (`./gradlew`, or `gradlew.bat` on Windows).

### Shared schema (`schema/`)

If you change any FlatBuffers definitions under `schema/fbs/`, regenerate both Kotlin and C++ outputs:

```bash
python schema/gen_code.py
```

Do not update only one generated side; query/schema/native changes must stay in lockstep.

### Core + JVM library (`Core/`, `dexkit/`)

For changes in native code, JNI, query DSL, result objects, reflection wrappers, or desktop behavior, run:

```bash
./gradlew :dexkit:cmakeBuild
./gradlew :dexkit:jar
./gradlew :dexkit:test
```

Notes:

- `:dexkit:test` depends on `:demo:assembleRelease` and copies the demo APK plus the native library before running tests.
- Even host-side tests need a working Android SDK/NDK setup because the demo app is part of the test fixture pipeline.

### Android AAR (`dexkit-android/`, `lint-rules/`)

For Android packaging or public Android API changes, run:

```bash
./gradlew :dexkit-android:assembleRelease
```

If you touched lint checks, also run:

```bash
./gradlew :lint-rules:jar
```

### Demo app (`demo/`)

When you need to verify the APK fixture directly, run:

```bash
./gradlew :demo:assembleRelease
```

### Desktop sample (`main/`)

For a quick host-side smoke test after native or bridge changes, run:

```bash
./gradlew :main:run
```

### Documentation (`README*.md`, `doc-source/`)

- If public APIs, examples, or behavior change, update `README.md`, `README_zh.md`, and the relevant docs under `doc-source/src/en/` and `doc-source/src/zh-cn/`.
- Keep README examples aligned with `JavaReadMeTest` and `KtReadMeTest`.
- Validate the docs site with:

```bash
cd doc-source
yarn install
yarn docs:build
```

## Common Pitfalls

- `DexKitBridge.create(...)` is expensive; do not introduce repeated creation in hot paths unless the change explicitly requires it.
- Query matcher, enum, or result changes often require synchronized edits across `query/`, generated `schema/`, native decoding, docs, and tests.
- A fix verified only on desktop may still break the Android AAR, and vice versa, because both package the same native core through different build paths.
- Do not hand-edit generated schema files when the source of truth is under `schema/fbs/`.
- Avoid committing local or generated artifacts from IDE, Gradle, CMake, NDK, or `node_modules`.

## Git Commit

- Mirror recent history styles such as `feat(scope): ...`, `fix(scope): ...`, `refactor(scope): ...`, or `[Core] ...` for native-core-only changes.
- Use a short, specific scope when it helps (`CacheBridge`, `InstanceUtil`, `mingw`, `docs`, `dexkit-android`).
- Keep the subject concise and check recent history first with `git log --oneline -n 10` so your prefix and capitalization match the current repo style.
