package io.luckypray.dexkit

import java.lang.reflect.Field
import java.lang.reflect.Member

class DexFieldDescriptor {

    val declaringClassSig: String
    val name: String
    val typeSig: String

    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf(':')

        declaringClassSig = descriptor.substring(0, idx1)
        name = descriptor.substring(idx1 + 2, idx2)
        typeSig = descriptor.substring(idx2 + 1)
    }

    constructor(declaringClassSig: String, name: String, typeSig: String) {
        this.declaringClassSig = declaringClassSig
        this.name = name
        this.typeSig = typeSig
    }

    constructor(field: Field) {
        this.declaringClassSig = getTypeSig(field.declaringClass)
        this.name = field.name
        this.typeSig = getTypeSig(field.type)
    }

    fun getFieldInstance(classLoader: ClassLoader): Member {
        try {
            var clz = classLoader.loadClass(getDeclareClassName())
            do {
                for (field in clz.declaredFields) {
                    if (field.name == name && typeSig == getTypeSig(field.type)) {
                        return field
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchFieldException("Field $this not found in $declaringClassSig")
        } catch(e: ClassNotFoundException) {
            throw NoSuchFieldException("No such field: $this").initCause(e)
        }
    }

    fun getDeclareClassName(): String {
        return getClassName(declaringClassSig)
    }

    override fun toString(): String {
        return "$declaringClassSig->$name:$typeSig"
    }

}