@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil
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

    private val classSign : String by lazy {
        dexDescriptor.substringBefore("->")
    }

    private val typeSign : String by lazy {
        dexDescriptor.substringAfter(":")
    }

    val className: String by lazy {
        DexSignUtil.getSimpleName(classSign)
    }

    val fieldName : String by lazy {
        dexDescriptor.substringAfter("->").substringBefore(":")
    }

    val typeName : String by lazy {
        DexSignUtil.getSimpleName(typeSign)
    }

    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader): Field {
        try {
            var clz = classLoader.loadClass(className)
            do {
                for (field in clz.declaredFields) {
                    if (field.name == fieldName && typeSign == DexSignUtil.getTypeSign(field.type)) {
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
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, classId))).firstOrNull()
    }

    fun getType(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, typeId))).firstOrNull()
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getFieldAnnotations(getEncodeId(dexId, id))
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