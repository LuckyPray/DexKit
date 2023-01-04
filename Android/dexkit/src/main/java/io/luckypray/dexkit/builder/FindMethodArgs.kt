@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
class FindMethodArgs private constructor(
    val methodDescriptor: String,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParamTypes: Array<String>?,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        inline fun build(block: Builder.() -> Unit): FindMethodArgs {
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

    class Builder : BaseArgs.Builder<FindMethodArgs>() {

        /**
         * **method descriptor**
         *
         *     e.g. "Lcom/example/MainActivity;.onCreate:(Landroid/os/Bundle;)V"
         */
        var methodDescriptor: String = ""

        /**
         * **method declare class**
         *
         *     e.g. "Lcom/example/MainActivity;" or "com.example.MainActivity"
         */
        var methodDeclareClass: String = ""

        /**
         * **method name**
         *
         * if empty, match any name
         */
        var methodName: String = ""

        /**
         * **method return type**
         *
         * if empty, match any type
         *
         *     e.g. "V" or "void"
         */
        var methodReturnType: String = ""

        /**
         * **method param types**
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
        var methodParamTypes: Array<String>? = null

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
         * [Builder.methodParamTypes]
         */
        fun methodParamTypes(methodParamTypes: Array<String>?) = this.also {
            this.methodParamTypes = methodParamTypes
        }

        /**
         * build [FindMethodArgs]
         *
         * @return [FindMethodArgs]
         */
        override fun build(): FindMethodArgs {
            verifyArgs()
            return FindMethodArgs(
                methodDescriptor,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParamTypes,
            )
        }

        private fun verifyArgs() {
            if (methodDescriptor.isEmpty()
                && methodDeclareClass.isEmpty()
                && methodName.isEmpty()
                && methodReturnType.isEmpty()
                && methodParamTypes == null
            ) {
                throw IllegalArgumentException("args cannot all be empty")
            }
        }
    }
}