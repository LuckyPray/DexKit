@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.alias.InnerFieldMeta
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexDescriptorUtil.getClassName
import org.luckypray.dexkit.util.DexDescriptorUtil.getTypeSig
import java.lang.reflect.Field

class FieldData private constructor(
    private val bridge: DexKitBridge,
    val id: Int,
    val dexId: Int,
    val classId: Int,
    val modifiers: Int,
    val dexDescriptor: String,
    val typeId: Int
): BaseData(bridge) {

    internal constructor(bridge: DexKitBridge, fieldMeta: InnerFieldMeta) : this(
        bridge,
        fieldMeta.id.toInt(),
        fieldMeta.dexId.toInt(),
        fieldMeta.classId.toInt(),
        fieldMeta.accessFlags.toInt(),
        fieldMeta.dexDescriptor ?: "",
        fieldMeta.typeId.toInt()
    )

    val classDescriptor : String by lazy {
        dexDescriptor.substringBefore("->")
    }

    val className: String by lazy {
        getClassName(classDescriptor)
    }

    val name : String by lazy {
        dexDescriptor.substringAfter("->").substringBefore(":")
    }

    val typeDescriptor : String by lazy {
        dexDescriptor.substringAfter(":")
    }

    val typeName : String by lazy {
        getClassName(typeDescriptor)
    }

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader): Field {
        try {
            var clz = classLoader.loadClass(className)
            do {
                for (field in clz.declaredFields) {
                    if (field.name == name && typeDescriptor == getTypeSig(field.type)) {
                        return field
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchFieldException("Field $this not found in $className")
        } catch (e: ClassNotFoundException) {
            throw NoSuchFieldException("No such field: $this").initCause(e)
        }
    }

    fun getClass(): ClassData? {
        return bridge.getClassByIds(intArrayOf(classId)).firstOrNull()
    }

    fun getType(): ClassData? {
        return bridge.getClassByIds(intArrayOf(typeId)).firstOrNull()
    }

    override fun toString(): String {
        return dexDescriptor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FieldData && other.dexDescriptor == dexDescriptor
    }

    override fun hashCode(): Int {
        return dexDescriptor.hashCode()
    }
}