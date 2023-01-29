package io.luckypray.dexkit.util

import java.lang.reflect.Constructor
import java.lang.reflect.Method

object DexDescriptorUtil {

    @JvmStatic
    fun getClassName(classDesc: String): String {
        if (classDesc.startsWith("L") && classDesc.endsWith(";")) {
            return classDesc.substring(1, classDesc.length - 1).replace('/', '.')
        }
        return classDesc.replace('/', '.')
    }

    @JvmStatic
    fun getTypeSig(type: Class<*>): String {
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
        return if (type.isArray) "[" + getTypeSig(type.componentType!!)
        else "L" + type.name.replace('.', '/') + ";"
    }

    @JvmStatic
    fun getMethodSignature(method: Method): String {
        return buildString {
            append("(")
            append(method.parameterTypes.joinToString("") { getTypeSig(it) })
            append(")")
            append(getTypeSig(method.returnType))
        }
    }

    @JvmStatic
    fun getConstructorSignature(constructor: Constructor<*>): String {
        return buildString {
            append("(")
            append(constructor.parameterTypes.joinToString("") { getTypeSig(it) })
            append(")V")
        }
    }
}