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

package org.luckypray.dexkit.wrap

import org.luckypray.dexkit.util.DexSignUtil.getParamTypeNames
import org.luckypray.dexkit.util.DexSignUtil.getTypeName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import org.luckypray.dexkit.util.InstanceUtil
import java.io.Serializable
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class DexMethod: Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }

    val className: String
    val name: String
    val paramTypeNames: List<String>
    val returnTypeName: String

    /**
     * method sign
     * ----------------
     * 方法签名
     */
    val methodSign by lazy {
        getSign()
    }

    private fun getSign() = buildString {
        append("(")
        append(paramTypeNames.joinToString("") { getTypeSign(it) })
        append(")")
        append(getTypeSign(returnTypeName))
    }

    /**
     * Whether the method is a constructor method
     * ----------------
     * 该方法是否为构造方法
     */
    val isConstructor get() = name == "<init>"

    /**
     * Whether the method is a static initializer
     * ----------------
     * 该方法是否为静态初始化方法
     */
    val isStaticInitializer get() = name == "<clinit>"

    /**
     * Whether the method is a normal method
     * ----------------
     * 该方法是否为普通方法
     */
    val isMethod get() = !isStaticInitializer && !isConstructor

    /**
     * Convert method descriptor to [DexMethod].
     * ----------------
     * 转换方法描述符为 [DexMethod]。
     *
     * @param methodDescriptor method descriptor / 方法描述符
     */
    constructor(methodDescriptor: String) {
        val idx1 = methodDescriptor.indexOf("->")
        val idx2 = methodDescriptor.indexOf("(", idx1 + 1)
        val idx3 = methodDescriptor.indexOf(")", idx2 + 1)
        if (idx1 == -1 || idx2 == -1 || idx3 == -1) {
            throw IllegalAccessError("not method descriptor: $methodDescriptor")
        }
        className = getTypeName(methodDescriptor.substring(0, idx1))
        name = methodDescriptor.substring(idx1 + 2, idx2)
        paramTypeNames = getParamTypeNames(methodDescriptor.substring(idx2 + 1, idx3))
        returnTypeName = getTypeName(methodDescriptor.substring(idx3 + 1))
    }

    /**
     * Convert method to [DexMethod].
     * ----------------
     * 转换方法为 [DexMethod]。
     *
     * @param method method / 方法
     */
    constructor(method: Method) {
        className = getTypeName(method.declaringClass)
        name = method.name
        paramTypeNames = method.parameterTypes.map { getTypeName(it) }
        returnTypeName = getTypeName(method.returnType)
    }

    /**
     * Convert constructor to [DexMethod].
     * ----------------
     * 转换构造方法为 [DexMethod]。
     *
     * @param constructor constructor / 构造方法
     */
    constructor(constructor: Constructor<*>) {
        className = constructor.declaringClass.name
        name = "<init>"
        paramTypeNames = constructor.parameterTypes.map { getTypeSign(it) }
        returnTypeName = "void"
    }

    /**
     * Load constructor from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载构造方法
     *
     * @param classLoader class loader / 类加载器
     * @return [Constructor]
     */
    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader): Constructor<*> {
        return InstanceUtil.getConstructorInstance(classLoader, this)
    }

    /**
     * Load method from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载方法
     *
     * @param classLoader class loader / 类加载器
     * @return [Method]
     */
    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader): Method {
        return InstanceUtil.getMethodInstance(classLoader, this)
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append(methodSign)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexMethod) return false
        return className == other.className
            && name == other.name
            && paramTypeNames == other.paramTypeNames
            && returnTypeName == other.returnTypeName
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 +
            name.hashCode() * 31 +
            paramTypeNames.hashCode() * 31 +
            returnTypeName.hashCode()
    }
}