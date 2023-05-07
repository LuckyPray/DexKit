@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.5
 */
class MethodUsingNumberArgs private constructor(
    override val findPackage: String,
    override val sourceFile: String,
    val usingNumber: Long,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParamTypes: Array<String>?,
    val unique: Boolean,
) : BaseSourceArgs(findPackage, sourceFile) {

    companion object {

        /**
         * @since 1.1.5
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodUsingNumberArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.5
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseSourceArgs.Builder<Builder, MethodUsingNumberArgs>() {

        /**
         * **using string**
         *
         *     e.g. 0x7f7f7f7f
         */
        @set:JvmSynthetic
        var usingNumber: Long = 0L

        /**
         * **caller method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;" or "com.example.MainActivity"
         */
        @set:JvmSynthetic
        var methodDeclareClass: String = ""

        /**
         * **caller method name**
         *
         * if empty, match any name
         */
        @set:JvmSynthetic
        var methodName: String = ""

        /**
         * **caller method return type**
         *
         * if empty, match any type
         *
         *     e.g. "V" or "void"
         */
        @set:JvmSynthetic
        var methodReturnType: String = ""

        /**
         * **caller method param types**
         *
         *     e.g. ["I", "Ljava/lang/String;"] or ["int", "java.lang.String"]
         *
         * if null, match any parameter types
         *
         *     matches(null, ["I", "java.lang.String"]) == true
         *
         * If a parameter in the argument list is empty, the parameter matches any type.
         * But if parameter args not null, parameter args must be the same length as the find method's parameter size
         *
         *     matches(["I", ""], ["int", "long"]) == true
         *     matches(["I", ""], ["int"]) == false
         */
        @set:JvmSynthetic
        var methodParamTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        @set:JvmSynthetic
        var unique: Boolean = true

        /**
         * [Builder.usingNumber]
         */
        fun usingNumber(usingNumber: Long) = this.also {
            this.usingNumber = usingNumber
        }

        /**
         * [Builder.methodDeclareClass]
         */
        fun methodDeclareClass(methodDeclareClass: String) = this.also {
            this.methodDeclareClass = methodDeclareClass
        }

        /**
         * [Builder.methodName]
         */
        fun methodName(methodName: String) = this.also {
            this.methodName = methodName
        }

        /**
         * [Builder.methodReturnType]
         */
        fun methodReturnType(methodReturnType: String) = this.also {
            this.methodReturnType = methodReturnType
        }

        /**
         * [Builder.methodParamTypes]
         */
        fun methodParamTypes(methodParamTypes: Array<String>?) = this.also {
            this.methodParamTypes = methodParamTypes
        }

        /**
         * [Builder.unique]
         */
        fun unique(unique: Boolean) = this.also {
            this.unique = unique
        }

        /**
         * build [MethodUsingNumberArgs]
         *
         * @return [MethodUsingNumberArgs]
         */
        override fun build(): MethodUsingNumberArgs {
            return MethodUsingNumberArgs(
                findPackage,
                sourceFile,
                usingNumber,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParamTypes,
                unique,
            )
        }
    }
}