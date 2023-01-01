package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class MethodUsingAnnotationArgs private constructor(
    val annotationClass: String,
    val annotationUsingString: String,
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
        inline fun build(block: MethodUsingAnnotationArgs.Builder.() -> Unit): MethodUsingAnnotationArgs {
            return MethodUsingAnnotationArgs.Builder().apply(block).build()
        }
    }

    class Builder : BaseArgs.Builder<MethodUsingAnnotationArgs>() {

        /**
         * **annotation class**
         *
         *     e.g. "Lcom/example/MyAnnotation;" or "com.example.MyAnnotation"
         */
        var annotationClass: String = ""

        /**
         * **annotation using string**
         *
         * if empty, match any annotation
         */
        var annotationUsingString: String = ""

        /**
         * **method declare class**
         *
         * if empty, match any class
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
         */
        var methodReturnType: String = ""

        /**
         * **method param types**
         *
         * if null, match any param types
         */
        var methodParamTypes: Array<String>? = null

        /**
         * [Builder.annotationClass]
         */
        fun setAnnotationClass(annotationClass: String) = this.also {
            this.annotationClass = annotationClass
        }

        /**
         * [Builder.annotationUsingString]
         */
        fun setAnnotationUsingString(annotationUsingString: String) = this.also {
            this.annotationUsingString = annotationUsingString
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
         * [Builder.methodParamTypes]
         */
        fun setMethodParamTypes(methodParamTypes: Array<String>) = this.also {
            this.methodParamTypes = methodParamTypes
        }

        /**
         * build [MethodUsingAnnotationArgs]
         *
         * @return [MethodUsingAnnotationArgs]
         */
        override fun build(): MethodUsingAnnotationArgs {
            verifyArgs()
            return MethodUsingAnnotationArgs(
                annotationClass,
                annotationUsingString,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParamTypes,
            )
        }

        private fun verifyArgs() {
            if (annotationClass.isEmpty()
                && annotationUsingString.isEmpty()
            ) {
                throw IllegalArgumentException("annotationClass, annotationUsingString cannot all be empty")
            }
        }
    }
}
