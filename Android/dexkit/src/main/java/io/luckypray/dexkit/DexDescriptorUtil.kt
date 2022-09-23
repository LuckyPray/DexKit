@file:JvmName("DexDescriptorUtil")
package io.luckypray.dexkit

import java.lang.reflect.Constructor
import java.lang.reflect.Method


fun getClassName(classDesc: String): String {
    if (classDesc.startsWith("L") && classDesc.endsWith(";")) {
        return classDesc.substring(1, classDesc.length - 1).replace('/', '.')
    }
    return classDesc.replace('/', '.')
}

fun getTypeSig(type: Class<*>): String {
    if (type.isPrimitive) {
        return when (type) {
            Boolean::class.javaObjectType -> "Z"
            Char::class.javaObjectType -> "C"
            Short::class.javaObjectType -> "S"
            Int::class.javaObjectType -> "I"
            Float::class.javaObjectType -> "F"
            Long::class.javaObjectType -> "J"
            Double::class.javaObjectType -> "D"
            Void.TYPE -> "V"
            else -> throw IllegalStateException("Unknown primitive type: $type")
        }
    }
    return if (type.isArray) {
        "[" + getTypeSig(type.componentType!!)
    } else "L" + type.name.replace('.', '/') + ";"
}

fun getMethodTypeSig(method: Method): String {
    return "(" + method.parameterTypes.joinToString("") { getTypeSig(it) } + ")" + getTypeSig(
        method.returnType
    )
}

fun getConstructorTypeSig(constructor: Constructor<*>): String {
    return "(" + constructor.parameterTypes.joinToString("") { getTypeSig(it) } + ")V"
}