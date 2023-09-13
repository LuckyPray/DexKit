@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.result

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.InnerMethodMeta
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.query.wrap.DexMethod
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.util.OpCodeUtil
import org.luckypray.dexkit.util.getClassInstance
import org.luckypray.dexkit.util.getMethodInstance
import org.luckypray.dexkit.util.getConstructorInstance
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

    val dexMethod by lazy {
        DexMethod(dexDescriptor)
    }

    val methodSign get() = dexDescriptor.substring(dexDescriptor.indexOf("("))

    val className get() = dexMethod.declaredClass

    val methodName get() = dexMethod.name

    val name get() = methodName

    val paramTypeNames get() = dexMethod.paramTypes

    val returnTypeName get() = dexMethod.returnType

    val isConstructor: Boolean by lazy {
        methodName == "<init>"
    }

    val isMethod: Boolean by lazy {
        methodName != "<clinit>" && !isConstructor
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

    fun getParameterNames(): List<String?>? {
        return bridge.getParameterNames(getEncodeId(dexId, id))
    }

    fun getAnnotations(): List<AnnotationData> {
        return bridge.getMethodAnnotations(getEncodeId(dexId, id))
    }

    fun getParameterAnnotations(): List<List<AnnotationData>> {
        return bridge.getParameterAnnotations(getEncodeId(dexId, id))
    }

    fun getOpCodes(): List<Int> {
        return bridge.getMethodOpCodes(getEncodeId(dexId, id))
    }

    fun getOpNames(): List<String> {
        return getOpCodes().map { OpCodeUtil.getOpFormat(it) }
    }

    fun getMethodCallers(): List<MethodData> {
        return bridge.getCallMethods(getEncodeId(dexId, id))
    }

    fun getInvokeMethods(): List<MethodData> {
        return bridge.getInvokeMethods(getEncodeId(dexId, id))
    }

    fun getUsingStrings(): List<String> {
        return bridge.getMethodUsingStrings(getEncodeId(dexId, id))
    }

    @Throws(ClassNotFoundException::class)
    fun getClassInstance(classLoader: ClassLoader): Class<*> {
        return getClassInstance(classLoader, className)
    }

    @Throws(ClassNotFoundException::class)
    fun getReturnTypeInstance(classLoader: ClassLoader): Class<*> {
        return getClassInstance(classLoader, returnTypeName)
    }

    @Throws(NoSuchMethodException::class)
    fun getConstructorInstance(classLoader: ClassLoader): Constructor<*> {
        return getConstructorInstance(classLoader, this)
    }

    @Throws(NoSuchMethodException::class)
    fun getMethodInstance(classLoader: ClassLoader): Method {
        return getMethodInstance(classLoader, this)
    }

    override fun toString(): String {
        return buildString {
            if (modifiers != 0) {
                append("${Modifier.toString(modifiers)} ")
            }
            append(returnTypeName)
            append(" ")
            append(className)
            append(".")
            append(methodName)
            append(paramTypeNames.joinToString(", "))
            append(")")
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