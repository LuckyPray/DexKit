@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil
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

    val classSign : String by lazy {
        dexDescriptor.substringBefore("->")
    }

    val typeSign : String by lazy {
        dexDescriptor.substringAfter(":")
    }

    val className: String by lazy {
        DexSignUtil.getSimpleName(classSign)
    }

    val fieldName : String by lazy {
        dexDescriptor.substringAfter("->").substringBefore(":")
    }

    val name get() = fieldName

    val typeName : String by lazy {
        DexSignUtil.getSimpleName(typeSign)
    }

    fun getClass(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, classId))).firstOrNull()
    }

    fun getType(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, typeId))).firstOrNull()
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getFieldAnnotations(getEncodeId(dexId, id))
    }

    fun getMethods(): List<MethodData> {
        return bridge.fieldGetMethods(getEncodeId(dexId, id))
    }

    fun putMethods(): List<MethodData> {
        return bridge.fieldPutMethods(getEncodeId(dexId, id))
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
            append(fieldName)
            append(";")
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