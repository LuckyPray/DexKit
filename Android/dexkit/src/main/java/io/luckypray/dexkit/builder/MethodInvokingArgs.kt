@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
class MethodInvokingArgs private constructor(
    override val findPackage: String,
    override val sourceFile: String,
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
) : BaseSourceArgs(findPackage, sourceFile) {

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
    class Builder : BaseSourceArgs.Builder<Builder, MethodInvokingArgs>() {

        /**
         * **method descriptor**
         *
         * Method description will be parsed to corresponding:
         * [methodDescriptor], [methodDeclareClass], [methodName], [methodReturnType].
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
         * **be invoked method descriptor**
         *
         * Method description will be parsed to corresponding: [beInvokedMethodDeclareClass], [beInvokedMethodName], [beInvokedMethodReturnType], [beInvokedMethodParamTypes].
         *
         *    e.g. "Ljava/lang/String;->length()I"
         */
        @set:JvmSynthetic
        var beInvokedMethodDescriptor: String = ""

        /**
         * **be invoked method declare class**
         */
        @set:JvmSynthetic
        var beInvokedMethodDeclareClass: String = ""

        /**
         * **be invoked method name**
         */
        @set:JvmSynthetic
        var beInvokedMethodName: String = ""

        /**
         * **be invoked method return type**
         */
        @set:JvmSynthetic
        var beInvokedMethodReturnType: String = ""

        /**
         * **be invoked method parameter types**
         */
        @set:JvmSynthetic
        var beInvokedMethodParameterTypes: Array<String>? = null

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
        fun beInvokedMethodParameterTypes(beInvokedMethodParameterTypes: Array<String>?) =
            this.also {
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
                findPackage,
                sourceFile,
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