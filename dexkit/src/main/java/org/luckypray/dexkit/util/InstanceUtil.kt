package org.luckypray.dexkit.util

import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import org.luckypray.dexkit.wrap.DexClass
import java.lang.reflect.Array
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object InstanceUtil {

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, dexClass: DexClass): Class<*> {
        return getClassInstance(classLoader, dexClass.typeName)
    }

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, typeName: String): Class<*> {
        if (typeName.endsWith("[]")) {
            val clazz = getClassInstance(classLoader, typeName.substring(0, typeName.length - 2))
            return Array.newInstance(clazz, 0)::class.java
        }
        return when (typeName) {
            "boolean" -> Int::class.javaPrimitiveType!!
            "byte" -> Byte::class.javaPrimitiveType!!
            "char" -> Char::class.javaPrimitiveType!!
            "short" -> Short::class.javaPrimitiveType!!
            "int" -> Int::class.javaPrimitiveType!!
            "long" -> Long::class.javaPrimitiveType!!
            "float" -> Float::class.javaPrimitiveType!!
            "double" -> Double::class.javaPrimitiveType!!
            "void" -> Void.TYPE
            else -> classLoader.loadClass(typeName)
        }
    }

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader, dexField: DexField): Field {
        try {
            var clz = classLoader.loadClass(dexField.className)
            do {
                for (field in clz.declaredFields) {
                    if (dexField.name == field.name && dexField.typeName == field.type.name) {
                        field.isAccessible = true
                        return field
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchFieldException("Field $dexField not found")
        } catch (e: ClassNotFoundException) {
            throw NoSuchFieldException("No such field: $dexField").initCause(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader, dexMethod: DexMethod): Constructor<*> {
        if (!dexMethod.isConstructor) {
            throw IllegalArgumentException("$dexMethod not a constructor")
        }
        try {
            var clz = classLoader.loadClass(dexMethod.className)
            do {
                for (constructor in clz.declaredConstructors) {
                    if (dexMethod.methodSign == DexSignUtil.getConstructorSign(constructor)) {
                        constructor.isAccessible = true
                        return constructor
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Constructor $dexMethod not found")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $dexMethod").initCause(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader, dexMethod: DexMethod): Method {
        if (!dexMethod.isMethod) {
            throw IllegalArgumentException("$dexMethod not a method")
        }
        try {
            var clz = classLoader.loadClass(dexMethod.className)
            do {
                for (method in clz.declaredMethods) {
                    if (method.name == dexMethod.name
                        && dexMethod.methodSign == DexSignUtil.getMethodSign(method)) {
                        method.isAccessible = true
                        return method
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Method $dexMethod not found")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $dexMethod").initCause(e)
        }
    }

}