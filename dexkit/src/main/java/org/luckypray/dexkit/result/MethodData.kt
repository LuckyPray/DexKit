@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerMethodMeta
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.DexSignUtil
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class MethodData private constructor(
    bridge: DexKitBridge,
    val id: Int,
    val dexId: Int,
    val classId: Int,
    val modifiers: Int,
    val dexDescriptor: String,
    val returnTypeId: Int,
    val parameterTypeIds: List<Int>
) : BaseData(bridge) {

    companion object {
        internal fun from(bridge: DexKitBridge, methodMeta: InnerMethodMeta) = MethodData(
            bridge,
            methodMeta.id.toInt(),
            methodMeta.dexId.toInt(),
            methodMeta.classId.toInt(),
            methodMeta.accessFlags.toInt(),
            methodMeta.dexDescriptor ?: "",
            methodMeta.returnType.toInt(),
            mutableListOf<Int>().apply {
                for (i in 0 until methodMeta.parameterTypesLength) {
                    add(methodMeta.parameterTypes(i))
                }
            }
        )
    }

    private val classSign: String by lazy {
        dexDescriptor.substringBefore("->")
    }

    private val paramSign: String by lazy {
        dexDescriptor.substringAfter("(").substringBefore(")")
    }

    private val returnTypeSign: String by lazy {
        dexDescriptor.substringAfter(")")
    }

    val className: String by lazy {
        DexSignUtil.getSimpleName(classSign)
    }

    val name: String by lazy {
        dexDescriptor.substringAfter("->").substringBefore("(")
    }

    val paramSignList: List<String> by lazy {
        DexSignUtil.getParamSignList(paramSign)
    }

    val returnTypeName: String by lazy {
        DexSignUtil.getSimpleName(returnTypeSign)
    }

    val isConstructor: Boolean by lazy {
        name == "<init>"
    }

    val isMethod: Boolean by lazy {
        name != "<clinit>" && !isConstructor
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader): Constructor<*> {
        if (!isConstructor) {
            throw IllegalArgumentException("$this not a constructor")
        }
        try {
            var clz = classLoader.loadClass(className)
            do {
                for (constructor in clz.declaredConstructors) {
                    if (dexDescriptor == DexSignUtil.getConstructorSign(constructor)) {
                        return constructor
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Constructor $this not found in $dexDescriptor")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader): Method {
        if (!isMethod) {
            throw IllegalArgumentException("$this not a method")
        }
        try {
            var clz = classLoader.loadClass(className)
            do {
                for (method in clz.declaredMethods) {
                    if (method.name == name && dexDescriptor == DexSignUtil.getMethodSign(method)) {
                        return method
                    }
                }
            } while (clz.superclass.also { clz = it } != null)
            throw NoSuchMethodException("Method $this not found in $dexDescriptor")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("No such method: $this").initCause(e)
        }
    }

    fun getClass(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, classId))).firstOrNull()
    }

    fun getReturnType(): ClassData? {
        return bridge.getClassByIds(longArrayOf(getEncodeId(dexId, returnTypeId))).firstOrNull()
    }

    fun getParameterTypes(): ClassDataList {
        return bridge.getClassByIds(parameterTypeIds.map { getEncodeId(dexId, it) }.toLongArray())
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getMethodAnnotations(getEncodeId(dexId, id))
    }

    fun getParameterAnnotations(): List<List<AnnotationData>> {
        return bridge.getParameterAnnotations(getEncodeId(dexId, id))
    }

    override fun toString(): String {
        return buildString {
            if (modifiers != 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append("$returnTypeName $name(")
            append(paramSignList.joinToString(", "))
            append(") {}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MethodData && other.dexDescriptor == dexDescriptor
    }

    override fun hashCode(): Int {
        return dexDescriptor.hashCode()
    }
}