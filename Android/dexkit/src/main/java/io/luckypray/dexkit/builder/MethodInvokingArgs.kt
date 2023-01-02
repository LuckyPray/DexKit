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
        inline fun build(block: Builder.() -> Unit): MethodInvokingArgs {
            return Builder().apply(block).build()
        }
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

        /**
         * **method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Ljava/lang/String;" or "java.lang.String"
         */
        var methodDeclareClass: String = ""

        /**
         * **method name**
         *
         * if empty, match any method name
         */
        var methodName: String = ""

        /**
         * **method return type**
         *
         * if empty, match any return type
         *
         *     e.g. "I" or "int"
         */
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
        var methodParameterTypes: Array<String>? = null

        /**
         * **be invoked method declare class**
         */
        var beInvokedMethodDeclareClass: String = ""

        /**
         * **be invoked method name**
         */
        var beInvokedMethodName: String = ""

        /**
         * **be invoked method return type**
         */
        var beInvokedMethodReturnType: String = ""

        /**
         * **be invoked method parameter types**
         */
        var beInvokedMethodParameterTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        var unique: Boolean = true

        /**
         * [Builder.methodDescriptor]
         */
        fun setMethodDescriptor(methodDescriptor: String) = this.also {
            this.methodDescriptor = methodDescriptor
        }

        /**
         * [Builder.methodDeclareClass]
         */
        fun setMethodDeclareClass(methodDeclareClass: String) = this.also {
            this.methodDeclareClass = methodDeclareClass
        }

        /**
         * [Builder.methodName]
         */
        fun setMethodName(methodName: String) = this.also {
            this.methodName = methodName
        }

        /**
         * [Builder.methodReturnType]
         */
        fun setMethodReturnType(methodReturnType: String) = this.also {
            this.methodReturnType = methodReturnType
        }

        /**
         * [Builder.methodParameterTypes]
         */
        fun setMethodParameterTypes(methodParameterTypes: Array<String>?) = this.also {
            this.methodParameterTypes = methodParameterTypes
        }

        /**
         * [Builder.beInvokedMethodDeclareClass]
         */
        fun setBeInvokedMethodDeclareClass(beInvokedMethodDeclareClass: String) = this.also {
            this.beInvokedMethodDeclareClass = beInvokedMethodDeclareClass
        }

        /**
         * [Builder.beInvokedMethodName]
         */
        fun setBeInvokedMethodName(beInvokedMethodName: String) = this.also {
            this.beInvokedMethodName = beInvokedMethodName
        }

        /**
         * [Builder.beInvokedMethodReturnType]
         */
        fun setBeInvokedMethodReturnType(beInvokedMethodReturnType: String) = this.also {
            this.beInvokedMethodReturnType = beInvokedMethodReturnType
        }

        /**
         * [Builder.beInvokedMethodParameterTypes]
         */
        fun setBeInvokedMethodParameterTypes(beInvokedMethodParameterTypes: Array<String>?) = this.also {
            this.beInvokedMethodParameterTypes = beInvokedMethodParameterTypes
        }

        /**
         * [Builder.unique]
         */
        fun setUnique(unique: Boolean) = this.also {
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