@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerClassMeta
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.query.FieldDataList
import org.luckypray.dexkit.query.MethodDataList
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil
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

    val className: String by lazy {
        DexSignUtil.getSimpleName(dexDescriptor)
    }

    val name get() = className

    val classSimpleName : String by lazy {
        className.substringAfterLast(".")
    }

    fun getSuperClass(): ClassData? {
        superClassId ?: return null
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, id))).firstOrNull()
    }

    fun getInterfaces(): ClassDataList {
        return bridge.getClassByIds(interfaceIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    fun getMethods(): MethodDataList {
        return bridge.getMethodByIds(methodIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    fun getFields(): FieldDataList {
        return bridge.getFieldByIds(fieldIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getClassAnnotations(getEncodeId(dexId, id))
    }

    @Throws(ClassNotFoundException::class)
    fun getInstance(classLoader: ClassLoader): Class<*> {
        return getClassInstance(classLoader, className)
    }

    @Throws(ClassNotFoundException::class)
    fun getSuperClassInstance(classLoader: ClassLoader): Class<*>? {
        return getSuperClass()?.getInstance(classLoader)
    }

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