package io.luckypray.dexkit

import java.io.Closeable
import java.net.URL

class DexKitBridge(apkPath: String) : Closeable {

    companion object {
        const val FLAG_GETTING = 0x00000001
        const val FLAG_SETTING = 0x00000002
        const val FLAG_USING = FLAG_GETTING or FLAG_SETTING

        @JvmStatic
        fun create(apkPath: String): DexKitBridge? {
            val helper = DexKitBridge(apkPath)
            return if (helper.valid()) helper else null
        }

        @JvmStatic
        fun create(loader: ClassLoader): DexKitBridge? {
            loader.loadClass("java/lang/ClassLoader").declaredMethods.first {
                it.name == "findResource"
                    && it.parameterTypes.size == 1
                    && it.parameterTypes[0] == String::class.java
                    && it.returnType == URL::class.java
            }.let { method ->
                method.isAccessible = true
                val url = method.invoke(loader, "AndroidManifest.xml") as URL
                url.path.substring(5, url.path.length - 21).let {
                    val helper = DexKitBridge(it)
                    return if (helper.valid()) helper else null
                }
            }
        }
    }

    private val token: Long = 0

    fun valid(): Boolean {
        return token != 0L
    }

    private external fun initDexKit(apkPath: String)

    external fun getDexNum(): Int

    external override fun close()

    external fun batchFindClassesUsingStrings(
        map: Map<String, Collection<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, Array<String>>

    external fun batchFindMethodsUsingStrings(
        map: Map<String, Collection<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = null
    ): Map<String, Array<String>>

    external fun findMethodBeInvoked(
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
    ): Array<String>

    external fun findMethodInvoking(
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
    ): Map<String, Array<String>>

    external fun findMethodUsingField(
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
    ): Map<String, Array<String>>

    external fun findMethodUsingString(
        usingString: String,
        advancedMatch: Boolean = true,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Array<String>

    external fun findMethod(
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Array<String>

    external fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = null
    ): Array<String>

    external fun findMethodOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = null
    ): Array<String>

    init {
        initDexKit(apkPath)
    }
}