@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.util

import java.lang.reflect.Constructor
import java.lang.reflect.Method

object DexSignUtil {

    private fun primitiveTypeName(typeSign: String): String {
        return when (typeSign) {
            "Z" -> "boolean"
            "B" -> "byte"
            "C" -> "char"
            "S" -> "short"
            "I" -> "int"
            "F" -> "float"
            "J" -> "long"
            "D" -> "double"
            "V" -> "void"
            else -> throw IllegalArgumentException("Unknown primitive typeSign: $typeSign")
        }
    }

    fun getSimpleName(sign: String): String {
        val arrDimensions = sign.filter { it == '[' }.length
        var type = sign.substring(arrDimensions)
        type = if (type.length == 1) {
            primitiveTypeName(type)
        } else {
            type.substring(1, type.length - 1).replace('/', '.')
        }
        return type + "[]".repeat(arrDimensions)
    }

    @JvmStatic
    fun getParamTypeNames(paramsSign: String): List<String> {
        val params = mutableListOf<String>()
        var left = 0
        var right = 0
        while (right < paramsSign.length) {
            val c = paramsSign[right]
            if (c == '[') {
                right++
                continue
            } else if (c == 'L') {
                val end = paramsSign.indexOf(';', right)
                right = end
            }
            val sign = paramsSign.substring(left, right + 1)
            params.add(getSimpleName(sign))
            left = ++right
        }
        if (left != right) {
            throw IllegalStateException("Unknown signString: $paramsSign")
        }
        return params
    }

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

    @JvmStatic
    fun getMethodSign(method: Method): String {
        return buildString {
            append("(")
            append(method.parameterTypes.joinToString("") { getTypeSign(it) })
            append(")")
            append(getTypeSign(method.returnType))
        }
    }

    @JvmStatic
    fun getConstructorSign(constructor: Constructor<*>): String {
        return buildString {
            append("(")
            append(constructor.parameterTypes.joinToString("") { getTypeSign(it) })
            append(")V")
        }
    }
}