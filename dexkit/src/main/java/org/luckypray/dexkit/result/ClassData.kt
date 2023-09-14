@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerClassMeta
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.query.FieldDataList
import org.luckypray.dexkit.query.MethodDataList
import org.luckypray.dexkit.query.wrap.DexType
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.getClassInstance
import java.lang.reflect.Modifier
import kotlin.jvm.Throws

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

    internal companion object `-Companion` {
        fun from(bridge: DexKitBridge, classMeta: InnerClassMeta) = ClassData(
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

    private val dexType by lazy {
        DexType(dexDescriptor)
    }

    /**
     * Full class name
     * ----------------
     * 完整类名
     *
     *     e.g. java.lang.String
     */
    val className get() = dexType.typeName

    /**
     * @see className
     */
    val name get() = dexType.typeName

    /**
     * Get super's [ClassData], if super class not defined in dex, return null
     * ----------------
     * 获取父类的 [ClassData]，如果父类未在 dex 中定义，返回 null
     */
    fun getSuperClass(): ClassData? {
        superClassId ?: return null
        return bridge.getTypeByIds(longArrayOf(getEncodeId(dexId, superClassId))).firstOrNull()
    }

    /**
     * Get implemented interfaces
     * ----------------
     * 获取实现接口列表
     */
    fun getInterfaces(): ClassDataList {
        return bridge.getTypeByIds(interfaceIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get implemented interfaces count
     * ----------------
     * 获取实现接口的数量
     */
    fun getInterfaceCount(): Int {
        return interfaceIds.size
    }

    /**
     * Get declared methods (include static block: `<clinit>`, and constructor: `<init>`)
     * ----------------
     * 获取定义的方法列表（包含 静态代码块: `<clinit>`，以及构造函数: `<init>`）
     */
    fun getMethods(): MethodDataList {
        return bridge.getMethodByIds(methodIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get declared methods count
     * ----------------
     * 获取定义的方法数量
     */
    fun getMethodCount(): Int {
        return methodIds.size
    }

    /**
     * Get declared fields
     * ----------------
     * 获取定义的字段列表
     */
    fun getFields(): FieldDataList {
        return bridge.getFieldByIds(fieldIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    /**
     * Get declared fields count
     * ----------------
     * 获取定义的字段数量
     */
    fun getFieldCount(): Int {
        return fieldIds.size
    }

    /**
     * Get declared annotations (not include dalvik system annotations)
     * ----------------
     * 获取标注的注解列表（不包含 dalvik 系统注解）
     */
    fun getAnnotations(): List<AnnotationData> {
        return bridge.getClassAnnotations(getEncodeId(dexId, id))
    }

    /**
     * Load this class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载此类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getInstance(classLoader: ClassLoader): Class<*> {
        return getClassInstance(classLoader, className)
    }

    /**
     * Load super class from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载父类
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getSuperClassInstance(classLoader: ClassLoader): Class<*>? {
        return getSuperClass()?.getInstance(classLoader)
    }

    /**
     * Load implemented interfaces from [ClassLoader]
     * ----------------
     * 从 [ClassLoader] 中加载实现的接口
     *
     * @param classLoader class loader / 类加载器
     * @return [Class]
     */
    @Throws(ClassNotFoundException::class)
    fun getInterfaceInstances(classLoader: ClassLoader): List<Class<*>> {
        return getInterfaces().map { getClassInstance(classLoader, it.className) }
    }

    override fun toString(): String {
        return buildString {
            if (modifiers > 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append("class $className")
            getSuperClass()?.let {
                append(" extends ")
                append(it.className)
            }
            if (getInterfaces().size > 0) {
                append(" implements ")
                append(getInterfaces().joinToString(", ") { it.className })
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ClassData && dexDescriptor == other.dexDescriptor
    }

    override fun hashCode(): Int {
        return dexDescriptor.hashCode()
    }
}