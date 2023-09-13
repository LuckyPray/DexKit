@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.wrap

import org.luckypray.dexkit.util.DexSignUtil.getParamTypeNames
import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class DexMethod {

    val declaredClass: String
    val name: String
    val paramTypes: List<String>
    val returnType: String

    /**
     * Convert method descriptor to [DexMethod].
     * ----------------
     * 转换方法描述符为 [DexMethod]。
     *
     * @param methodDescriptor method descriptor / 方法描述符
     */
    constructor(methodDescriptor: String) {
        val idx1 = methodDescriptor.indexOf("->")
        val idx2 = methodDescriptor.indexOf("(")
        val idx3 = methodDescriptor.indexOf(")")
        if (idx1 == -1 || idx2 == -1 || idx3 == -1) {
            throw IllegalAccessError("not method descriptor: $methodDescriptor")
        }
        declaredClass = getSimpleName(methodDescriptor.substring(0, idx1))
        name = methodDescriptor.substring(idx1 + 2, idx2)
        paramTypes = getParamTypeNames(methodDescriptor.substring(idx2 + 1, idx3))
        returnType = getSimpleName(methodDescriptor.substring(idx3 + 1))
    }

    /**
     * Convert method to [DexMethod].
     * ----------------
     * 转换方法为 [DexMethod]。
     *
     * @param method method / 方法
     */
    constructor(method: Method) {
        declaredClass = getSimpleName(method.declaringClass)
        name = method.name
        paramTypes = method.parameterTypes.map { getSimpleName(it) }
        returnType = getSimpleName(method.returnType)
    }

    /**
     * Convert constructor to [DexMethod].
     * ----------------
     * 转换构造方法为 [DexMethod]。
     *
     * @param constructor constructor / 构造方法
     */
    constructor(constructor: Constructor<*>) {
        declaredClass = constructor.declaringClass.name
        name = "<init>"
        paramTypes = constructor.parameterTypes.map { getTypeSign(it) }
        returnType = "void"
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(declaredClass))
            append("->")
            append(name)
            append("(")
            append(paramTypes.joinToString("") { getTypeSign(it) })
            append(")")
            append(getTypeSign(returnType))
        }
    }
}