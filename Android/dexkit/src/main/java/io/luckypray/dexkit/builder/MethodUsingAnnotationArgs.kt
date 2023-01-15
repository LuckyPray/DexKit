@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi
import io.luckypray.dexkit.enums.MatchType

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class MethodUsingAnnotationArgs private constructor(
    override val findPackage: String,
    val annotationClass: String,
    val annotationUsingString: String,
    val matchType: Int,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParamTypes: Array<String>?,
) : BaseArgs(findPackage) {

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
         * match type, type of string to match
         *
         * default [MatchType.SIMILAR_REGEX], similar regex matches, only support: '^', '$'
         */
        @set:JvmSynthetic
        var matchType: MatchType = MatchType.SIMILAR_REGEX

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
         * [Builder.matchType]
         */
        fun matchType(matchType: MatchType) = this.also {
            this.matchType = matchType
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
                findPackage,
                annotationClass,
                annotationUsingString,
                matchType.ordinal,
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
