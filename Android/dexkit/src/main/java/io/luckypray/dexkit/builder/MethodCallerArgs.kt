@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
class MethodCallerArgs private constructor(
    override val findPackage: String,
    val methodDescriptor: String,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParameterTypes: Array<String>?,
    val callerMethodDescriptor: String,
    val callerMethodDeclareClass: String,
    val callerMethodName: String,
    val callerMethodReturnType: String,
    val callerMethodParameterTypes: Array<String>?,
    val uniqueResult: Boolean,
) : BaseArgs(findPackage) {

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
        fun builder(): Builder = Builder()
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
        @set:JvmSynthetic
        var methodDescriptor: String = ""

        /**
         * **method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Ljava/lang/String;" or "java.lang.String"
         */
        @set:JvmSynthetic
        var methodDeclareClass: String = ""

        /**
         * **method name**
         *
         * if empty, match any method name
         */
        @set:JvmSynthetic
        var methodName: String = ""

        /**
         * **method return type**
         *
         * if empty, match any return type
         *
         *     e.g. "I" or "int"
         */
        @set:JvmSynthetic
        var methodReturnType: String = ""

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
        @set:JvmSynthetic
        var methodParameterTypes: Array<String>? = null

        /**
         * **caller method descriptor**
         *
         * Method description will be parsed to corresponding: [callerMethodDeclareClass], [callerMethodName], [callerMethodReturnType], [callerMethodParameterTypes].
         *
         *    e.g. "Lcom/example/MainActivity;->onCreate(Landroid/os/Bundle;)V"
         */
        @set:JvmSynthetic
        var callerMethodDescriptor: String = ""

        /**
         * **caller method declare class**
         */
        @set:JvmSynthetic
        var callerMethodDeclareClass: String = ""

        /**
         * **caller method name**
         */
        @set:JvmSynthetic
        var callerMethodName: String = ""

        /**
         * **caller method return type**
         */
        @set:JvmSynthetic
        var callerMethodReturnType: String = ""

        /**
         * **caller method parameter types**
         */
        @set:JvmSynthetic
        var callerMethodParameterTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        @set:JvmSynthetic
        var unique: Boolean = true

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
         * [Builder.callerMethodDescriptor]
         */
        fun callerMethodDescriptor(callerMethodDescriptor: String) = this.also {
            this.callerMethodDescriptor = callerMethodDescriptor
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
                findPackage,
                methodDescriptor,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParameterTypes,
                callerMethodDescriptor,
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