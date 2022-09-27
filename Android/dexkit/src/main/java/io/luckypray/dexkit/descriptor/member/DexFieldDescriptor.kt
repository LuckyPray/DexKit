package io.luckypray.dexkit.descriptor.member

import io.luckypray.dexkit.descriptor.DexDescriptor
import io.luckypray.dexkit.descriptor.util.getClassName
import io.luckypray.dexkit.descriptor.util.getTypeSig
import java.lang.reflect.Field
import java.lang.reflect.Member

class DexFieldDescriptor : DexDescriptor {

    val declaringClassSig: String
    val name: String
    val typeSig: String

    val declaringClassName: String
        get() = getClassName(declaringClassSig)

    override val descriptor: String
        get() = "$declaringClassSig->$name:$typeSig"
    override val signature: String
        get() = typeSig

    constructor(descriptor: String) {
        val idx1 = descriptor.indexOf("->")
        val idx2 = descriptor.indexOf(':')

        if (idx1 == -1 || idx2 == -1) {
            throw IllegalArgumentException("Invalid field descriptor: $descriptor")
        }

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

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader): Member {
        try {
            var clz = classLoader.loadClass(declaringClassName)
            do {
                for (field in clz.declaredFields) {
                    if (field.name == name && typeSig == getTypeSig(field.type)) {
                        return field
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchFieldException("Field $this not found in $declaringClassSig")
        } catch (e: ClassNotFoundException) {
            throw NoSuchFieldException("No such field: $this").initCause(e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexFieldDescriptor) return false

        if (declaringClassSig != other.declaringClassSig) return false
        if (name != other.name) return false
        if (typeSig != other.typeSig) return false

        return true
    }

    override fun hashCode(): Int {
        var result = declaringClassSig.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeSig.hashCode()
        return result
    }
}
