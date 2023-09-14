@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.wrap

import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import java.lang.reflect.Field

class DexField {

    val className: String
    val name: String
    val typeName: String

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

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append(":")
            append(getTypeSign(typeName))
        }
    }
}