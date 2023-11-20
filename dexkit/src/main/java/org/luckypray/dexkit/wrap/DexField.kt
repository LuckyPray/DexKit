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

import org.luckypray.dexkit.util.DexSignUtil.getTypeName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import org.luckypray.dexkit.util.InstanceUtil
import java.io.Serializable
import java.lang.reflect.Field

class DexField: Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }

    val className: String
    val name: String
    val typeName: String

    /**
     * field type sign
     * ----------------
     * 字段类型签名
     */
    val typeSign by lazy {
        getSign()
    }

    private fun getSign(): String {
        return getTypeSign(typeName)
    }

    /**
     * Convert field descriptor to [DexField].
     * ----------------
     * 转换字段描述符为 [DexField]。
     *
     * @param fieldDescriptor field descriptor / 字段描述符
     */
    constructor(fieldDescriptor: String) {
        val idx1 = fieldDescriptor.indexOf("->")
        val idx2 = fieldDescriptor.indexOf(":", idx1 + 1)
        if (idx1 == -1 || idx2 == -1) {
            throw IllegalAccessError("not field descriptor: $fieldDescriptor")
        }
        className = getTypeName(fieldDescriptor.substring(0, idx1))
        name = fieldDescriptor.substring(idx1 + 2, idx2)
        typeName = getTypeName(fieldDescriptor.substring(idx2 + 1))
    }

    /**
     * Convert field to [DexField].
     * ----------------
     * 转换字段为 [DexField]。
     *
     * @param field field / 字段
     */
    constructor(field: Field) {
        className = getTypeName(field.declaringClass)
        name = field.name
        typeName = getTypeName(field.type)
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
        return InstanceUtil.getFieldInstance(classLoader, this)
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append(":")
            append(typeSign)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexField) return false
        return className == other.className
            && name == other.name
            && typeName == other.typeName
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 +
            name.hashCode() * 31 +
            typeName.hashCode()
    }
}