@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerFieldMeta
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.InstanceUtil
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

    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, fieldMeta: InnerFieldMeta) = FieldData(
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

    /**
     * field type sign
     * ----------------
     * 字段类型签名
     */
    val typeSign get() = dexField.typeSign

    /**
     * field declaring class name
     * ----------------
     * 定义字段的类名
     */
    val className get() = dexField.className

    /**
     * field name
     * ----------------
     * 字段名
     */
    val fieldName get() = dexField.name

    /**
     * @see fieldName
     */
    val name get() = dexField.name

    /**
     * field type name
     * ----------------
     * 字段类型名
     */
    val typeName get() = dexField.typeName

    /**
     * get declared class' [ClassData]
     * ----------------
     * 获取定义字段的类的 [ClassData]
     */
    fun getClass(): ClassData {
        return bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, classId))).first()
    }

    /**
     * get field type's [ClassData]
     * ----------------
     * 获取字段类型的 [ClassData]
     */
    fun getType(): ClassData {
        return bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, typeId))).first()
    }

    /**
     * Get declared annotations (not include dalvik system annotations)
     * ----------------
     * 获取标注的注解列表（不包含 dalvik 系统注解）
     */
    fun getAnnotations(): List<AnnotationData> {
        return bridge.getFieldAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Using smali `iput-*`、`sput-*` instructions to read this field's methods
     * ----------------
     * 使用 smali `iget-*`、`sget-*` 指令读取字段的方法
     */
    fun getReadMethods(): List<MethodData> {
        return bridge.readFieldMethods(getEncodeId(dexId, id))
    }

    /**
     * Using smali `iput-*`、`sput-*` instructions to write this field's methods
     * ----------------
     * 使用 smali `iput-*`、`sput-*` 指令写入字段的方法
     */
    fun getWriteMethods(): List<MethodData> {
        return bridge.writeFieldMethods(getEncodeId(dexId, id))
    }

    /**
     * Load declared class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载定义字段的类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, className)
    }

    /**
     * Load field's type from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 加载字段类型
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getTypeInstance(classLoader: ClassLoader): Class<*> {
        return InstanceUtil.getClassInstance(classLoader, typeName)
    }

    /**
     * Get field's [Field] from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 获取字段对应的 [Field]
     *
     * @param classLoader class loader / 类加载器
     * @return [Field]
     */
    @Throws(NoSuchFieldException::class)
    fun getFieldInstance(classLoader: ClassLoader) = dexField.getFieldInstance(classLoader)

    /**
     * Convert to [DexField]
     * ----------------
     * 转换为 [DexField]
     *
     * @return [DexField]
     */
    fun toDexField(): DexField {
        return dexField
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
            append(name)
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