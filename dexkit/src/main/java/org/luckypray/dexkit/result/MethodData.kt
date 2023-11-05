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

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerMethodMeta
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.InstanceUtil
import org.luckypray.dexkit.util.OpCodeUtil
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class MethodData private constructor(
    bridge: DexKitBridge,
    val id: Int,
    val dexId: Int,
    val classId: Int,
    val modifiers: Int,
    val descriptor: String,
    val returnTypeId: Int,
    val paramTypeIds: List<Int>
) : BaseData(bridge) {

    internal companion object `-Companion` {

        /**
         * [ACC_DECLARED_SYNCHRONIZED](https://source.android.com/docs/core/runtime/dex-format#access-flags)
         */
        const val ACC_DECLARED_SYNCHRONIZED = 0x20000

        fun from(bridge: DexKitBridge, methodMeta: InnerMethodMeta): MethodData {
            var modifiers = methodMeta.accessFlags.toInt()
            if ((modifiers and ACC_DECLARED_SYNCHRONIZED) > 0) {
                modifiers = modifiers xor ACC_DECLARED_SYNCHRONIZED or Modifier.SYNCHRONIZED
            }
            return MethodData(
                bridge,
                methodMeta.id.toInt(),
                methodMeta.dexId.toInt(),
                methodMeta.classId.toInt(),
                modifiers,
                methodMeta.dexDescriptor ?: "",
                methodMeta.returnType.toInt(),
                mutableListOf<Int>().apply {
                    for (i in 0 until methodMeta.parameterTypesLength) {
                        add(methodMeta.parameterTypes(i))
                    }
                }
            )
        }
    }

    private val dexMethod by lazy {
        DexMethod(descriptor)
    }

    /**
     * method sign
     * ----------------
     * 方法签名
     */
    val methodSign get() = dexMethod.methodSign

    /**
     * method declaring class name
     * ----------------
     * 定义方法的类名
     */
    val className get() = dexMethod.className

    /**
     * method name
     * ----------------
     * 方法名
     */
    val methodName get() = dexMethod.name

    /**
     * @see methodName
     */
    val name get() = dexMethod.name

    /**
     * method parameter type names
     * ----------------
     * 方法参数类型名
     */
    val paramTypeNames get() = dexMethod.paramTypeNames

    /**
     * method return type name
     * ----------------
     * 方法返回类型名
     */
    val returnTypeName get() = dexMethod.returnTypeName

    /**
     * Whether the method is a constructor method
     * ----------------
     * 该方法是否为构造方法
     */
    val isConstructor get() = dexMethod.isConstructor

    /**
     * Whether the method is a normal method
     * ----------------
     * 该方法是否为普通方法
     */
    val isMethod get() = dexMethod.isMethod

    /**
     * Get declared class' [ClassData]
     * ----------------
     * 获取定义方法的类的 [ClassData]
     */
    fun getClass(): ClassData? {
        return bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, classId))).firstOrNull()
    }

    /**
     * Get return type's [ClassData]
     * ----------------
     * 获取返回类型的 [ClassData]
     */
    fun getReturnType(): ClassData? {
        return bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, returnTypeId))).firstOrNull()
    }

    /**
     * Get parameter types' [ClassDataList]
     * ----------------
     * 获取参数类型的 [ClassDataList]
     */
    fun getParameterTypes(): ClassDataList {
        return bridge.getTypeByIds(paramTypeIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get parameter types count
     * ----------------
     * 获取参数类型的数量
     */
    fun getParameterNames(): List<String?>? {
        return bridge.getParameterNames(getEncodeId(dexId, id))
    }

    /**
     * Get declared annotations.
     * ----------------
     * 获取标注的注解列表。
     */
    fun getAnnotations(): List<AnnotationData> {
        return bridge.getMethodAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Get parameter's annotations.
     * ----------------
     * 获取标注的注解列表。
     */
    fun getParamAnnotations(): List<List<AnnotationData>> {
        return bridge.getParameterAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Get opcodes (range: 0-255)
     * ----------------
     * 获取操作码列表 (范围：0-255)
     */
    fun getOpCodes(): List<Int> {
        return bridge.getMethodOpCodes(getEncodeId(dexId, id))
    }

    /**
     * Get opcode's smali instruction
     * ----------------
     * 获取操作码对应的 smali 指令
     */
    fun getOpNames(): List<String> {
        return getOpCodes().map { OpCodeUtil.getOpFormat(it) }
    }

    /**
     * Get method callers
     * ----------------
     * 获取调用该方法的方法列表
     */
    fun getMethodCallers(): List<MethodData> {
        return bridge.getCallMethods(getEncodeId(dexId, id))
    }

    /**
     * Get method invoke methods
     * ----------------
     * 获取该方法调用的方法列表
     */
    fun getInvokeMethods(): List<MethodData> {
        return bridge.getInvokeMethods(getEncodeId(dexId, id))
    }

    /**
     * Get method using strings
     * ----------------
     * 获取该方法使用的字符串列表
     */
    fun getUsingStrings(): List<String> {
        return bridge.getMethodUsingStrings(getEncodeId(dexId, id))
    }

    /**
     * Get method using fields
     * ----------------
     * 获取该方法使用的字段列表
     */
    fun getUsingFields(): List<UsingFieldData> {
        return bridge.getMethodUsingFields(getEncodeId(dexId, id))
    }

    /**
     * Load declared class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载定义方法的类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, className)
    }

    /**
     * Load return type from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载返回类型
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getReturnTypeInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, returnTypeName)
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
    fun getConstructorInstance(classLoader: ClassLoader) = dexMethod.getConstructorInstance(classLoader)

    /**
     * Load method from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载方法
     *
     * @param classLoader class loader / 类加载器
     * @return [Method]
     */
    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader) = dexMethod.getMethodInstance(classLoader)

    /**
     * Convert to [DexMethod]
     * ----------------
     * 转换为 [DexMethod]
     *
     * @return [DexMethod]
     */
    fun toDexMethod(): DexMethod {
        return dexMethod
    }

    override fun toString(): String {
        return buildString {
            if (modifiers != 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append(returnTypeName)
            append(" ")
            append(className)
            append(".")
            append(name)
            append("(")
            append(paramTypeNames.joinToString(", "))
            append(")")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MethodData && other.descriptor == descriptor
    }

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }
}