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
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.InstanceUtil
import org.luckypray.dexkit.wrap.DexField
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class FieldData private constructor(
    bridge: DexKitBridge,
    id: Int,
    dexId: Int,
    private val classId: Int,
    val modifiers: Int,
    val descriptor: String,
    private val typeId: Int
): BaseData(bridge, id, dexId) {

    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, fieldMeta: InnerFieldMeta) = FieldData(
            bridge,
            fieldMeta.id.toInt(),
            fieldMeta.dexId.toInt(),
            fieldMeta.classId.toInt(),
            fieldMeta.accessFlags.toInt(),
            fieldMeta.dexDescriptor!!,
            fieldMeta.typeId.toInt()
        )
    }

    private val dexField by lazy {
        DexField(descriptor)
    }

    /**
     * field type sign
     * ----------------
     * 字段类型签名
     */
    val typeSign get() = dexField.typeSign

    /**
     * field declaring class name
     * ----------------
     * 定义字段的类名
     */
    val className get() = dexField.className

    /**
     * field declaring class name
     * ----------------
     * 定义字段的类名
     */
    val declaredClassName get() = className

    /**
     * field name
     * ----------------
     * 字段名
     */
    val fieldName get() = dexField.name

    /**
     * @see fieldName
     */
    val name get() = dexField.name

    /**
     * field type name
     * ----------------
     * 字段类型名
     */
    val typeName get() = dexField.typeName

    /**
     * get declared class' [ClassData]
     * ----------------
     * 获取定义字段的类的 [ClassData]
     */
    val declaredClass: ClassData by lazy {
        bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, classId))).first()
    }

    /**
     * get field type's [ClassData]
     * ----------------
     * 获取字段类型的 [ClassData]
     */
    val type: ClassData by lazy {
        bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, typeId))).first()
    }

    /**
     * Get declared annotations.
     * ----------------
     * 获取标注的注解列表。
     */
    val annotations: List<AnnotationData> by lazy {
        bridge.getFieldAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Using smali `iput-*`、`sput-*` instructions to read this field's methods
     * ----------------
     * 使用 smali `iget-*`、`sget-*` 指令读取字段的方法
     */
    val readers by lazy {
        bridge.readFieldMethods(getEncodeId(dexId, id))
    }

    /**
     * Using smali `iput-*`、`sput-*` instructions to write this field's methods
     * ----------------
     * 使用 smali `iput-*`、`sput-*` 指令写入字段的方法
     */
    val writers by lazy {
        bridge.writeFieldMethods(getEncodeId(dexId, id))
    }

    /**
     * Load declared class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载定义字段的类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, className)
    }

    /**
     * Load field's type from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载字段类型
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getTypeInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, typeName)
    }

    /**
     * Get field's [Field] from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 获取字段对应的 [Field]
     *
     * @param classLoader class loader / 类加载器
     * @return [Field]
     */
    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader): Field {
        return dexField.getFieldInstance(classLoader, Modifier.isStatic(modifiers))
    }

    /**
     * Convert to [DexField]
     * ----------------
     * 转换为 [DexField]
     *
     * @return [DexField]
     */
    fun toDexField(): DexField {
        return dexField
    }

    override fun toString(): String {
        return buildString {
            if (modifiers > 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append(typeName)
            append(" ")
            append(className)
            append(".")
            append(name)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FieldData && other.descriptor == descriptor
    }

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }
}