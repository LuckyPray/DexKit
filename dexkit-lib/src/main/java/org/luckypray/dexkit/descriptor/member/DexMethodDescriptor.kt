@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.descriptor.member

import org.luckypray.dexkit.descriptor.DexDescriptor
import org.luckypray.dexkit.util.DexDescriptorUtil.getClassName
import org.luckypray.dexkit.util.DexDescriptorUtil.getConstructorSignature
import org.luckypray.dexkit.util.DexDescriptorUtil.getMethodSignature
import org.luckypray.dexkit.util.DexDescriptorUtil.getTypeSig
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method

class DexMethodDescriptor : DexDescriptor {

    val declaringClassSig: String
    val name: String
    val parameterTypesSig: String
    val returnTypeSig: String

    val declaringClassName: String
        get() = getClassName(declaringClassSig)

    override val descriptor: String
        get() = "$declaringClassSig->$name$signature"
    override val signature: String
        get() = "($parameterTypesSig)$returnTypeSig"

    val isConstructor
        get() = name == "<init>"
    val isMethod
        get() = name != "<clinit>" && !isConstructor

    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf('(')
        val idx3 = descriptor.indexOf(')')

        if (idx1 == -1 || idx2 == -1 || idx3 == -1) {
            throw IllegalArgumentException("Invalid method descriptor: $descriptor")
        }

        declaringClassSig = descriptor.substring(0, idx1)
        name = descriptor.substring(idx1 + 2, idx2)
        parameterTypesSig = descriptor.substring(idx2 + 1, idx3)
        returnTypeSig = descriptor.substring(idx3 + 1)
    }

    constructor(method: Method) {
        declaringClassSig = getTypeSig(method.declaringClass)
        name = method.name
        parameterTypesSig = method.parameterTypes.joinToString("") { getTypeSig(it) }
        returnTypeSig = getTypeSig(method.returnType)
    }

    constructor(constructor: Constructor<*>) {
        declaringClassSig = getTypeSig(constructor.declaringClass)
        name = "<init>"
        parameterTypesSig = constructor.parameterTypes.joinToString("") { getTypeSig(it) }
        returnTypeSig = "V"
    }

    constructor(clz: String, name: String, methodSignature: String) {
        val idx = methodSignature.indexOf(')')
        if (idx == -1 || methodSignature.first() != '(') {
            throw IllegalArgumentException("Invalid method signature: $methodSignature")
        }
        declaringClassSig = clz
        this.name = name
        parameterTypesSig = methodSignature.substring(1, idx)
        returnTypeSig = methodSignature.substring(idx + 1)
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader): Constructor<*> {
        if (!isConstructor) {
            throw IllegalArgumentException("$this not a constructor")
        }
        try {
            var clz = classLoader.loadClass(declaringClassName)
            do {
                for (constructor in clz.declaredConstructors) {
                    if (signature == getConstructorSignature(constructor)) {
                        return constructor
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Constructor $this not found in $declaringClassSig")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader): Method {
        if (!isMethod) {
            throw IllegalArgumentException("$this not a method")
        }
        try {
            var clz = classLoader.loadClass(declaringClassName)
            do {
                for (method in clz.declaredMethods) {
                    if (method.name == name && signature == getMethodSignature(method)) {
                        return method
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Method $this not found in $declaringClassSig")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    @Throws(NoSuchMethodError::class)
    fun getMemberInstance(classLoader: ClassLoader): Member {
        if (name == "<clinit>") {
            throw NoSuchMethodError("<clinit> method cannot be instantiated: $this")
        }
        return if (isConstructor) {
            getConstructorInstance(classLoader)
        } else {
            getMethodInstance(classLoader)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexMethodDescriptor) return false

        if (declaringClassSig != other.declaringClassSig) return false
        if (name != other.name) return false
        if (parameterTypesSig != other.parameterTypesSig) return false
        if (returnTypeSig != other.returnTypeSig) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaringClassSig.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + parameterTypesSig.hashCode()
        result = 31 * result + returnTypeSig.hashCode()
        return result
    }
}
