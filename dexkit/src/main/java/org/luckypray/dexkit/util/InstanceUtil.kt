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

import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

object InstanceUtil {

    private val classCache = WeakCache<String, Class<*>>()

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, dexClass: DexClass): Class<*> {
        return getClassInstance(classLoader, dexClass.typeName)
    }

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader, typeName: String): Class<*> {
        return classCache.get(typeName) {
            var name = typeName
            var arrayDepth = 0

            while (name.endsWith("[]")) {
                arrayDepth++
                name = name.substring(0, name.length - 2)
            }

            val primitiveMap = mapOf(
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
    ): Class<*>? = runCatching {
        getClassInstance(classLoader, typeName)
    }.getOrNull()

    private fun resolveParamTypesOrNull(
        classLoader: ClassLoader,
        paramTypeNames: List<String>
    ): Array<Class<*>>? {
        val out = ArrayList<Class<*>>(paramTypeNames.size)
        for (n in paramTypeNames) {
            val t = tryLoadType(classLoader, n) ?: return null
            out += t
        }
        return out.toTypedArray()
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

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader, dexField: DexField): Field {
        try {
            var clz: Class<*> = getClassInstance(classLoader, dexField.className)
            val type = tryLoadType(classLoader, dexField.typeName)
                ?: throw NoSuchMethodException("Field $dexField not available: return type missing")

            do {
                getDeclaredFieldOrNull(clz, dexField.name)?.let { f ->
                    if (f.type == type) return f
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchFieldException("Field $dexField not found")
        } catch (e: ClassNotFoundException) {
            throw NoSuchFieldException("No such field: $dexField").initCause(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader, dexMethod: DexMethod): Constructor<*> {
        require(dexMethod.isConstructor) { "$dexMethod not a constructor" }

        val paramTypes = resolveParamTypesOrNull(classLoader, dexMethod.paramTypeNames)
            ?: throw NoSuchMethodException("Constructor $dexMethod not available: parameter type(s) missing")

        var clz: Class<*> = getClassInstance(classLoader, dexMethod.className)
        do {
            getDeclaredCtorOrNull(clz, paramTypes)?.let { return it }
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Constructor $dexMethod not found")
    }

    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader, dexMethod: DexMethod): Method {
        require(dexMethod.isMethod) { "$dexMethod not a method" }

        val paramTypes = resolveParamTypesOrNull(classLoader, dexMethod.paramTypeNames)
            ?: throw NoSuchMethodException("Method $dexMethod not available: parameter type(s) missing")
        val retType = tryLoadType(classLoader, dexMethod.returnTypeName)
            ?: throw NoSuchMethodException("Method $dexMethod not available: return type missing")

        var clz: Class<*> = getClassInstance(classLoader, dexMethod.className)
        do {
            getDeclaredMethodOrNull(clz, dexMethod.name, paramTypes)?.let { m ->
                if (m.returnType == retType) return m
            }
        } while (clz.superclass.also { clz = it } != null)
        throw NoSuchMethodException("Method $dexMethod not found")
    }

}