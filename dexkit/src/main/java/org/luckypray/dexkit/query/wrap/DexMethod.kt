@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.wrap

import org.luckypray.dexkit.util.DexSignUtil.getParamTypeNames
import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class DexMethod {

    val className: String
    val name: String
    val paramTypeNames: List<String>
    val returnTypeName: String

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
        className = getSimpleName(methodDescriptor.substring(0, idx1))
        name = methodDescriptor.substring(idx1 + 2, idx2)
        paramTypeNames = getParamTypeNames(methodDescriptor.substring(idx2 + 1, idx3))
        returnTypeName = getSimpleName(methodDescriptor.substring(idx3 + 1))
    }

    /**
     * Convert method to [DexMethod].
     * ----------------
     * 转换方法为 [DexMethod]。
     *
     * @param method method / 方法
     */
    constructor(method: Method) {
        className = getSimpleName(method.declaringClass)
        name = method.name
        paramTypeNames = method.parameterTypes.map { getSimpleName(it) }
        returnTypeName = getSimpleName(method.returnType)
    }

    /**
     * Convert constructor to [DexMethod].
     * ----------------
     * 转换构造方法为 [DexMethod]。
     *
     * @param constructor constructor / 构造方法
     */
    constructor(constructor: Constructor<*>) {
        className = constructor.declaringClass.name
        name = "<init>"
        paramTypeNames = constructor.parameterTypes.map { getTypeSign(it) }
        returnTypeName = "void"
    }

    override fun toString(): String {
        return buildString {
            append(getTypeSign(className))
            append("->")
            append(name)
            append("(")
            append(paramTypeNames.joinToString("") { getTypeSign(it) })
            append(")")
            append(getTypeSign(returnTypeName))
        }
    }
}