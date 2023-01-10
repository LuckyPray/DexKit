@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
class MethodCallerArgs private constructor(
    val methodDescriptor: String,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParameterTypes: Array<String>?,
    val callerMethodDeclareClass: String,
    val callerMethodName: String,
    val callerMethodReturnType: String,
    val callerMethodParameterTypes: Array<String>?,
    val uniqueResult: Boolean,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodCallerArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    /**
     * @since 1.1.0
     */
    class Builder : BaseArgs.Builder<MethodCallerArgs>() {

        /**
         * **method descriptor**
         *
         * Method description will be parsed to corresponding:
         * [methodDeclareClass], [methodName], [methodReturnType], [methodParameterTypes].
         *
         *     e.g. "Ljava/lang/String;->length()I"
         */
        var methodDescriptor: String = ""
            @JvmSynthetic set

        /**
         * **method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Ljava/lang/String;" or "java.lang.String"
         */
        var methodDeclareClass: String = ""
            @JvmSynthetic set

        /**
         * **method name**
         *
         * if empty, match any method name
         */
        var methodName: String = ""
            @JvmSynthetic set

        /**
         * **method return type**
         *
         * if empty, match any return type
         *
         *     e.g. "I" or "int"
         */
        var methodReturnType: String = ""
            @JvmSynthetic set

        /**
         * **method parameter types**
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
        var methodParameterTypes: Array<String>? = null
            @JvmSynthetic set

        /**
         * **caller method declare class**
         */
        var callerMethodDeclareClass: String = ""
            @JvmSynthetic set

        /**
         * **caller method name**
         */
        var callerMethodName: String = ""
            @JvmSynthetic set

        /**
         * **caller method return type**
         */
        var callerMethodReturnType: String = ""
            @JvmSynthetic set

        /**
         * **caller method parameter types**
         */
        var callerMethodParameterTypes: Array<String>? = null
            @JvmSynthetic set

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        var unique: Boolean = true
            @JvmSynthetic set

        /**
         * [Builder.methodDescriptor]
         */
        fun methodDescriptor(methodDescriptor: String) = this.also {
            this.methodDescriptor = methodDescriptor
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
         * [Builder.methodParameterTypes]
         */
        fun methodParameterTypes(methodParameterTypes: Array<String>?) = this.also {
            this.methodParameterTypes = methodParameterTypes
        }

        /**
         * [Builder.callerMethodDeclareClass]
         */
        fun callerMethodDeclareClass(callerMethodDeclareClass: String) = this.also {
            this.callerMethodDeclareClass = callerMethodDeclareClass
        }

        /**
         * [Builder.callerMethodName]
         */
        fun callerMethodName(callerMethodName: String) = this.also {
            this.callerMethodName = callerMethodName
        }

        /**
         * [Builder.callerMethodReturnType]
         */
        fun callerMethodReturnType(callerMethodReturnType: String) = this.also {
            this.callerMethodReturnType = callerMethodReturnType
        }

        /**
         * [Builder.callerMethodParameterTypes]
         */
        fun callerMethodParameterTypes(callerMethodParameterTypes: Array<String>?) = this.also {
            this.callerMethodParameterTypes = callerMethodParameterTypes
        }

        /**
         * [Builder.unique]
         */
        fun unique(unique: Boolean) = this.also {
            this.unique = unique
        }

        /**
         * build [MethodCallerArgs]
         *
         * @return [MethodCallerArgs]
         */
        override fun build(): MethodCallerArgs {
            verifyArgs()
            return MethodCallerArgs(
                methodDescriptor,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParameterTypes,
                callerMethodDeclareClass,
                callerMethodName,
                callerMethodReturnType,
                callerMethodParameterTypes,
                unique,
            )
        }

        private fun verifyArgs() {
            if (methodDescriptor.isEmpty()
                && methodDeclareClass.isEmpty()
                && methodName.isEmpty()
                && methodReturnType.isEmpty()
                && methodParameterTypes == null
            ) {
                throw IllegalArgumentException("methodDescriptor, methodDeclareClass, methodName, methodReturnType, methodParameterTypes can't all be empty")
            }
        }
    }
}