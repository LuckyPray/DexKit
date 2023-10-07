@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.wrap

import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import org.luckypray.dexkit.util.InstanceUtil
import java.io.Serializable
import kotlin.jvm.Throws

class DexClass: Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }

    val typeName: String

    /**
     * Convert class descriptor to [DexClass].
     * ----------------
     * 转换类描述符为 [DexClass]。
     *
     * @param classDescriptor class descriptor / 类描述符
     */
    constructor(classDescriptor: String) {
        typeName = getSimpleName(classDescriptor)
    }

    /**
     * Convert class to [DexClass].
     * ----------------
     * 转换类为 [DexClass]。
     *
     * @param clazz class / 类
     */
    constructor(clazz: Class<*>) {
        typeName = clazz.typeName
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