package io.luckypray.dexkit

class DexKitHelper {

    companion object {
        const val FLAG_GETTING = 1
        const val FLAG_SETTING = 2
        const val FLAG_USING = FLAG_GETTING or FLAG_SETTING
    }

    /**
     * 使用完成后切记记得调用 [release]，否则内存不会释放
     */
    private var token: Long = 0

    constructor(classLoader: ClassLoader) {
        initDexKit(classLoader)
    }
    constructor(apkPath: String) {
        initDexByPath(apkPath)
    }

    private external fun initDexKit(classLoader: ClassLoader)

    private external fun initDexByPath(apkPath: String)

    private external fun release()

    private external fun batchFindClassesUsedStrings(
        map: Map<String, Set<String>>,
        advancedMatch: Boolean,
        dexPriority: IntArray? = null,
    ): Map<String, Array<String>>

    private external fun batchFindMethodsUsedStrings(
        map: Map<String, Set<String>>,
        advancedMatch: Boolean,
        dexPriority: IntArray? = null,
    ): Map<String, Array<String>>

    private external fun findMethodBeInvoked(
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Array<String>

    private external fun findMethodInvoking(
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        beCalledMethodDeclareClass: String,
        beCalledMethodName: String,
        beCalledMethodReturnType: String,
        beCalledMethodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Map<String, Array<String>>

    private external fun findMethodUsedField(
        fieldDescriptor: String,
        fieldDeclareClass: String,
        fieldName: String,
        fieldType: String,
        usedFlags: Int,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Array<String>

    private external fun findMethodUsedString(
        usedString: String,
        advancedMatch: Boolean,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Array<String>

    private external fun findMethod(
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Array<String>

    private external fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = null,
    ): Array<String>

    private external fun findMethodOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray? = null,
    ): Array<String>
}