@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.wrap

import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign

class DexType {

    val typeName: String

    /**
     * Convert class descriptor to [DexType].
     * ----------------
     * 转换类描述符为 [DexType]。
     *
     * @param classDescriptor class descriptor / 类描述符
     */
    constructor(classDescriptor: String) {
        typeName = getSimpleName(classDescriptor)
    }

    /**
     * Convert class to [DexType].
     * ----------------
     * 转换类为 [DexType]。
     *
     * @param clazz class / 类
     */
    constructor(clazz: Class<*>) {
        typeName = clazz.typeName
    }

    override fun toString(): String {
        return getTypeSign(typeName)
    }
}