package io.luckypray.dexkit

import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexFieldDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import java.io.Closeable
import java.net.URL

class DexKitBridge private constructor(apkPath: String) : Closeable {

    private var token: Long = 0L

    init {
        token = nativeInitDexKit(apkPath)
    }

    val isValid
        get() = token != 0L

    override fun close() {
        nativeRelease(token)
    }

    /**
     * Get all parsed dex counts.
     */
    fun getDexNum(): Int {
        return nativeGetDexNum(token)
    }

    fun batchFindClassesUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> {
        return nativeBatchFindClassesUsingStrings(token, map, advancedMatch, dexPriority)
            .mapValues { it.value.map { DexClassDescriptor(it) } }
    }

    fun batchFindClassesUsingArrayStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> {
        return batchFindClassesUsingStrings(
            map.mapValues { it.value.toList() },
            advancedMatch,
            dexPriority
        )
    }

    fun batchFindMethodsUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> {
        return nativeBatchFindMethodsUsingStrings(token, map, advancedMatch, dexPriority)
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    fun batchFindMethodsUsingArrayStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> {
        return batchFindMethodsUsingStrings(
            map.mapValues { it.value.toList() },
            advancedMatch,
            dexPriority
        )
    }

    fun findMethodBeInvoked(
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParameterTypes: Array<String>? = null,
        callerMethodDeclareClass: String = "",
        callerMethodName: String = "",
        callerMethodReturnType: String = "",
        callerMethodParameterTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodBeInvoked(
            token,
            methodDescriptor,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParameterTypes,
            callerMethodDeclareClass,
            callerMethodName,
            callerMethodReturnType,
            callerMethodParameterTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    fun findMethodInvoking(
        methodDescriptor: String,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParameterTypes: Array<String>? = null,
        beCalledMethodDeclareClass: String = "",
        beCalledMethodName: String = "",
        beCalledMethodReturnType: String = "",
        beCalledMethodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Map<DexMethodDescriptor, List<DexMethodDescriptor>> {
        return nativeFindMethodInvoking(
            token,
            methodDescriptor,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParameterTypes,
            beCalledMethodDeclareClass,
            beCalledMethodName,
            beCalledMethodReturnType,
            beCalledMethodParamTypes,
            dexPriority
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    fun findMethodUsingField(
        fieldDescriptor: String,
        fieldDeclareClass: String,
        fieldName: String,
        fieldType: String,
        usedFlags: Int = FLAG_USING,
        callerMethodDeclareClass: String = "",
        callerMethodName: String = "",
        callerMethodReturnType: String = "",
        callerMethodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Map<DexMethodDescriptor, List<DexFieldDescriptor>> {
        return nativeFindMethodUsingField(
            token,
            fieldDescriptor,
            fieldDeclareClass,
            fieldName,
            fieldType,
            usedFlags,
            callerMethodDeclareClass,
            callerMethodName,
            callerMethodReturnType,
            callerMethodParamTypes,
            dexPriority
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexFieldDescriptor(it) } }
    }

    fun findMethodUsingString(
        usingString: String,
        advancedMatch: Boolean = true,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingString(
            token,
            usingString,
            advancedMatch,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    fun findMethod(
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethod(
            token,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = null
    ): List<DexClassDescriptor> {
        return nativeFindSubClasses(token, parentClass, dexPriority)
            .map { DexClassDescriptor(it) }
    }

    fun findMethodOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodOpPrefixSeq(
            token,
            opPrefixSeq,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    companion object {
        const val FLAG_GETTING = 0x00000001
        const val FLAG_SETTING = 0x00000002
        const val FLAG_USING = FLAG_GETTING or FLAG_SETTING

        @JvmStatic
        fun create(apkPath: String): DexKitBridge? {
            val helper = DexKitBridge(apkPath)
            return if (helper.isValid) helper else null
        }

        @JvmStatic
        fun create(loader: ClassLoader): DexKitBridge? {
            loader.loadClass("java.lang.ClassLoader").declaredMethods.first {
                it.name == "findResource"
                    && it.parameterTypes.size == 1
                    && it.parameterTypes[0] == String::class.java
                    && it.returnType == URL::class.java
            }.let { method ->
                method.isAccessible = true
                val url = method.invoke(loader, "AndroidManifest.xml") as URL
                url.path.substring(5, url.path.length - 21).let {
                    val helper = DexKitBridge(it)
                    return if (helper.isValid) helper else null
                }
            }
        }

        @JvmStatic
        private external fun nativeInitDexKit(apkPath: String): Long

        @JvmStatic
        private external fun nativeGetDexNum(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeRelease(nativePtr: Long): Unit

        @JvmStatic
        private external fun nativeBatchFindClassesUsingStrings(
            nativePtr: Long,
            map: Map<String, Iterable<String>>,
            advancedMatch: Boolean,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeBatchFindMethodsUsingStrings(
            nativePtr: Long,
            map: Map<String, Iterable<String>>,
            advancedMatch: Boolean,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodBeInvoked(
            nativePtr: Long,
            methodDescriptor: String,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParameterTypes: Array<String>?,
            callerMethodDeclareClass: String,
            callerMethodName: String,
            callerMethodReturnType: String,
            callerMethodParameterTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethodInvoking(
            nativePtr: Long,
            methodDescriptor: String,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParameterTypes: Array<String>?,
            beCalledMethodDeclareClass: String,
            beCalledMethodName: String,
            beCalledMethodReturnType: String,
            beCalledMethodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodUsingField(
            nativePtr: Long,
            fieldDescriptor: String,
            fieldDeclareClass: String,
            fieldName: String,
            fieldType: String,
            usedFlags: Int = FLAG_USING,
            callerMethodDeclareClass: String,
            callerMethodName: String,
            callerMethodReturnType: String,
            callerMethodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodUsingString(
            nativePtr: Long,
            usingString: String,
            advancedMatch: Boolean,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethod(
            nativePtr: Long,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindSubClasses(
            nativePtr: Long,
            parentClass: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethodOpPrefixSeq(
            nativePtr: Long,
            opPrefixSeq: IntArray,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>
    }

    protected fun finalize() {
        close()
    }
}