@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.wrap

import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
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
    val typeSign get() = getTypeSign(typeName)

    /**
     * Convert field descriptor to [DexField].
     * ----------------
     * 转换字段描述符为 [DexField]。
     *
     * @param fieldDescriptor field descriptor / 字段描述符
     */
    constructor(fieldDescriptor: String) {
        val idx1 = fieldDescriptor.indexOf("->")
        val idx2 = fieldDescriptor.indexOf(":")
        if (idx1 == -1 || idx2 == -1) {
            throw IllegalAccessError("not field descriptor: $fieldDescriptor")
        }
        className = getSimpleName(fieldDescriptor.substring(0, idx1))
        name = fieldDescriptor.substring(idx1 + 2, idx2)
        typeName = getSimpleName(fieldDescriptor.substring(idx2 + 1))
    }

    /**
     * Convert field to [DexField].
     * ----------------
     * 转换字段为 [DexField]。
     *
     * @param field field / 字段
     */
    constructor(field: Field) {
        className = getSimpleName(field.declaringClass)
        name = field.name
        typeName = getSimpleName(field.type)
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
            append(getTypeSign(typeName))
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