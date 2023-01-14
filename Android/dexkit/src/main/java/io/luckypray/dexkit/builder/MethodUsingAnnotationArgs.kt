@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class MethodUsingAnnotationArgs private constructor(
    val annotationClass: String,
    val annotationUsingString: String,
    val advancedMatch: Boolean,
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
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodUsingAnnotationArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<MethodUsingAnnotationArgs>() {

        /**
         * **annotation class**
         *
         *     e.g. "Lcom/example/MyAnnotation;" or "com.example.MyAnnotation"
         */
        @set:JvmSynthetic
        var annotationClass: String = ""

        /**
         * **annotation using string**
         *
         * if empty, match any annotation
         */
        @set:JvmSynthetic
        var annotationUsingString: String = ""

        /**
         * **advanced match**
         *
         * if true, match annotation using string
         */
        @set:JvmSynthetic
        var advancedMatch: Boolean = true

        /**
         * **method declare class**
         *
         * if empty, match any class
         */
        @set:JvmSynthetic
        var methodDeclareClass: String = ""

        /**
         * **method name**
         *
         * if empty, match any name
         */
        @set:JvmSynthetic
        var methodName: String = ""

        /**
         * **method return type**
         *
         * if empty, match any type
         */
        @set:JvmSynthetic
        var methodReturnType: String = ""

        /**
         * **method param types**
         *
         * if null, match any param types
         */
        @set:JvmSynthetic
        var methodParamTypes: Array<String>? = null

        /**
         * [Builder.annotationClass]
         */
        fun annotationClass(annotationClass: String) = this.also {
            this.annotationClass = annotationClass
        }

        /**
         * [Builder.annotationUsingString]
         */
        fun annotationUsingString(annotationUsingString: String) = this.also {
            this.annotationUsingString = annotationUsingString
        }

        /**
         * [Builder.advancedMatch]
         */
        fun advancedMatch(advancedMatch: Boolean) = this.also {
            this.advancedMatch = advancedMatch
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
        fun methodParamTypes(methodParamTypes: Array<String>) = this.also {
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
                advancedMatch,
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
