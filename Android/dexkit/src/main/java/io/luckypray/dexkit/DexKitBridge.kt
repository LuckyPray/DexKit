package io.luckypray.dexkit

import io.luckypray.dexkit.annotations.DexKitExperimentalApi
import io.luckypray.dexkit.builder.BatchFindArgs
import io.luckypray.dexkit.builder.ClassUsingAnnotationArgs
import io.luckypray.dexkit.builder.FieldUsingAnnotationArgs
import io.luckypray.dexkit.builder.FindMethodArgs
import io.luckypray.dexkit.builder.MethodOpcodeArgs
import io.luckypray.dexkit.builder.MethodCallerArgs
import io.luckypray.dexkit.builder.MethodInvokingArgs
import io.luckypray.dexkit.builder.MethodUsingAnnotationArgs
import io.luckypray.dexkit.builder.MethodUsingFieldArgs
import io.luckypray.dexkit.builder.MethodUsingStringArgs
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexFieldDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.util.getOpFormat
import java.io.Closeable

class DexKitBridge : Closeable {

    private var token: Long = 0L

    private constructor(apkPath: String) {
        token = nativeInitDexKit(apkPath)
    }

    private constructor(classLoader: ClassLoader, useMemoryDexFile: Boolean) {
        token = nativeInitDexKitByClassLoader(classLoader, useMemoryDexFile)
    }

    /**
     * DexKit is valid only when token is not 0
     */
    val isValid
        get() = token != 0L

    /**
     * release native resource
     */
    @Synchronized
    override fun close() {
        if (isValid) {
            nativeRelease(token)
            token = 0L
        }
    }

    /**
     * set DexKit work thread number
     *
     * @param [num] work thread number
     */
    fun setThreadNum(num: Int) {
        nativeSetThreadNum(token, num)
    }

    /**
     * Get all parsed dex counts.
     *
     * @return The number of dex parsed by DexKit
     */
    fun getDexNum(): Int {
        return nativeGetDexNum(token)
    }

    /**
     * write all dex file to [outPath]
     *
     * @param [outPath] dex file output path
     *
     * @since 1.1.0
     */
    fun exportDexFile(outPath: String) {
        nativeExportDexFile(token, outPath)
    }

    /**
     * find used all matched keywords in class (all methods of this class)
     *
     * @param [args] search builder by [BatchFindArgs]
     *
     * @since 1.1.0
     */
    fun batchFindClassesUsingStrings(
        args: BatchFindArgs
    ): Map<String, List<DexClassDescriptor>> {
        return nativeBatchFindClassesUsingStrings(token, args.queryMap, args.advancedMatch, null)
            .mapValues { it.value.map { DexClassDescriptor(it) } }
    }

    /**
     * @suppress use [batchFindClassesUsingStrings] instead
     * @see [batchFindClassesUsingStrings]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun batchFindClassesUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> {
        return nativeBatchFindClassesUsingStrings(token, map, advancedMatch, dexPriority)
            .mapValues { it.value.map { DexClassDescriptor(it) } }
    }

    /**
     * @suppress use [batchFindClassesUsingStrings] instead
     * @see [batchFindClassesUsingStrings]
     */
    @Deprecated("full-argument functions have been deprecated")
    @JvmName("batchFindClassesUsingArrayStrings")
    fun batchFindClassesUsingStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> {
        return nativeBatchFindClassesUsingStrings(
            token,
            map.mapValues { it.value.toList() },
            advancedMatch,
            dexPriority
        ).mapValues { it.value.map { DexClassDescriptor(it) } }
    }

