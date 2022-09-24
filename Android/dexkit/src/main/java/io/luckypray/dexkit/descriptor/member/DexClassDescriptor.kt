package io.luckypray.dexkit.descriptor.member

import io.luckypray.dexkit.descriptor.DexDescriptor

class DexClassDescriptor: DexDescriptor {

    val signature: String

    override val descriptor: String
        get() = signature
    val name: String
        get() = signature.substring(1, signature.length - 1).replace('/', '.')
    val simpleName: String
        get() = name.substring(name.lastIndexOf('.') + 1)

    constructor(descriptor: String) {
        signature = descriptor
    }

    constructor(clazz: Class<*>) {
        signature = "L${clazz.name.replace(".", "/")};"
    }

    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return classLoader.loadClass(name)
    }
}