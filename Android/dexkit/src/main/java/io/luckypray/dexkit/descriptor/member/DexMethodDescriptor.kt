package io.luckypray.dexkit.descriptor.member

import io.luckypray.dexkit.descriptor.*
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method

class DexMethodDescriptor: DexDescriptor {

    val declaringClassSig: String
    val name: String
    val parameterTypesSig: String
    val returnTypeSig: String
    val methodTypeSig: String

    override val descriptor: String
        get() = "$declaringClassSig->$name$methodTypeSig"

    val isConstructor
        get() = name == "<init>"
    val isMethod
        get() = name != "<clinit>" && !isConstructor

    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf('(')
        val idx3 = descriptor.indexOf(')')

        declaringClassSig = descriptor.substring(0, idx1)
        name = descriptor.substring(idx1 + 2, idx2)
        methodTypeSig = descriptor.substring(idx2)
        parameterTypesSig = descriptor.substring(idx2 + 1, idx3)
        returnTypeSig = descriptor.substring(idx2 + 1)
    }

    constructor(method: Method) {
        declaringClassSig = getTypeSig(method.declaringClass)
        name = method.name
        parameterTypesSig = method.parameterTypes.joinToString("") { getTypeSig(it) }
        returnTypeSig = getTypeSig(method.returnType)
        methodTypeSig = "($parameterTypesSig)$returnTypeSig"
    }

    constructor(constructor: Constructor<*>) {
        declaringClassSig = getTypeSig(constructor.declaringClass)
        name = "<init>"
        parameterTypesSig = constructor.parameterTypes.joinToString("") { getTypeSig(it) }
        returnTypeSig = "V"
        methodTypeSig = "($parameterTypesSig)$returnTypeSig"
    }

    constructor(clz: String, name: String, methodTypeSig: String) {
        declaringClassSig = clz
        this.name = name
        this.methodTypeSig = methodTypeSig
        val idx = methodTypeSig.indexOf(')')
        parameterTypesSig = methodTypeSig.substring(1, idx)
        returnTypeSig = methodTypeSig.substring(idx + 1)
    }

    fun getConstructor(classLoader: ClassLoader): Constructor<*> {
        if (!isConstructor) {
            throw IllegalArgumentException("$this not a constructor")
        }
        try {
            var clz = classLoader.loadClass(getDeclareClassName())
            do {
                for (constructor in clz.declaredConstructors) {
                    if (methodTypeSig == getConstructorTypeSig(constructor)) {
                        return constructor
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Constructor $this not found in $declaringClassSig")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    fun getMethod(classLoader: ClassLoader): Method {
        if (!isMethod) {
            throw IllegalArgumentException("$this not a method")
        }
        try {
            var clz = classLoader.loadClass(getDeclareClassName())
            do {
                for (method in clz.declaredMethods) {
                    if (method.name == name && methodTypeSig == getMethodTypeSig(method)) {
                        return method
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Method $this not found in $declaringClassSig")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    fun getMemberInstance(classLoader: ClassLoader): Member {
        if (name == "<clinit>") {
            throw NoSuchMethodException("<clinit> method cannot be instantiated: $this")
        }
        return if (isConstructor) {
            getConstructor(classLoader)
        } else {
            getMethod(classLoader)
        }
    }

    fun getDeclareClassName(): String {
        return getClassName(declaringClassSig)
    }

}