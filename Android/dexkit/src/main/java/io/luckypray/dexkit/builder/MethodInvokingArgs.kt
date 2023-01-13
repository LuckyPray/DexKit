@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
class MethodInvokingArgs private constructor(
    val methodDescriptor: String,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParameterTypes: Array<String>?,
    val beInvokedMethodDescriptor: String,
    val beInvokedMethodDeclareClass: String,
    val beInvokedMethodName: String,
    val beInvokedMethodReturnType: String,
    val beInvokedMethodParamTypes: Array<String>?,
    val uniqueResult: Boolean,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodInvokingArgs {
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
    class Builder : BaseArgs.Builder<MethodInvokingArgs>() {

        /**
         * **method descriptor**
         *
         * Method description will be parsed to corresponding:
         * [methodDescriptor], [methodDeclareClass], [methodName], [methodReturnType].
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
         * **be invoked method descriptor**
         *
         * Method description will be parsed to corresponding: [beInvokedMethodDeclareClass], [beInvokedMethodName], [beInvokedMethodReturnType], [beInvokedMethodParamTypes].
         *
         *    e.g. "Ljava/lang/String;->length()I"
         */
        var beInvokedMethodDescriptor: String = ""
            @JvmSynthetic set

        /**
         * **be invoked method declare class**
         */
        var beInvokedMethodDeclareClass: String = ""
            @JvmSynthetic set

        /**
         * **be invoked method name**
         */
        var beInvokedMethodName: String = ""
            @JvmSynthetic set

        /**
         * **be invoked method return type**
         */
        var beInvokedMethodReturnType: String = ""
            @JvmSynthetic set

        /**
         * **be invoked method parameter types**
         */
        var beInvokedMethodParameterTypes: Array<String>? = null
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
         * [Builder.beInvokedMethodDescriptor]
         */
        fun beInvokedMethodDescriptor(beInvokedMethodDescriptor: String) = this.also {
            this.beInvokedMethodDescriptor = beInvokedMethodDescriptor
        }

        /**
         * [Builder.beInvokedMethodDeclareClass]
         */
        fun beInvokedMethodDeclareClass(beInvokedMethodDeclareClass: String) = this.also {
            this.beInvokedMethodDeclareClass = beInvokedMethodDeclareClass
        }

        /**
         * [Builder.beInvokedMethodName]
         */
        fun beInvokedMethodName(beInvokedMethodName: String) = this.also {
            this.beInvokedMethodName = beInvokedMethodName
        }

        /**
         * [Builder.beInvokedMethodReturnType]
         */
        fun beInvokedMethodReturnType(beInvokedMethodReturnType: String) = this.also {
            this.beInvokedMethodReturnType = beInvokedMethodReturnType
        }

        /**
         * [Builder.beInvokedMethodParameterTypes]
         */
        fun beInvokedMethodParameterTypes(beInvokedMethodParameterTypes: Array<String>?) = this.also {
            this.beInvokedMethodParameterTypes = beInvokedMethodParameterTypes
        }

        /**
         * [Builder.unique]
         */
        fun unique(unique: Boolean) = this.also {
            this.unique = unique
        }

        /**
         * build [MethodInvokingArgs]
         *
         * @return [MethodInvokingArgs]
         */
        override fun build(): MethodInvokingArgs {
            verifyArgs()
            return MethodInvokingArgs(
                methodDescriptor,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParameterTypes,
                beInvokedMethodDescriptor,
                beInvokedMethodDeclareClass,
                beInvokedMethodName,
                beInvokedMethodReturnType,
                beInvokedMethodParameterTypes,
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