    /**
     * find used all matched keywords in method.
     *
     * @param [args] search builder by [BatchFindArgs]
     *
     * @since 1.1.0
     */
    fun batchFindMethodsUsingStrings(
        args: BatchFindArgs
    ): Map<String, List<DexMethodDescriptor>> {
        return nativeBatchFindMethodsUsingStrings(token, args.queryMap, args.advancedMatch, null)
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    /**
     * @suppress use [batchFindMethodsUsingStrings] instead
     * @see [batchFindMethodsUsingStrings]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun batchFindMethodsUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> {
        return nativeBatchFindMethodsUsingStrings(token, map, advancedMatch, dexPriority)
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    /**
     * @suppress use [batchFindMethodsUsingStrings] instead
     * @see [batchFindMethodsUsingStrings]
     */
    @Deprecated("full-argument functions have been deprecated")
    @JvmName("batchFindMethodsUsingArrayStrings")
    fun batchFindMethodsUsingStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> {
        return nativeBatchFindMethodsUsingStrings(
            token,
            map.mapValues { it.value.toList() },
            advancedMatch,
            dexPriority
        ).mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    /**
     * find caller for specified method.
     *
     * @param [args] search builder by [MethodCallerArgs]
     *
     * @since 1.1.0
     */
    fun findMethodCaller(
        args: MethodCallerArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethodCaller(
            token,
            args.methodDescriptor,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParameterTypes,
            args.callerMethodDeclareClass,
            args.callerMethodName,
            args.callerMethodReturnType,
            args.callerMethodParameterTypes,
            args.uniqueResult,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethodCaller] instead
     * @see [findMethodCaller]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodCaller(
        methodDescriptor: String,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParameterTypes: Array<String>? = null,
        callerMethodDeclareClass: String = "",
        callerMethodName: String = "",
        callerMethodReturnType: String = "",
        callerMethodParameterTypes: Array<String>? = null,
        uniqueResult: Boolean = true,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodCaller(
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
            uniqueResult,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * find the specified method's invoking list.
     *
     * @param [args] search builder by [MethodInvokingArgs]
     * @return Aggregate according to caller
     *
     *     key: caller method descriptor
     *     value: list of invoked method descriptor
     *
     * @since 1.1.0
     */
    fun findMethodInvoking(
        args: MethodInvokingArgs
    ): Map<DexMethodDescriptor, List<DexMethodDescriptor>> {
        return nativeFindMethodInvoking(
            token,
            args.methodDescriptor,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParameterTypes,
            args.beInvokedMethodDeclareClass,
            args.beInvokedMethodName,
            args.beInvokedMethodReturnType,
            args.beInvokedMethodParamTypes,
            args.uniqueResult,
            null
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    /**
     * @suppress use [findMethodInvoking] instead
     * @see [findMethodInvoking]
     */
    @Deprecated("full-argument functions have been deprecated")
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
        uniqueResult: Boolean = true,
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
            uniqueResult,
            dexPriority
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexMethodDescriptor(it) } }
    }

    /**
     * find method getting specified field.
     *
     * @param [args] search builder by [FieldGetterArgs]
     * @return Aggregate according to caller
     *
     *    key: caller method descriptor
     *    value: list of field descriptor
     *
     * @since 1.1.0
     */
    fun findMethodUsingField(
        args: MethodUsingFieldArgs
    ): Map<DexMethodDescriptor, List<DexFieldDescriptor>> {
        return nativeFindMethodUsingField(
            token,
            args.fieldDescriptor,
            args.fieldDeclareClass,
            args.fieldName,
            args.fieldType,
            args.usingFlag,
            args.callerMethodDeclareClass,
            args.callerMethodName,
            args.callerMethodReturnType,
            args.callerMethodParamTypes,
            args.uniqueResult,
            null
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexFieldDescriptor(it) } }
    }

    /**
     * @suppress use [findMethodUsingField] instead
     * @see [findMethodUsingField]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodUsingField(
        fieldDescriptor: String,
        fieldDeclareClass: String = "",
        fieldName: String = "",
        fieldType: String = "",
        usedFlags: Int = FLAG_USING,
        callerMethodDeclareClass: String = "",
        callerMethodName: String = "",
        callerMethodReturnType: String = "",
        callerMethodParamTypes: Array<String>? = null,
        uniqueResult: Boolean = true,
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
            uniqueResult,
            dexPriority
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { DexFieldDescriptor(it) } }
    }

    /**
     * find method used utf8 string
     *
     * @param [args] search builder by [MethodUsingStringArgs]
     * @return Aggregate according to caller
     *
     * @since 1.1.0
     */
    fun findMethodUsingString(
        args: MethodUsingStringArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingString(
            token,
            args.usingString,
            args.advancedMatch,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            args.unique,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethodUsingString] instead
     * @see [findMethodUsingString]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodUsingString(
        usingString: String,
        advancedMatch: Boolean = true,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        uniqueResult: Boolean = true,
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
            uniqueResult,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * find using annotation's class
     *
     * @param [args] search builder by [ClassUsingAnnotationArgs]
     * @return [DexClassDescriptor] list
     *
     * @since 1.1.0
     */
    @DexKitExperimentalApi
    fun findClassUsingAnnotation(
        args: ClassUsingAnnotationArgs
    ): List<DexClassDescriptor> {
        return nativeFindClassUsingAnnotation(
            token,
            args.annotationClass,
            args.annotationUsingString,
            null
        ).map { DexClassDescriptor(it) }
    }

    /**
     * @suppress use [findClassUsingAnnotation] instead
     * @see [findClassUsingAnnotation]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findClassUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        dexPriority: IntArray? = null
    ): List<DexClassDescriptor> {
        return nativeFindClassUsingAnnotation(
            token,
            annotationClass,
            annotationUsingString,
            dexPriority
        ).map { DexClassDescriptor(it) }
    }

    /**
     * find using annotation's field
     *
     * @param [args] search builder by [FieldUsingAnnotationArgs]
     * @return [DexFieldDescriptor] list
     */
    @DexKitExperimentalApi
    fun findFieldUsingAnnotation(
        args: FieldUsingAnnotationArgs
    ): List<DexFieldDescriptor> {
        return nativeFindFieldUsingAnnotation(
            token,
            args.annotationClass,
            args.annotationUsingString,
            args.fieldDeclareClass,
            args.fieldName,
            args.fieldType,
            null
        ).map { DexFieldDescriptor(it) }
    }

    /**
     * @suppress use [findFieldUsingAnnotation] instead
     * @see [findFieldUsingAnnotation]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findFieldUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        fieldDeclareClass: String = "",
        fieldName: String = "",
        fieldType: String = "",
        dexPriority: IntArray? = null
    ): List<DexFieldDescriptor> {
        return nativeFindFieldUsingAnnotation(
            token,
            annotationClass,
            annotationUsingString,
            fieldDeclareClass,
            fieldName,
            fieldType,
            dexPriority
        ).map { DexFieldDescriptor(it) }
    }

    /**
     * find using annotation's method
     *
     * @param [args] search builder by [MethodUsingAnnotationArgs]
     * @return [DexMethodDescriptor] list
     *
     * @since 1.1.0
     */
    @DexKitExperimentalApi
    fun findMethodUsingAnnotation(
        args: MethodUsingAnnotationArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingAnnotation(
            token,
            args.annotationClass,
            args.annotationUsingString,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethodUsingAnnotation] instead
     * @see [findMethodUsingAnnotation]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingAnnotation(
            token,
            annotationClass,
            annotationUsingString,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * find method by multiple conditions
     *
     * @param [args] search builder by [FindMethodArgs]
     * @return [DexMethodDescriptor] list
     *
     * @since 1.1.0
     */
    fun findMethod(
        args: FindMethodArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethod(
            token,
            args.methodDescriptor,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethod] instead
     * @see [findMethod]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethod(
        methodDeclareClass: String,
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethod(
            token,
            "",
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * find all direct subclasses of the specified class
     *
     * @param [parentClass] parent class
     * @return [DexClassDescriptor] list
     *
     * @since 1.1.0
     */
    fun findSubClasses(
        parentClass: String,
    ): List<DexClassDescriptor> {
        return nativeFindSubClasses(token, parentClass, null)
            .map { DexClassDescriptor(it) }
    }

    /**
     * @suppress use [findSubClasses] instead
     * @see [findSubClasses]
     */
    @Deprecated("param dexPriority have been deprecated")
    fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = null
    ): List<DexClassDescriptor> {
        return nativeFindSubClasses(token, parentClass, dexPriority)
            .map { DexClassDescriptor(it) }
    }

    /**
     * find all method used opcode prefix sequence
     *
     * @param [args] search builder by [MethodOpcodeArgs]
     * @return [DexMethodDescriptor] list
     *
     * @since 1.1.0
     */
    fun findMethodUsingOpPrefixSeq(
        args: MethodOpcodeArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingOpPrefixSeq(
            token,
            args.opSeq,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethodUsingOpPrefixSeq] instead
     * @see [findMethodUsingOpPrefixSeq]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodUsingOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingOpPrefixSeq(
            token,
            opPrefixSeq,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * find all method using opcode sequence
     *
     * @param [args] search builder by [MethodOpcodeArgs]
     * @return [DexMethodDescriptor] list
     *
     * @since 1.1.0
     */
    fun findMethodUsingOpCodeSeq(
        args: MethodOpcodeArgs
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingOpCodeSeq(
            token,
            args.opSeq,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * @suppress use [findMethodUsingOpCodeSeq] instead
     * @see [findMethodUsingOpCodeSeq]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun findMethodUsingOpCodeSeq(
        opSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> {
        return nativeFindMethodUsingOpCodeSeq(
            token,
            opSeq,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).map { DexMethodDescriptor(it) }
    }

    /**
     * get method opcodes sequence
     *
     * @param [args] search builder by  [FindMethodArgs]
     * @return [Int] opcode array
     *
     * @since 1.1.0
     */
    fun getMethodOpCodeSeq(
        args: FindMethodArgs
    ): Map<DexMethodDescriptor, IntArray> {
        return nativeGetMethodOpCodeSeq(
            token,
            args.methodDescriptor,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).mapKeys { DexMethodDescriptor(it.key) }
    }

    /**
     * get method opcodeFormat sequence
     *
     * @param [args] search builder by  [FindMethodArgs]
     * @return [String] opcodeFormat array
     *
     *     e.g. ["const/16", "const/16", "add-int", "move-result"]
     *
     * @since 1.1.0
     */
    fun getMethodOpFormatSeq(
        args: FindMethodArgs
    ): Map<DexMethodDescriptor, Array<String>> {
        return nativeGetMethodOpCodeSeq(
            token,
            args.methodDescriptor,
            args.methodDeclareClass,
            args.methodName,
            args.methodReturnType,
            args.methodParamTypes,
            null
        ).mapKeys { DexMethodDescriptor(it.key) }
            .mapValues { it.value.map { getOpFormat(it) }.toTypedArray() }
    }

    /**
     * @suppress use [getMethodOpCodeSeq] instead
     * @see [getMethodOpCodeSeq]
     */
    @Deprecated("full-argument functions have been deprecated")
    fun getMethodOpCodeSeq(
        methodDescriptor: String,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Map<DexMethodDescriptor, IntArray> {
        return nativeGetMethodOpCodeSeq(
            token,
            methodDescriptor,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        ).mapKeys { DexMethodDescriptor(it.key) }
    }

    companion object {

        @Deprecated("using FieldUsingType.GET.toFlag() instead")
        const val FLAG_GETTING = 0x00000001

        @Deprecated("using FieldUsingType.SET.toFlag() instead")
        const val FLAG_SETTING = 0x00000002

        @Deprecated("using FieldUsingType.ALL.toFlag() instead")
        const val FLAG_USING = FLAG_GETTING or FLAG_SETTING

        @JvmStatic
        fun create(apkPath: String): DexKitBridge? {
            val helper = DexKitBridge(apkPath)
            return if (helper.isValid) helper else null
        }

        /**
         *
         * @param loader class loader
         * @param useMemoryDexFile
         * if true, will try to use cookie to load memory dex file. else will use dex file path.
         * if cookies file contains CompactDex, will use apkPath to load dex.
         * if contains OatDex, Some functions may not work properly.
         */
        @JvmStatic
        fun create(loader: ClassLoader, useMemoryDexFile: Boolean): DexKitBridge? {
            val helper = DexKitBridge(loader, useMemoryDexFile)
            return if (helper.isValid) helper else null
        }

        @JvmStatic
        private external fun nativeInitDexKit(apkPath: String): Long

        @JvmStatic
        private external fun nativeInitDexKitByClassLoader(
            loader: ClassLoader,
            useMemoryDexFile: Boolean
        ): Long

        @JvmStatic
        private external fun nativeSetThreadNum(nativePtr: Long, threadNum: Int)

        @JvmStatic
        private external fun nativeGetDexNum(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeRelease(nativePtr: Long)

        @JvmStatic
        private external fun nativeExportDexFile(nativePtr: Long, outDir: String)

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
        private external fun nativeFindMethodCaller(
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
            uniqueResult: Boolean,
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
            uniqueResult: Boolean,
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
            uniqueResult: Boolean,
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
            uniqueResult: Boolean,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindClassUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindFieldUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            fieldDeclareClass: String,
            fieldName: String,
            fieldType: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethodUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethod(
            nativePtr: Long,
            methodDescriptor: String,
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
        private external fun nativeFindMethodUsingOpPrefixSeq(
            nativePtr: Long,
            opPrefixSeq: IntArray,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethodUsingOpCodeSeq(
            nativePtr: Long,
            opSeq: IntArray,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeGetMethodOpCodeSeq(
            nativePtr: Long,
            methodDescriptor: String,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            dexPriority: IntArray?
        ): Map<String, IntArray>
    }

    protected fun finalize() {
        close()
    }
}
