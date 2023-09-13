@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.wrap

import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign

class DexClass {

    val className: String

    /**
     * Convert class descriptor to [DexClass].
     * ----------------
     * 转换类描述符为 [DexClass]。
     *
     * @param classDescriptor class descriptor / 类描述符
     */
    constructor(classDescriptor: String) {
        if (classDescriptor[0] != 'L' && classDescriptor.last() != ';') {
            throw IllegalAccessError("not class descriptor: $classDescriptor")
        }
        className = getSimpleName(classDescriptor)
    }

    /**
     * Convert class to [DexClass].
     * ----------------
     * 转换类为 [DexClass]。
     *
     * @param clazz class / 类
     */
    constructor(clazz: Class<*>) {
        className = clazz.name
    }

    override fun toString(): String {
        return getTypeSign(className)
    }
}