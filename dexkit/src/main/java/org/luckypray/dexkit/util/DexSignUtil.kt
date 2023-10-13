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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.util

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object DexSignUtil {

    private val primitiveMap: Map<String, String> = mutableMapOf(
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "short" to "S",
        "int" to "I",
        "float" to "F",
        "long" to "J",
        "double" to "D",
        "void" to "V"
    )

    private val primitiveTypeNameMap: Map<String, String> = mutableMapOf(
        "Z" to "boolean",
        "B" to "byte",
        "C" to "char",
        "S" to "short",
        "I" to "int",
        "F" to "float",
        "J" to "long",
        "D" to "double",
        "V" to "void"
    )

    @JvmStatic
    private fun primitiveTypeName(typeSign: String): String {
        return primitiveTypeNameMap[typeSign]
            ?: throw IllegalArgumentException("Unknown primitive typeSign: $typeSign")
    }

    /**
     * Convert descriptor to class name.
     * ----------------
     * 转换描述符为类名。
     *
     *     getSimpleName("Ljava/lang/String;") -> "java.lang.String"
     *     getSimpleName("[Ljava/lang/String;") -> "java.lang.String[]"
     *     getSimpleName("[[Ljava/lang/String;") -> "java.lang.String[][]"
     *     getSimpleName("[I") -> "int[]"
     *
     * @param typeSign type sign / 类型签名
     * @return simple name / 类名
     */
    @JvmStatic
    fun getSimpleName(typeSign: String): String {
        if (typeSign[0] == '[') {
            return getSimpleName(typeSign.substring(1)) + "[]"
        }
        if (typeSign.length == 1) {
            return primitiveTypeName(typeSign)
        }
        if (typeSign[0] != 'L' || typeSign[typeSign.length - 1] != ';') {
            throw IllegalStateException("Unknown class sign: $typeSign")
        }
        return typeSign.substring(1, typeSign.length - 1).replace('/', '.')
    }

    /**
     * Convert class to simple name.
     * ----------------
     * 转换类为类名。
     *
     *     getSimpleName(String.class) -> "java.lang.String"
     *     getSimpleName(int.class) -> "int"
     *     getSimpleName(int[].class) -> "int[]"
     *     getSimpleName(int[][].class) -> "int[][]"
     *
     * @param clazz class / 类
     * @return simple name / 类名
     */
    @JvmStatic
    fun getSimpleName(clazz: Class<*>): String {
        if (clazz.isArray) {
            return getSimpleName(clazz.componentType!!) + "[]"
        }
        if (clazz.isPrimitive) {
            return when (clazz) {
                Boolean::class.javaPrimitiveType -> "boolean"
                Byte::class.javaPrimitiveType -> "byte"
                Char::class.javaPrimitiveType -> "char"
                Short::class.javaPrimitiveType -> "short"
                Int::class.javaPrimitiveType -> "int"
                Float::class.javaPrimitiveType -> "float"
                Long::class.javaPrimitiveType -> "long"
                Double::class.javaPrimitiveType -> "double"
                Void.TYPE -> "void"
                else -> throw IllegalStateException("Unknown primitive type: $clazz")
            }
        }
        return clazz.name
    }

    /**
     * Get parameter type names from parameter sign.
     * ----------------
     * 从参数签名获取参数类型名。
     *
     *     getParamTypeNames("Ljava/lang/String;I[I") -> ["java.lang.String", "int", "int[]"]
     *
     * @param paramSigns parameter sign / 参数签名
     * @return parameter type names / 参数类型名
     */
    @JvmStatic
    fun getParamTypeNames(paramSigns: String): List<String> {
        val params = mutableListOf<String>()
        var left = 0
        var right = 0
        while (right < paramSigns.length) {
            val c = paramSigns[right]
            if (c == '[') {
                right++
                continue
            } else if (c == 'L') {
                val end = paramSigns.indexOf(';', right)
                right = end
            }
            val sign = paramSigns.substring(left, right + 1)
            params.add(getSimpleName(sign))
            left = ++right
        }
        if (left != right) {
            throw IllegalStateException("Unknown signString: $paramSigns")
        }
        return params
    }

    /**
     * Convert type to type sign.
     * ----------------
     * 转换类型为类型签名。
     *
     *     getTypeSign(boolean.class) -> "Z"
     *     getTypeSign(int.class) -> "I"
     *     getTypeSign(String.class) -> "Ljava/lang/String;"
     *     getTypeSign(String[].class) -> "[Ljava/lang/String;"
     *
     * @param type type / 类型
     * @return type sign / 类型签名
     */
    @JvmStatic
    fun getTypeSign(type: Class<*>): String {
        if (type.isPrimitive) {
            return when (type) {
                Boolean::class.javaPrimitiveType -> "Z"
                Byte::class.javaPrimitiveType -> "B"
                Char::class.javaPrimitiveType -> "C"
                Short::class.javaPrimitiveType -> "S"
                Int::class.javaPrimitiveType -> "I"
                Float::class.javaPrimitiveType -> "F"
                Long::class.javaPrimitiveType -> "J"
                Double::class.javaPrimitiveType -> "D"
                Void.TYPE -> "V"
                else -> throw IllegalStateException("Unknown primitive type: $type")
            }
        }
        return if (type.isArray) "[" + getTypeSign(type.componentType!!)
        else "L" + type.name.replace('.', '/') + ";"
    }

    /**
     * Convert type name to type sign.
     * ----------------
     * 转换类型名为类型签名。
     *
     *     getTypeSign("int") -> "I"
     *     getTypeSign("java.lang.String") -> "Ljava/lang/String;"
     *     getTypeSign("java.lang.String[]") -> "[Ljava/lang/String;"
     *
     * @param typeName type name / 类型名
     * @return type sign / 类型签名
     */
    @JvmStatic
    fun getTypeSign(typeName: String): String {
        if (typeName.endsWith("[]")) {
            return "[" + getTypeSign(typeName.substring(0, typeName.length - 2))
        }
        return primitiveMap[typeName] ?: ("L" + typeName.replace('.', '/') + ";")
    }

    /**
     * Get method sign.
     * ----------------
     * 获取方法签名。
     *
     *     getMethodSign(String.class.getMethod("length")) -> "()I"
     *
     * @param method method / 方法
     * @return method sign / 方法签名
     */
    @JvmStatic
    fun getMethodSign(method: Method): String {
        return buildString {
            append("(")
            append(method.parameterTypes.joinToString("") { getTypeSign(it) })
            append(")")
            append(getTypeSign(method.returnType))
        }
    }

    /**
     * Get constructor sign.
     * ----------------
     * 获取构造方法签名。
     *
     *     getConstructorSign(String.class.getConstructor()) -> "()V"
     *
     * @param constructor constructor / 构造方法
     * @return constructor sign / 构造方法签名
     */
    @JvmStatic
    fun getConstructorSign(constructor: Constructor<*>): String {
        return buildString {
            append("(")
            append(constructor.parameterTypes.joinToString("") { getTypeSign(it) })
            append(")V")
        }
    }

    /**
     * Convert class to class descriptor.
     * ----------------
     * 转换类为类描述符。
     *
     *     getDescriptor(String.class) -> "Ljava/lang/String;"
     *
     * @param clazz class / 类
     * @return class descriptor / 类描述符
     */
    @JvmStatic
    fun getDescriptor(clazz: Class<*>): String {
        return getTypeSign(clazz)
    }

    /**
     * Convert class to class descriptor.
     * ----------------
     * 转换方法为方法描述符。
     *
     *     getClassDescriptor(String.class) -> "Ljava/lang/String;"
     *
     * @param clazz class / 类
     * @return class descriptor / 类描述符
     */
    @JvmStatic
    fun getClassDescriptor(clazz: Class<*>): String {
        return getDescriptor(clazz)
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换方法为方法描述符。
     *
     *     getDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    @JvmStatic
    fun getDescriptor(method: Method): String {
        return buildString {
            append(getTypeSign(method.declaringClass))
            append("->")
            append(method.name)
            append(getMethodSign(method))
        }
    }

    /**
     * Convert method to method descriptor.
     * ----------------
     * 转换构造方法为方法描述符。
     *
     *     getMethodDescriptor(String.class.getMethod("length")) -> "Ljava/lang/String;->length()I"
     *
     * @param method method / 方法
     * @return method descriptor / 方法描述符
     */
    @JvmStatic
    fun getMethodDescriptor(method: Method): String {
        return getDescriptor(method)
    }

    /**
     * Convert constructor to method descriptor.
     * ----------------
     * 转换构造方法为方法描述符。
     *
     *     getDescriptor(String.class.getConstructor()) -> "Ljava/lang/String;-><init>()V"
     *
     * @param constructor constructor / 构造方法
     * @return constructor descriptor / 构造方法描述符
     */
    @JvmStatic
    fun getDescriptor(constructor: Constructor<*>): String {
        return buildString {
            append(getTypeSign(constructor.declaringClass))
            append("->")
            append("<init>")
            append(getConstructorSign(constructor))
        }
    }

    /**
     * Convert constructor to method descriptor.
     * ----------------
     * 转换构造方法为方法描述符。
     *
     *     getMethodDescriptor(String.class.getConstructor()) -> "Ljava/lang/String;-><init>()V"
     *
     * @param constructor constructor / 构造方法
     * @return constructor descriptor / 构造方法描述符
     */
    @JvmStatic
    fun getMethodDescriptor(constructor: Constructor<*>): String {
        return getDescriptor(constructor)
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    @JvmStatic
    fun getDescriptor(field: Field): String {
        return buildString {
            append(getTypeSign(field.declaringClass))
            append("->")
            append(field.name)
            append(":")
            append(getTypeSign(field.type))
        }
    }

    /**
     * Convert field to field descriptor.
     * ----------------
     * 转换字段为字段描述符。
     *
     *     getFieldDescriptor(String.class.getField("CASE_INSENSITIVE_ORDER")) -> "Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;"
     *
     * @param field field / 字段
     * @return field descriptor / 字段描述符
     */
    @JvmStatic
    fun getFieldDescriptor(field: Field): String {
        return getDescriptor(field)
    }
}