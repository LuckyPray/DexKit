package io.luckypray.dexkit.descriptor.member

import io.luckypray.dexkit.descriptor.DexDescriptor
import io.luckypray.dexkit.descriptor.getClassName

class DexClassDescriptor : DexDescriptor {

    val typeSig: String

    val name: String
        get() = getClassName(typeSig)
    val simpleName: String
        get() = name.substring(name.lastIndexOf('.') + 1)

    override val descriptor: String
        get() = typeSig
    override val signature: String
        get() = typeSig

    constructor(descriptor: String) {
        if (descriptor.first() != 'L' || descriptor.last() != ';') {
            throw IllegalArgumentException("Invalid class descriptor: $descriptor")
        }
        typeSig = descriptor
    }

    constructor(clazz: Class<*>) {
        typeSig = "L${clazz.name.replace(".", "/")};"
    }

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return classLoader.loadClass(name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DexClassDescriptor) return false

        if (typeSig != other.typeSig) return false

        return true
    }

    override fun hashCode(): Int {
        return typeSig.hashCode()
    }
}