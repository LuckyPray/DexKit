@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerClassMeta
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexDescriptorUtil.getClassName

class ClassData private constructor(
    bridge: DexKitBridge,
    val id: Int,
    val dexId: Int,
    val sourceFile: String,
    val modifiers: Int,
    val dexDescriptor: String,
    val superClassId: Int?,
    val interfaceIds: List<Int>,
    val methodIds: List<Int>,
    val fieldIds: List<Int>,
): BaseData(bridge) {

    companion object {
        internal fun from(bridge: DexKitBridge, classMeta: InnerClassMeta) = ClassData(
            bridge,
            classMeta.id.toInt(),
            classMeta.dexId.toInt(),
            classMeta.sourceFile ?: "",
            classMeta.accessFlags.toInt(),
            classMeta.dexDescriptor ?: "",
            classMeta.superClass.toInt().let { if (it == -1) null else it },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.interfacesLength) {
                    add(classMeta.interfaces(i))
                }
            },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.methodsLength) {
                    add(classMeta.methods(i))
                }
            },
            mutableListOf<Int>().apply {
                for (i in 0 until classMeta.fieldsLength) {
                    add(classMeta.fields(i))
                }
            },
        )
    }

    val name: String by lazy {
        getClassName(dexDescriptor)
    }

    val canonicalName get() = name

    val simpleName : String by lazy {
        name.substringAfterLast(".")
    }

    fun getSuperClass(): ClassData? {
        superClassId ?: return null
        return bridge.getClassByIds(intArrayOf(superClassId)).firstOrNull()
    }

    fun getInterfaces(): List<ClassData> {
        return bridge.getClassByIds(interfaceIds.toIntArray())
    }

    fun getMethods(): List<MethodData> {
        return bridge.getMethodByIds(methodIds.toIntArray())
    }

    fun getFieldsMeta(): List<FieldData> {
        return bridge.getFieldByIds(fieldIds.toIntArray())
    }

    override fun toString(): String {
        return dexDescriptor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ClassData && dexDescriptor == other.dexDescriptor
    }

    override fun hashCode(): Int {
        return dexDescriptor.hashCode()
    }
}