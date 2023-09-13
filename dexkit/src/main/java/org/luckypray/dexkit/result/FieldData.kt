@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.query.wrap.DexField
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.getClassInstance
import org.luckypray.dexkit.util.getFieldInstance
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class FieldData private constructor(
    bridge: DexKitBridge,
    val id: Int,
    val dexId: Int,
    val classId: Int,
    val modifiers: Int,
    val dexDescriptor: String,
    val typeId: Int
): BaseData(bridge) {

    companion object {
        internal fun from(bridge: DexKitBridge, fieldMeta: InnerFieldMeta) = FieldData(
            bridge,
            fieldMeta.id.toInt(),
            fieldMeta.dexId.toInt(),
            fieldMeta.classId.toInt(),
            fieldMeta.accessFlags.toInt(),
            fieldMeta.dexDescriptor!!,
            fieldMeta.typeId.toInt()
        )
    }

    private val dexField by lazy {
        DexField(dexDescriptor)
    }

    val typeSign get() = dexDescriptor.substringAfter(":")

    val className get() = dexField.declaredClass

    val fieldName get() = dexField.name

    val name get() = fieldName

    val typeName get() = dexField.type

    fun getClass(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, classId))).firstOrNull()
    }

    fun getType(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, typeId))).firstOrNull()
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getFieldAnnotations(getEncodeId(dexId, id))
    }

    fun getReadMethods(): List<MethodData> {
        return bridge.readFieldMethods(getEncodeId(dexId, id))
    }

    fun getWriteMethods(): List<MethodData> {
        return bridge.writeFieldMethods(getEncodeId(dexId, id))
    }

    @Throws(ClassNotFoundException::class)
    fun getTypeInstance(classLoader: ClassLoader): Class<*> {
        return getClassInstance(classLoader, className)
    }

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader): Field {
        return getFieldInstance(classLoader, this)
    }


    override fun toString(): String {
        return buildString {
            if (modifiers > 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append(typeName)
            append(" ")
            append(className)
            append(".")
            append(fieldName)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FieldData && other.dexDescriptor == dexDescriptor
    }

    override fun hashCode(): Int {
        return dexDescriptor.hashCode()
    }
}