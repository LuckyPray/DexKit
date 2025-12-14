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

package org.luckypray.dexkit.util

import org.luckypray.dexkit.util.DexSignUtil.getConstructorSign
import org.luckypray.dexkit.util.DexSignUtil.getMethodSign
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object InstanceUtil {

    private val classCache = AdaptiveLoaderCache<String, Class<*>>(weakValue = true)

    private val primitiveMap = mapOf(
        "boolean" to Boolean::class.javaPrimitiveType,
        "byte"    to Byte::class.javaPrimitiveType,
        "char"    to Char::class.javaPrimitiveType,
        "short"   to Short::class.javaPrimitiveType,
        "int"     to Int::class.javaPrimitiveType,
        "long"    to Long::class.javaPrimitiveType,
        "float"   to Float::class.javaPrimitiveType,
        "double"  to Double::class.javaPrimitiveType,
        "void"    to Void.TYPE
    )

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, dexClass: DexClass): Class<*> {
        return getClassInstance(classLoader, dexClass.typeName)
    }

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, typeName: String): Class<*> {
        return classCache.get(classLoader, typeName) {
            var name = typeName
            var arrayDepth = 0

            while (name.endsWith("[]")) {
                arrayDepth++
                name = name.substring(0, name.length - 2)
            }

            val baseClass = primitiveMap[name] ?: classLoader.loadClass(name)

            var clazz: Class<*> = baseClass
            repeat(arrayDepth) {
                clazz = java.lang.reflect.Array.newInstance(clazz, 0)::class.java
            }
            clazz
        }
    }

    private fun tryLoadType(
        classLoader: ClassLoader,
        typeName: String
    ): Result<Class<*>> = runCatching {
        getClassInstance(classLoader, typeName)
    }

    private fun resolveParamTypesOrNull(
        classLoader: ClassLoader,
        paramTypeNames: List<String>
    ): Result<Array<Class<*>>> {
        val out = ArrayList<Class<*>>(paramTypeNames.size)
        for (n in paramTypeNames) {
            val t = tryLoadType(classLoader, n).getOrElse { exception ->
                return Result.failure(exception)
            }
            out += t
        }
        return Result.success(out.toTypedArray())
    }

    private fun getDeclaredMethodOrNull(
        clazz: Class<*>,
        name: String,
        paramTypes: Array<Class<*>>
    ): Method? = runCatching {
        clazz.getDeclaredMethod(name, *paramTypes).apply { isAccessible = true }
    }.getOrNull()

    private fun getDeclaredCtorOrNull(
        clazz: Class<*>,
        paramTypes: Array<Class<*>>
    ): Constructor<*>? = runCatching {
        clazz.getDeclaredConstructor(*paramTypes).apply { isAccessible = true }
    }.getOrNull()

    private fun getDeclaredFieldOrNull(
        clazz: Class<*>,
        name: String
    ): Field? = runCatching {
        clazz.getDeclaredField(name).apply { isAccessible = true }
    }.getOrNull()

    @JvmOverloads
    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader, dexField: DexField, isStatic: Boolean? = null): Field {

        val declaredClass: Class<*> = getClassInstance(classLoader, dexField.className)
        val type = tryLoadType(classLoader, dexField.typeName).getOrElse {
            throw NoSuchFieldException("Field $dexField not available: type missing")
                .apply { initCause(it) }
        }

        var clz: Class<*>? = declaredClass
        while (clz != null) {
            getDeclaredFieldOrNull(clz, dexField.name)?.let { f ->
                if (f.type == type) return f
            }
            clz = clz.superclass
        }

        // When certain classes use the @RequiresApi annotation, a NoClassDefFoundError
        // may occur if the Android device is running an API at a level lower than the
        // required level. Ideally, we should use getDeclaredMethodsUnchecked. However,
        // it is a @hide API that requires using hiddenApiByPass, which is unacceptable
        // for this project.
        try {
            clz = declaredClass
            while (clz != null) {
                for (field in clz.declaredFields) {
                    if (dexField.name == field.name
                        && dexField.typeSign == getTypeSign(field.type)) {
                        field.isAccessible = true
                        return field
                    }
                }
                clz = clz.superclass
            }
        } catch (_: Throwable) {}

        // It's a fallback option, but it maybe cause the class to be initialized.
        NativeReflect.getReflectedField(
            declaredClass,
            dexField.name,
            dexField.typeSign,
            isStatic
        )?.let {
            return it.apply { isAccessible = true }
        }

        throw NoSuchFieldException("Field $dexField not found")
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader, dexMethod: DexMethod): Constructor<*> {
        require(dexMethod.isConstructor) { "$dexMethod not a constructor" }

        val declaredClass: Class<*> = getClassInstance(classLoader, dexMethod.className)
        val paramTypes = resolveParamTypesOrNull(classLoader, dexMethod.paramTypeNames).getOrElse {
            throw NoSuchMethodException("Constructor $dexMethod not available: parameter type(s) missing")
                .apply { initCause(it) }
        }

        getDeclaredCtorOrNull(declaredClass, paramTypes)?.let { return it }

        // When certain classes use the @RequiresApi annotation, a NoClassDefFoundError
        // may occur if the Android device is running an API at a level lower than the
        // required level. Ideally, we should use getDeclaredMethodsUnchecked. However,
        // it is a @hide API that requires using hiddenApiByPass, which is unacceptable
        // for this project.
        try {
            for (constructor in declaredClass.declaredConstructors) {
                if (dexMethod.methodSign == getConstructorSign(constructor)) {
                    constructor.isAccessible = true
                    return constructor
                }
            }
        } catch (_: Throwable) {}

        // It's a fallback option, but it maybe cause the class to be initialized.
        NativeReflect.getReflectedMethod(
            declaredClass,
            dexMethod.name,
            dexMethod.methodSign,
            false
        )?.let {
            return (it as Constructor<*>).apply { isAccessible = true }
        }

        throw NoSuchMethodException("Constructor $dexMethod not found")
    }

    @JvmOverloads
    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader, dexMethod: DexMethod, isStatic: Boolean? = null): Method {
        require(dexMethod.isMethod) { "$dexMethod not a method" }

        val declaredClass: Class<*> = getClassInstance(classLoader, dexMethod.className)
        val paramTypes = resolveParamTypesOrNull(classLoader, dexMethod.paramTypeNames).getOrElse {
            throw NoSuchMethodException("Method $dexMethod not available: parameter type(s) missing")
                .apply { initCause(it) }
        }
        val retType = tryLoadType(classLoader, dexMethod.returnTypeName).getOrElse {
            throw NoSuchMethodException("Method $dexMethod not available: return type missing")
                .apply { initCause(it) }
        }

        var clz: Class<*>? = declaredClass
        while (clz != null) {
            getDeclaredMethodOrNull(clz, dexMethod.name, paramTypes)?.let { m ->
                // https://github.com/LuckyPray/DexKit/issues/45
                // Class.getDeclaredMethod() matches only by method name + parameter types.
                // If a compiler-generated bridge/synthetic method exists alongside another
                // method with the same name and parameters, the wrong method might be returned,
                // leading to a match failure.
                if (m.returnType == retType) return m
            }
            clz = clz.superclass
        }

        // When certain classes use the @RequiresApi annotation, a NoClassDefFoundError
        // may occur if the Android device is running an API at a level lower than the
        // required level. Ideally, we should use getDeclaredMethodsUnchecked. However,
        // it is a @hide API that requires using hiddenApiByPass, which is unacceptable
        // for this project.
        try {
            clz = declaredClass
            while (clz != null) {
                for (method in clz.declaredMethods) {
                    if (method.name == dexMethod.name
                        && dexMethod.methodSign == getMethodSign(method)) {
                        method.isAccessible = true
                        return method
                    }
                }
                clz = clz.superclass
            }
        } catch (_: Throwable) {}

        // It's a fallback option, but it maybe cause the class to be initialized.
        NativeReflect.getReflectedMethod(
            declaredClass,
            dexMethod.name,
            dexMethod.methodSign,
            isStatic
        )?.let {
            return (it as Method).apply { isAccessible = true }
        }

        throw NoSuchMethodException("Method $dexMethod not found")
    }

}