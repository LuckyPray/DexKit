@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit

import io.luckypray.dexkit.annotations.DexKitExperimentalApi
import io.luckypray.dexkit.builder.BatchFindArgs
import io.luckypray.dexkit.builder.ClassUsingAnnotationArgs
import io.luckypray.dexkit.builder.FieldUsingAnnotationArgs
import io.luckypray.dexkit.builder.FindMethodArgs
import io.luckypray.dexkit.builder.MethodCallerArgs
import io.luckypray.dexkit.builder.MethodInvokingArgs
import io.luckypray.dexkit.builder.MethodOpcodeArgs
import io.luckypray.dexkit.builder.MethodUsingAnnotationArgs
import io.luckypray.dexkit.builder.MethodUsingFieldArgs
import io.luckypray.dexkit.builder.MethodUsingStringArgs
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexFieldDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.enums.FieldUsingType
import io.luckypray.dexkit.enums.MatchType
import io.luckypray.dexkit.util.OpCodeUtil.getOpFormat
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

    //#region batchFindClassesUsingStrings
    /**
     * find used all matched keywords in class (all methods of this class)
     *
     * @param [args] search builder by [BatchFindArgs]
     *
     * @since 1.1.0
     */
    fun batchFindClassesUsingStrings(
        args: BatchFindArgs
    ): Map<String, List<DexClassDescriptor>> =
        nativeBatchFindClassesUsingStrings(
            token,
            args.queryMap,
            args.matchType,
            args.findPackage,
            null
        ).mapValues { m -> m.value.map { DexClassDescriptor(it) } }

    inline fun batchFindClassesUsingStrings(builder: BatchFindArgs.Builder.() -> Unit) =
        batchFindClassesUsingStrings(BatchFindArgs.build(builder))

    /**
     * @see [batchFindClassesUsingStrings]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("batchFindClassesUsingStrings(BatchFindArgs)")
    )
    fun batchFindClassesUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> =
        nativeBatchFindClassesUsingStrings(
            token,
            map,
            if (advancedMatch) MatchType.SIMILAR_REGEX.ordinal else MatchType.CONTAINS.ordinal,
            "",
            dexPriority
        ).mapValues { m -> m.value.map { DexClassDescriptor(it) } }

    /**
     * @see [batchFindClassesUsingStrings]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("batchFindClassesUsingStrings(BatchFindArgs)")
    )
    @JvmName("batchFindClassesUsingArrayStrings")
    fun batchFindClassesUsingStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexClassDescriptor>> = nativeBatchFindClassesUsingStrings(
        token,
        map.mapValues { it.value.toList() },
        if (advancedMatch) MatchType.SIMILAR_REGEX.ordinal else MatchType.CONTAINS.ordinal,
        "",
        dexPriority
    ).mapValues { m -> m.value.map { DexClassDescriptor(it) } }

    //#endregion

    //#region batchFindMethodsUsingStrings
    /**
     * find used all matched keywords in method.
     *
     * @param [args] search builder by [BatchFindArgs]
     *
     * @since 1.1.0
     */
    fun batchFindMethodsUsingStrings(
        args: BatchFindArgs
    ): Map<String, List<DexMethodDescriptor>> =
        nativeBatchFindMethodsUsingStrings(
            token,
            args.queryMap,
            args.matchType,
            args.findPackage,
            null
        ).mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    inline fun batchFindMethodsUsingStrings(builder: BatchFindArgs.Builder.() -> Unit) =
        batchFindMethodsUsingStrings(BatchFindArgs.build(builder))

    /**
     * @see [batchFindMethodsUsingStrings]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("batchFindMethodsUsingStrings(BatchFindArgs)")
    )
    fun batchFindMethodsUsingStrings(
        map: Map<String, Iterable<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> =
        nativeBatchFindMethodsUsingStrings(
            token,
            map,
            if (advancedMatch) MatchType.SIMILAR_REGEX.ordinal else MatchType.CONTAINS.ordinal,
            "",
            dexPriority
        ).mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    /**
     * @see [batchFindMethodsUsingStrings]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("batchFindMethodsUsingStrings(BatchFindArgs)")
    )
    @JvmName("batchFindMethodsUsingArrayStrings")
    fun batchFindMethodsUsingStrings(
        map: Map<String, Array<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, List<DexMethodDescriptor>> = nativeBatchFindMethodsUsingStrings(
        token,
        map.mapValues { it.value.toList() },
        if (advancedMatch) MatchType.SIMILAR_REGEX.ordinal else MatchType.CONTAINS.ordinal,
        "",
        dexPriority
    ).mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    //#endregion

    //#region findMethodCaller
    /**
     * find caller for specified method.
     *
     * @param [args] search builder by [MethodCallerArgs]
     *
     * @since 1.1.0
     */
    fun findMethodCaller(
        args: MethodCallerArgs
    ): Map<DexMethodDescriptor, List<DexMethodDescriptor>> = nativeFindMethodCaller(
        token,
        args.methodDescriptor,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParameterTypes,
        args.callerMethodDescriptor,
        args.callerMethodDeclareClass,
        args.callerMethodName,
        args.callerMethodReturnType,
        args.callerMethodParameterTypes,
        args.uniqueResult,
        args.findPackage,
        null
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    inline fun findMethodCaller(builder: MethodCallerArgs.Builder.() -> Unit) =
        findMethodCaller(MethodCallerArgs.build(builder))

    /**
     * @see [findMethodCaller]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodCaller(MethodCallerArgs)")
    )
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
    ): List<DexMethodDescriptor> = nativeFindMethodCaller(
        token,
        methodDescriptor,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParameterTypes,
        "",
        callerMethodDeclareClass,
        callerMethodName,
        callerMethodReturnType,
        callerMethodParameterTypes,
        uniqueResult,
        "",
        dexPriority
    ).let { resultMap ->
        mutableListOf<DexMethodDescriptor>().apply {
            resultMap.forEach { (key, value) ->
                val callerMethod = DexMethodDescriptor(key)
                if (uniqueResult) {
                    add(callerMethod)
                } else {
                    value.forEach { _ -> add(callerMethod) }
                }
            }
        }
    }

    //#endregion

    //#region findMethodInvoking
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
    ): Map<DexMethodDescriptor, List<DexMethodDescriptor>> = nativeFindMethodInvoking(
        token,
        args.methodDescriptor,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParameterTypes,
        args.beInvokedMethodDescriptor,
        args.beInvokedMethodDeclareClass,
        args.beInvokedMethodName,
        args.beInvokedMethodReturnType,
        args.beInvokedMethodParamTypes,
        args.uniqueResult,
        args.findPackage,
        null
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    inline fun findMethodInvoking(builder: MethodInvokingArgs.Builder.() -> Unit) =
        findMethodInvoking(MethodInvokingArgs.build(builder))

    /**
     * @see [findMethodInvoking]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodInvoking(MethodInvokingArgs)")
    )
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
    ): Map<DexMethodDescriptor, List<DexMethodDescriptor>> = nativeFindMethodInvoking(
        token,
        methodDescriptor,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParameterTypes,
        "",
        beCalledMethodDeclareClass,
        beCalledMethodName,
        beCalledMethodReturnType,
        beCalledMethodParamTypes,
        uniqueResult,
        "",
        dexPriority
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { DexMethodDescriptor(it) } }

    //#endregion

    //#region findMethodUsingField
    /**
     * find method getting specified field.
     *
     * @param [args] search builder by [MethodUsingFieldArgs]
     * @return Aggregate according to caller
     *
     *    key: caller method descriptor
     *    value: list of field descriptor
     *
     * @since 1.1.0
     */
    fun findMethodUsingField(
        args: MethodUsingFieldArgs
    ): Map<DexMethodDescriptor, List<DexFieldDescriptor>> = nativeFindMethodUsingField(
        token,
        args.fieldDescriptor,
        args.fieldDeclareClass,
        args.fieldName,
        args.fieldType,
        args.usingFlag,
        args.callerMethodDescriptor,
        args.callerMethodDeclareClass,
        args.callerMethodName,
        args.callerMethodReturnType,
        args.callerMethodParamTypes,
        args.uniqueResult,
        args.findPackage,
        null
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { DexFieldDescriptor(it) } }

    inline fun findMethodUsingField(builder: MethodUsingFieldArgs.Builder.() -> Unit) =
        findMethodUsingField(MethodUsingFieldArgs.build(builder))

    /**
     * @see [findMethodUsingField]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodUsingField(MethodUsingFieldArgs)")
    )
    fun findMethodUsingField(
        fieldDescriptor: String,
        fieldDeclareClass: String = "",
        fieldName: String = "",
        fieldType: String = "",
        usedFlags: Int = FieldUsingType.ALL.toByteFlag(),
        callerMethodDeclareClass: String = "",
        callerMethodName: String = "",
        callerMethodReturnType: String = "",
        callerMethodParamTypes: Array<String>? = null,
        uniqueResult: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<DexMethodDescriptor, List<DexFieldDescriptor>> = nativeFindMethodUsingField(
        token,
        fieldDescriptor,
        fieldDeclareClass,
        fieldName,
        fieldType,
        usedFlags,
        "",
        callerMethodDeclareClass,
        callerMethodName,
        callerMethodReturnType,
        callerMethodParamTypes,
        uniqueResult,
        "",
        dexPriority
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { DexFieldDescriptor(it) } }

    //#endregion

    //#region findMethodUsingString
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
    ): List<DexMethodDescriptor> = nativeFindMethodUsingString(
        token,
        args.usingString,
        args.matchType,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.unique,
        args.findPackage,
        null
    ).map { DexMethodDescriptor(it) }

    inline fun findMethodUsingString(builder: MethodUsingStringArgs.Builder.() -> Unit) =
        findMethodUsingString(MethodUsingStringArgs.build(builder))

    /**
     * @see [findMethodUsingString]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodUsingString(MethodUsingStringArgs)")
    )
    fun findMethodUsingString(
        usingString: String,
        advancedMatch: Boolean = true,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        uniqueResult: Boolean = true,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> = nativeFindMethodUsingString(
        token,
        usingString,
        if (advancedMatch) MatchType.SIMILAR_REGEX.ordinal else MatchType.CONTAINS.ordinal,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        uniqueResult,
        "",
        dexPriority
    ).map { DexMethodDescriptor(it) }

    //#endregion

    //#region findClassUsingAnnotation
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
    ): List<DexClassDescriptor> = nativeFindClassUsingAnnotation(
        token,
        args.annotationClass,
        args.annotationUsingString,
        args.matchType,
        args.findPackage,
        null
    ).map { DexClassDescriptor(it) }

    @DexKitExperimentalApi
    inline fun findClassUsingAnnotation(builder: ClassUsingAnnotationArgs.Builder.() -> Unit) =
        findClassUsingAnnotation(ClassUsingAnnotationArgs.build(builder))

    /**
     * @see [findClassUsingAnnotation]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findClassUsingAnnotation(ClassUsingAnnotationArgs)")
    )
    fun findClassUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        dexPriority: IntArray? = null
    ): List<DexClassDescriptor> = nativeFindClassUsingAnnotation(
        token,
        annotationClass,
        annotationUsingString,
        MatchType.CONTAINS.ordinal,
        "",
        dexPriority
    ).map { DexClassDescriptor(it) }

    //#endregion

    //#region findFieldUsingAnnotation
    /**
     * find using annotation's field
     *
     * @param [args] search builder by [FieldUsingAnnotationArgs]
     * @return [DexFieldDescriptor] list
     */
    @DexKitExperimentalApi
    fun findFieldUsingAnnotation(
        args: FieldUsingAnnotationArgs
    ): List<DexFieldDescriptor> = nativeFindFieldUsingAnnotation(
        token,
        args.annotationClass,
        args.annotationUsingString,
        args.matchType,
        args.fieldDeclareClass,
        args.fieldName,
        args.fieldType,
        args.findPackage,
        null
    ).map { DexFieldDescriptor(it) }

    @DexKitExperimentalApi
    inline fun findFieldUsingAnnotation(builder: FieldUsingAnnotationArgs.Builder.() -> Unit) =
        findFieldUsingAnnotation(FieldUsingAnnotationArgs.build(builder))

    /**
     * @see [findFieldUsingAnnotation]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findFieldUsingAnnotation(FieldUsingAnnotationArgs)")
    )
    fun findFieldUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        fieldDeclareClass: String = "",
        fieldName: String = "",
        fieldType: String = "",
        dexPriority: IntArray? = null
    ): List<DexFieldDescriptor> = nativeFindFieldUsingAnnotation(
        token,
        annotationClass,
        annotationUsingString,
        MatchType.CONTAINS.ordinal,
        fieldDeclareClass,
        fieldName,
        fieldType,
        "",
        dexPriority
    ).map { DexFieldDescriptor(it) }

    //#endregion

    //#region findMethodUsingAnnotation
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
    ): List<DexMethodDescriptor> = nativeFindMethodUsingAnnotation(
        token,
        args.annotationClass,
        args.annotationUsingString,
        args.matchType,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    ).map { DexMethodDescriptor(it) }

    @DexKitExperimentalApi
    inline fun findMethodUsingAnnotation(builder: MethodUsingAnnotationArgs.Builder.() -> Unit) =
        findMethodUsingAnnotation(MethodUsingAnnotationArgs.build(builder))

    /**
     * @see [findMethodUsingAnnotation]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodUsingAnnotation(MethodUsingAnnotationArgs)")
    )
    fun findMethodUsingAnnotation(
        annotationClass: String,
        annotationUsingString: String = "",
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> = nativeFindMethodUsingAnnotation(
        token,
        annotationClass,
        annotationUsingString,
        MatchType.CONTAINS.ordinal,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        "",
        dexPriority
    ).map { DexMethodDescriptor(it) }

    //#endregion

    //#region findMethod

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
    ): List<DexMethodDescriptor> = nativeFindMethod(
        token,
        args.methodDescriptor,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    ).map { DexMethodDescriptor(it) }

    inline fun findMethod(builder: FindMethodArgs.Builder.() -> Unit) =
        findMethod(FindMethodArgs.build(builder))

    /**
     * @see [findMethod]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethod(FindMethodArgs)")
    )
    fun findMethod(
        methodDeclareClass: String,
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> = nativeFindMethod(
        token,
        "",
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        "",
        dexPriority
    ).map { DexMethodDescriptor(it) }

    //#endregion

    //#region findSubClasses
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
    ): List<DexClassDescriptor> = nativeFindSubClasses(token, parentClass, null)
        .map { DexClassDescriptor(it) }

    /**
     * @see [findSubClasses]
     */
    @Deprecated("param dexPriority have been deprecated", ReplaceWith("findSubClasses(String)"))
    fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = null
    ): List<DexClassDescriptor> = nativeFindSubClasses(token, parentClass, dexPriority)
        .map { DexClassDescriptor(it) }

    //#endregion

    //#region findMethodUsingOpPrefixSeq
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
    ): List<DexMethodDescriptor> = nativeFindMethodUsingOpPrefixSeq(
        token,
        args.opSeq,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    ).map { DexMethodDescriptor(it) }

    inline fun findMethodUsingOpPrefixSeq(builder: MethodOpcodeArgs.Builder.() -> Unit) =
        findMethodUsingOpPrefixSeq(MethodOpcodeArgs.build(builder))

    /**
     * @see [findMethodUsingOpPrefixSeq]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodUsingOpPrefixSeq(MethodOpcodeArgs)")
    )
    fun findMethodUsingOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> = nativeFindMethodUsingOpPrefixSeq(
        token,
        opPrefixSeq,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        "",
        dexPriority
    ).map { DexMethodDescriptor(it) }

    //#endregion

    //#region findMethodUsingOpCodeSeq
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
    ): List<DexMethodDescriptor> = nativeFindMethodUsingOpCodeSeq(
        token,
        args.opSeq,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    ).map { DexMethodDescriptor(it) }

    inline fun findMethodUsingOpCodeSeq(builder: MethodOpcodeArgs.Builder.() -> Unit) =
        findMethodUsingOpCodeSeq(MethodOpcodeArgs.build(builder))

    /**
     * @see [findMethodUsingOpCodeSeq]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("findMethodUsingOpCodeSeq(MethodOpcodeArgs)")
    )
    fun findMethodUsingOpCodeSeq(
        opSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): List<DexMethodDescriptor> = nativeFindMethodUsingOpCodeSeq(
        token,
        opSeq,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        "",
        dexPriority
    ).map { DexMethodDescriptor(it) }

    //#endregion

    //#region getMethodOpSeq
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
    ): Map<DexMethodDescriptor, IntArray> = nativeGetMethodOpCodeSeq(
        token,
        args.methodDescriptor,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    ).mapKeys { DexMethodDescriptor(it.key) }

    inline fun getMethodOpCodeSeq(builder: FindMethodArgs.Builder.() -> Unit) =
        getMethodOpCodeSeq(FindMethodArgs.build(builder))

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
    ): Map<DexMethodDescriptor, Array<String>> = nativeGetMethodOpCodeSeq(
        token,
        args.methodDescriptor,
        args.methodDeclareClass,
        args.methodName,
        args.methodReturnType,
        args.methodParamTypes,
        args.findPackage,
        null
    )
        .mapKeys { DexMethodDescriptor(it.key) }
        .mapValues { m -> m.value.map { getOpFormat(it) }.toTypedArray() }

    inline fun getMethodOpFormatSeq(builder: FindMethodArgs.Builder.() -> Unit) =
        getMethodOpFormatSeq(FindMethodArgs.build(builder))

    /**
     * @see [getMethodOpCodeSeq]
     */
    @Deprecated(
        "full-argument functions have been deprecated",
        ReplaceWith("getMethodOpCodeSeq(FindMethodArgs)")
    )
    fun getMethodOpCodeSeq(
        methodDescriptor: String,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Map<DexMethodDescriptor, IntArray> = nativeGetMethodOpCodeSeq(
        token,
        methodDescriptor,
        methodDeclareClass,
        methodName,
        methodReturnType,
        methodParamTypes,
        "",
        dexPriority
    ).mapKeys { DexMethodDescriptor(it.key) }
    //#endregion

    companion object {

        @Deprecated("using FieldUsingType.GET.toFlag() instead")
        const val FLAG_GETTING = 0x00000001

        @Deprecated("using FieldUsingType.SET.toFlag() instead")
        const val FLAG_SETTING = 0x00000002

        @Deprecated("using FieldUsingType.ALL.toFlag() instead")
        @Suppress("DEPRECATION")
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
            matchType: Int,
            findPath: String,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeBatchFindMethodsUsingStrings(
            nativePtr: Long,
            map: Map<String, Iterable<String>>,
            matchType: Int,
            findPath: String,
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
            callerMethodDescriptor: String,
            callerMethodDeclareClass: String,
            callerMethodName: String,
            callerMethodReturnType: String,
            callerMethodParameterTypes: Array<String>?,
            uniqueResult: Boolean,
            findPath: String,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodInvoking(
            nativePtr: Long,
            methodDescriptor: String,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParameterTypes: Array<String>?,
            beCalledMethodDescriptor: String,
            beCalledMethodDeclareClass: String,
            beCalledMethodName: String,
            beCalledMethodReturnType: String,
            beCalledMethodParamTypes: Array<String>?,
            uniqueResult: Boolean,
            findPath: String,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodUsingField(
            nativePtr: Long,
            fieldDescriptor: String,
            fieldDeclareClass: String,
            fieldName: String,
            fieldType: String,
            usedFlags: Int,
            callerMethodDescriptor: String,
            callerMethodDeclareClass: String,
            callerMethodName: String,
            callerMethodReturnType: String,
            callerMethodParamTypes: Array<String>?,
            uniqueResult: Boolean,
            findPath: String,
            dexPriority: IntArray?
        ): Map<String, Array<String>>

        @JvmStatic
        private external fun nativeFindMethodUsingString(
            nativePtr: Long,
            usingString: String,
            matchType: Int,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            uniqueResult: Boolean,
            findPath: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindClassUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            matchType: Int,
            findPath: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindFieldUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            matchType: Int,
            fieldDeclareClass: String,
            fieldName: String,
            fieldType: String,
            findPath: String,
            dexPriority: IntArray?
        ): Array<String>

        @JvmStatic
        private external fun nativeFindMethodUsingAnnotation(
            nativePtr: Long,
            annotationClass: String,
            annotationUsingString: String,
            matchType: Int,
            methodDeclareClass: String,
            methodName: String,
            methodReturnType: String,
            methodParamTypes: Array<String>?,
            findPath: String,
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
            findPath: String,
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
            findPath: String,
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
            findPath: String,
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
            findPath: String,
            dexPriority: IntArray?
        ): Map<String, IntArray>
    }

    protected fun finalize() {
        close()
    }
}
