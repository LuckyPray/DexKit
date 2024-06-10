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

class DexClass: ISerializable {

    companion object {

        @JvmStatic
        fun deserialize(descriptor: String) = DexClass(descriptor)
    }

    val typeName: String

    val className get() = typeName

    val simpleName get() = typeName.substringAfterLast('.')

    val isArray get() = typeName.endsWith("[]")

    /**
     * Convert class descriptor to [DexClass].
     * ----------------
     * 转换类描述符为 [DexClass]。
     *
     * @param descriptor class descriptor / 类描述符
     */
    constructor(descriptor: String) {
        typeName = getTypeName(descriptor)
    }

    /**
     * Convert class to [DexClass].
     * ----------------
     * 转换类为 [DexClass]。
     *
     * @param clazz class / 类
     */
    constructor(clazz: Class<*>) {
        typeName = getTypeName(clazz)
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
    fun getInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, this)
    }

    override fun toString(): String {
        return getTypeSign(typeName)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexClass) return false
        return typeName == other.typeName
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }
}