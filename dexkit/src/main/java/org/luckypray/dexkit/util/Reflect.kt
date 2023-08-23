@file:JvmSynthetic
package org.luckypray.dexkit.util

import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method


@Throws(ClassNotFoundException::class)
internal fun getClassInstance(classLoader: ClassLoader, className: String): Class<*> {
    return classLoader.loadClass(className)
}

@Throws(NoSuchFieldException::class)
internal fun getFieldInstance(classLoader: ClassLoader, fieldData: FieldData): Field {
    try {
        var clz = classLoader.loadClass(fieldData.className)
        do {
            for (field in clz.declaredFields) {
                if (field.name == fieldData.fieldName && fieldData.typeSign == DexSignUtil.getTypeSign(field.type)) {
                    field.isAccessible = true
                    return field
                }
            }
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchFieldException("Field $fieldData not found in ${fieldData.className}")
    } catch (e: ClassNotFoundException) {
        throw NoSuchFieldException("No such field: $fieldData").initCause(e)
    }
}

@Throws(NoSuchMethodException::class)
internal fun getConstructorInstance(classLoader: ClassLoader, methodData: MethodData): Constructor<*> {
    if (!methodData.isConstructor) {
        throw IllegalArgumentException("$methodData not a constructor")
    }
    try {
        var clz = classLoader.loadClass(methodData.className)
        do {
            for (constructor in clz.declaredConstructors) {
                if (methodData.dexDescriptor == DexSignUtil.getConstructorSign(constructor)) {
                    constructor.isAccessible = true
                    return constructor
                }
            }
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Constructor $methodData not found in $methodData.dexDescriptor")
    } catch (e: ClassNotFoundException) {
        throw NoSuchMethodException("No such method: $methodData").initCause(e)
    }
}

@Throws(NoSuchMethodException::class)
internal fun getMethodInstance(classLoader: ClassLoader, methodData: MethodData): Method {
    if (!methodData.isMethod) {
        throw IllegalArgumentException("$methodData not a method")
    }
    try {
        var clz = classLoader.loadClass(methodData.className)
        do {
            for (method in clz.declaredMethods) {
                if (method.name == methodData.methodName && methodData.dexDescriptor == DexSignUtil.getMethodSign(method)) {
                    method.isAccessible = true
                    return method
                }
            }
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $methodData not found in $methodData.dexDescriptor")
    } catch (e: ClassNotFoundException) {
        throw NoSuchMethodException("No such method: $methodData").initCause(e)
    }
}