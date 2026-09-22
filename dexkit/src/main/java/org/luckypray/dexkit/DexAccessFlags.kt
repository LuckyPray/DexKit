/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
@file:Suppress("unused")

package org.luckypray.dexkit

/**
 * Constants and helpers for raw DEX `access_flags` values.
 *
 * Use these with the `accessFlags` result property or matcher condition for exact DEX semantics.
 * The separate `modifiers` property and condition follow Android Java reflection, including
 * hidden Java bits such as [BRIDGE], [VARARGS], and [SYNTHETIC]. Use
 * [java.lang.reflect.Modifier] for reflection conditions.
 *
 * [CONSTRUCTOR] and [DECLARED_SYNCHRONIZED] are DEX-only bits: inspect them through `accessFlags`.
 * The helpers only test bits and never normalize them. In raw DEX, [SYNCHRONIZED] is only valid
 * for native methods; reflection maps [DECLARED_SYNCHRONIZED] to [SYNCHRONIZED] instead.
 *
 * Several bit positions depend on the target: [SYNCHRONIZED] and [SUPER] are both `0x20`,
 * [VOLATILE] and [BRIDGE] are both `0x40`, and [TRANSIENT] and [VARARGS] are both `0x80`.
 * [SUPER] mirrors slicer's complete `kAcc` set but is not used by a DEX `class_def_item`.
 * See [DEX access_flags](https://source.android.com/docs/core/runtime/dex-format#access-flags).
 *
 * Java can access constants and helpers directly, for example:
 *
 *     DexAccessFlags.isConstructor(methodData.getAccessFlags())
 *     DexAccessFlags.isBridge(methodData.getAccessFlags())
 *
 * ----------------
 * 原始 DEX `access_flags` 常量及辅助方法。
 *
 * 精确 DEX 语义请使用结果属性或匹配条件 `accessFlags`；独立的 `modifiers` 属性和条件遵循
 * Android Java 反射语义，保留 [BRIDGE]、[VARARGS]、[SYNTHETIC] 等隐藏 Java 标志位。
 * 反射条件使用 [java.lang.reflect.Modifier]。
 *
 * [CONSTRUCTOR]、[DECLARED_SYNCHRONIZED] 是 DEX 专有标志，应通过 `accessFlags` 读取。
 * 本工具类只判断位值，不做归一化。原始 DEX 的 [SYNCHRONIZED] 仅适用于 native 方法；
 * 反射语义则把 [DECLARED_SYNCHRONIZED] 转换为 [SYNCHRONIZED]。
 *
 * 部分位值按目标类型复用：[SYNCHRONIZED] 与 [SUPER] 均为 `0x20`，[VOLATILE] 与 [BRIDGE]
 * 均为 `0x40`，[TRANSIENT] 与 [VARARGS] 均为 `0x80`。请选择适用于类、字段或方法的常量。
 * [SUPER] 仅用于对齐 slicer 的完整 `kAcc` 集合，不用于 DEX `class_def_item`。
 */
object DexAccessFlags {

    /** `ACC_PUBLIC`: class, field, method, or `InnerClass`. */
    const val PUBLIC = 0x0001

    /** `ACC_PRIVATE`: field, method, or `InnerClass`. */
    const val PRIVATE = 0x0002

    /** `ACC_PROTECTED`: field, method, or `InnerClass`. */
    const val PROTECTED = 0x0004

    /** `ACC_STATIC`: field, method, or `InnerClass`. */
    const val STATIC = 0x0008

    /** `ACC_FINAL`: class, field, method, or `InnerClass`. */
    const val FINAL = 0x0010

    /** `ACC_SYNCHRONIZED`: synchronized native method. */
    const val SYNCHRONIZED = 0x0020

    /** `ACC_SUPER`: class-file flag; not used by a DEX `class_def_item`. */
    const val SUPER = 0x0020

    /** `ACC_VOLATILE`: volatile field. */
    const val VOLATILE = 0x0040

    /** `ACC_BRIDGE`: compiler-generated bridge method. */
    const val BRIDGE = 0x0040

    /** `ACC_TRANSIENT`: transient field. */
    const val TRANSIENT = 0x0080

    /** `ACC_VARARGS`: variable-arity method. */
    const val VARARGS = 0x0080

    /** `ACC_NATIVE`: native method. */
    const val NATIVE = 0x0100

    /** `ACC_INTERFACE`: interface class or `InnerClass`. */
    const val INTERFACE = 0x0200

    /** `ACC_ABSTRACT`: abstract class, method, or `InnerClass`. */
    const val ABSTRACT = 0x0400

    /** `ACC_STRICT`: strict floating-point method. */
    const val STRICT = 0x0800

    /** `ACC_SYNTHETIC`: compiler-generated class, field, method, or `InnerClass`. */
    const val SYNTHETIC = 0x1000

    /** `ACC_ANNOTATION`: annotation class or `InnerClass`. */
    const val ANNOTATION = 0x2000

    /** `ACC_ENUM`: enum class, enum field, or `InnerClass`. */
    const val ENUM = 0x4000

    /** `ACC_CONSTRUCTOR`: DEX-only class or instance initializer method. */
    const val CONSTRUCTOR = 0x00010000

    /** `ACC_DECLARED_SYNCHRONIZED`: DEX-only declared synchronized method. */
    const val DECLARED_SYNCHRONIZED = 0x00020000

    /** Returns whether [accessFlags] contains [PUBLIC]. */
    @JvmStatic
    fun isPublic(accessFlags: Int) = hasFlag(accessFlags, PUBLIC)

