@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.wrap

import org.luckypray.dexkit.util.DexSignUtil.getParamTypeNames
import org.luckypray.dexkit.util.DexSignUtil.getSimpleName
import org.luckypray.dexkit.util.DexSignUtil.getTypeSign
import org.luckypray.dexkit.util.InstanceUtil
import java.io.Serializable
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class DexMethod: Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }

    val className: String
    val name: String
    val paramTypeNames: List<String>
    val returnTypeName: String

    /**
     * method sign
     * ----------------
     * 方法签名
     */
    val methodSign get() = buildString {
        append("(")
        append(paramTypeNames.joinToString("") { getTypeSign(it) })
        append(")")
        append(getTypeSign(returnTypeName))
    }

    /**
     * Whether the method is a constructor method
     * ----------------
     * 该方法是否为构造方法
     */
    val isConstructor get() = name == "<init>"

    /**
     * Whether the method is a normal method
     * ----------------
     * 该方法是否为普通方法
     */
    val isMethod get() = name != "<clinit>" && !isConstructor

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

    /**
     * Load constructor from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载构造方法
     *
     * @param classLoader class loader / 类加载器
     * @return [Constructor]
     */
    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader): Constructor<*> {
        return InstanceUtil.getConstructorInstance(classLoader, this)
    }

    /**
     * Load method from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载方法
     *
     * @param classLoader class loader / 类加载器
     * @return [Method]
     */
    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader): Method {
        return InstanceUtil.getMethodInstance(classLoader, this)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexMethod) return false
        return className == other.className
            && name == other.name
            && paramTypeNames == other.paramTypeNames
            && returnTypeName == other.returnTypeName
    }

    override fun hashCode(): Int {
        return className.hashCode() * 31 +
            name.hashCode() * 31 +
            paramTypeNames.hashCode() * 31 +
            returnTypeName.hashCode()
    }
}