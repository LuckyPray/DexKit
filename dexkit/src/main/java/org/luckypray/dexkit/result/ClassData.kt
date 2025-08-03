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
import org.luckypray.dexkit.InnerClassMeta
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.InstanceUtil
import org.luckypray.dexkit.wrap.DexClass
import java.lang.reflect.Modifier

class ClassData private constructor(
    bridge: DexKitBridge,
    id: Int,
    dexId: Int,
    val sourceFile: String,
    val modifiers: Int,
    val descriptor: String,
    private val superClassId: Int?,
    private val interfaceIds: List<Int>,
    private val methodIds: List<Int>,
    private val fieldIds: List<Int>,
): BaseData(bridge, id, dexId) {

    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, classMeta: InnerClassMeta) = ClassData(
            bridge,
            classMeta.id.toInt(),
            classMeta.dexId.toInt(),
            classMeta.sourceFile ?: "",
            classMeta.accessFlags.toInt(),
            classMeta.dexDescriptor ?: "",
            classMeta.superClass.toInt().let { if (it == -1) null else it },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.interfacesLength) {
                    add(classMeta.interfaces(i))
                }
            },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.methodsLength) {
                    add(classMeta.methods(i))
                }
            },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.fieldsLength) {
                    add(classMeta.fields(i))
                }
            },
        )
    }

    private val dexClass by lazy {
        DexClass(descriptor)
    }

    /**
     * Full class name
     * ----------------
     * 完整类名
     *
     *     e.g. java.lang.String
     */
    val name get() = dexClass.typeName

    /**
     * Simple class name
     * ----------------
     * 简单类名
     *
     *     e.g. String
     */
    val simpleName get() = dexClass.simpleName

    val isArray get() = dexClass.isArray

    /**
     * Get super's [ClassData], if super class not defined in dex, return null
     * ----------------
     * 获取父类的 [ClassData]，如果父类未在 dex 中定义，返回 null
     */
    val superClass: ClassData? by lazy {
        superClassId?.let {
            bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, it))).firstOrNull()
        }
    }

    /**
     * Get implemented interfaces
     * ----------------
     * 获取实现接口列表
     */
    val interfaces: ClassDataList by lazy {
        bridge.getTypeByIds(interfaceIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get implemented interfaces count
     * ----------------
     * 获取实现接口的数量
     */
    val interfaceCount: Int get() = interfaceIds.size

    /**
     * Get declared methods (include static block: `<clinit>`, and constructor: `<init>`)
     * ----------------
     * 获取定义的方法列表（包含 静态代码块: `<clinit>`，以及构造函数: `<init>`）
     */
    val methods: MethodDataList by lazy {
        bridge.getMethodByIds(methodIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get declared methods count
     * ----------------
     * 获取定义的方法数量
     */
    val methodCount: Int get() = methodIds.size

    /**
     * Get declared fields
     * ----------------
     * 获取定义的字段列表
     */
    val fields: FieldDataList by lazy {
        bridge.getFieldByIds(fieldIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get declared fields count
     * ----------------
     * 获取定义的字段数量
     */
    val fieldCount: Int get() = fieldIds.size

    /**
     * Get declared annotations.
     * ----------------
     * 获取标注的注解列表。
     */
    val annotations: List<AnnotationData> by lazy {
        bridge.getClassAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Load this class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载此类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getInstance(classLoader: ClassLoader) = dexClass.getInstance(classLoader)

    /**
     * Load super class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载父类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getSuperClassInstance(classLoader: ClassLoader): Class<*>? {
        return superClass?.getInstance(classLoader)
    }

    /**
     * Load implemented interfaces from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载实现的接口
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getInterfaceInstances(classLoader: ClassLoader): List<Class<*>> {
        return interfaces.map { InstanceUtil.getClassInstance(classLoader, it.name) }
    }

    @Deprecated(
        message = "please use toDexClass",
        replaceWith = ReplaceWith("toDexClass()")
    )
    fun toDexType(): DexClass {
        return dexClass
    }

    /**
     * Convert to [DexClass]
     * ----------------
     * 转换为 [DexClass]
     *
     * @return [DexClass]
     */
    fun toDexClass(): DexClass {
        return dexClass
    }

    // region DexKit Search

    /**
     * Search in this class with multiple conditions.
     * ----------------
     * 在本类中进行多条件方法搜索。
     *
     * @param [findMethod] query object / 查询对象
     * @return [MethodDataList]
     */
    fun findMethod(findMethod: FindMethod): MethodDataList {
        findMethod.searchInClass(listOf(this))
        return bridge.findMethod(findMethod)
    }

    /**
     * @see findMethod
     */
    @JvmSynthetic
    fun findMethod(init: FindMethod.() -> Unit): MethodDataList {
        return findMethod(FindMethod().apply(init))
    }

    /**
     * Search in this class with multiple conditions.
     * ----------------
     * 在本类中进行多条件字段搜索。
     *
     * @param [findField] query object / 查询对象
     * @return [FieldDataList]
     */
    fun findField(findField: FindField): FieldDataList {
        findField.searchInClass(listOf(this))
        return bridge.findField(findField)
    }

    /**
     * @see findField
     */
    @JvmSynthetic
    fun findField(init: FindField.() -> Unit): FieldDataList {
        return findField(FindField().apply(init))
    }

    // endregion

    override fun toString(): String {
        return buildString {
            if (modifiers > 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append("class $name")
            superClass?.let {
                append(" extends ")
                append(it.name)
            }
            if (interfaceCount > 0) {
                append(" implements ")
                append(interfaces.joinToString(", ") { it.name })
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ClassData && descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }
}