    /** Returns whether [accessFlags] contains [PRIVATE]. */
    @JvmStatic
    fun isPrivate(accessFlags: Int) = hasFlag(accessFlags, PRIVATE)

    /** Returns whether [accessFlags] contains [PROTECTED]. */
    @JvmStatic
    fun isProtected(accessFlags: Int) = hasFlag(accessFlags, PROTECTED)

    /** Returns whether [accessFlags] contains [STATIC]. */
    @JvmStatic
    fun isStatic(accessFlags: Int) = hasFlag(accessFlags, STATIC)

    /** Returns whether [accessFlags] contains [FINAL]. */
    @JvmStatic
    fun isFinal(accessFlags: Int) = hasFlag(accessFlags, FINAL)

    /** Returns whether [accessFlags] contains [SYNCHRONIZED]. */
    @JvmStatic
    fun isSynchronized(accessFlags: Int) = hasFlag(accessFlags, SYNCHRONIZED)

    /** Returns whether [accessFlags] contains [SUPER]. */
    @JvmStatic
    fun isSuper(accessFlags: Int) = hasFlag(accessFlags, SUPER)

    /** Returns whether [accessFlags] contains [VOLATILE]. */
    @JvmStatic
    fun isVolatile(accessFlags: Int) = hasFlag(accessFlags, VOLATILE)

    /** Returns whether [accessFlags] contains [BRIDGE]. */
    @JvmStatic
    fun isBridge(accessFlags: Int) = hasFlag(accessFlags, BRIDGE)

    /** Returns whether [accessFlags] contains [TRANSIENT]. */
    @JvmStatic
    fun isTransient(accessFlags: Int) = hasFlag(accessFlags, TRANSIENT)

    /** Returns whether [accessFlags] contains [VARARGS]. */
    @JvmStatic
    fun isVarArgs(accessFlags: Int) = hasFlag(accessFlags, VARARGS)

    /** Returns whether [accessFlags] contains [NATIVE]. */
    @JvmStatic
    fun isNative(accessFlags: Int) = hasFlag(accessFlags, NATIVE)

    /** Returns whether [accessFlags] contains [INTERFACE]. */
    @JvmStatic
    fun isInterface(accessFlags: Int) = hasFlag(accessFlags, INTERFACE)

    /** Returns whether [accessFlags] contains [ABSTRACT]. */
    @JvmStatic
    fun isAbstract(accessFlags: Int) = hasFlag(accessFlags, ABSTRACT)

    /** Returns whether [accessFlags] contains [STRICT]. */
    @JvmStatic
    fun isStrict(accessFlags: Int) = hasFlag(accessFlags, STRICT)

    /** Returns whether [accessFlags] contains [SYNTHETIC]. */
    @JvmStatic
    fun isSynthetic(accessFlags: Int) = hasFlag(accessFlags, SYNTHETIC)

    /** Returns whether [accessFlags] contains [ANNOTATION]. */
    @JvmStatic
    fun isAnnotation(accessFlags: Int) = hasFlag(accessFlags, ANNOTATION)

    /** Returns whether [accessFlags] contains [ENUM]. */
    @JvmStatic
    fun isEnum(accessFlags: Int) = hasFlag(accessFlags, ENUM)

    /** Returns whether [accessFlags] contains [CONSTRUCTOR]. */
    @JvmStatic
    fun isConstructor(accessFlags: Int) = hasFlag(accessFlags, CONSTRUCTOR)

    /** Returns whether [accessFlags] contains [DECLARED_SYNCHRONIZED]. */
    @JvmStatic
    fun isDeclaredSynchronized(accessFlags: Int) = hasFlag(accessFlags, DECLARED_SYNCHRONIZED)

    /** Returns a context-aware string for class access flags. */
    @JvmStatic
    fun toClassString(accessFlags: Int) = stringify(
        accessFlags,
        PUBLIC to "public",
        PROTECTED to "protected",
        PRIVATE to "private",
        ABSTRACT to "abstract",
        STATIC to "static",
        FINAL to "final",
        SUPER to "super",
        INTERFACE to "interface",
        SYNTHETIC to "synthetic",
        ANNOTATION to "annotation",
        ENUM to "enum"
    )

    /** Returns a context-aware string for field access flags. */
    @JvmStatic
    fun toFieldString(accessFlags: Int) = stringify(
        accessFlags,
        PUBLIC to "public",
        PROTECTED to "protected",
        PRIVATE to "private",
        STATIC to "static",
        FINAL to "final",
        TRANSIENT to "transient",
        VOLATILE to "volatile",
        SYNTHETIC to "synthetic",
        ENUM to "enum"
    )

    /** Returns a context-aware string for method access flags. */
    @JvmStatic
    fun toMethodString(accessFlags: Int) = stringify(
        accessFlags,
        PUBLIC to "public",
        PROTECTED to "protected",
        PRIVATE to "private",
        ABSTRACT to "abstract",
        STATIC to "static",
        FINAL to "final",
        SYNCHRONIZED to "synchronized",
        BRIDGE to "bridge",
        VARARGS to "varargs",
        NATIVE to "native",
        STRICT to "strictfp",
        SYNTHETIC to "synthetic",
        CONSTRUCTOR to "constructor",
        DECLARED_SYNCHRONIZED to "declared-synchronized"
    )

    private fun hasFlag(accessFlags: Int, flag: Int) = accessFlags and flag != 0

    private fun stringify(accessFlags: Int, vararg names: Pair<Int, String>): String {
        return names.asSequence()
            .filter { (flag, _) -> hasFlag(accessFlags, flag) }
            .joinToString(" ") { (_, name) -> name }
    }
}
