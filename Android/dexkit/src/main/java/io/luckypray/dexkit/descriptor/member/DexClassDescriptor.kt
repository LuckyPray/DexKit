package io.luckypray.dexkit.descriptor.member

import io.luckypray.dexkit.descriptor.DexDescriptor

class DexClassDescriptor: DexDescriptor {

    val classSig: String

    override val descriptor: String
        get() = classSig
    val className: String
        get() = classSig.substring(1, classSig.length - 1).replace('/', '.')
    val simpleName: String
        get() = className.substring(className.lastIndexOf('.') + 1)

    constructor(descriptor: String) {
        classSig = descriptor
    }

    constructor(clazz: Class<*>) {
        classSig = "L${clazz.name.replace(".", "/")};"
    }

    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return classLoader.loadClass(className)
    }
}