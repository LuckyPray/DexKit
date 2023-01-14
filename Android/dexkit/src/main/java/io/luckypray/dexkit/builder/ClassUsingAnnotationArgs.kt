@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class ClassUsingAnnotationArgs private constructor(
    val annotationClass: String,
    val annotationUsingString: String,
    val advancedMatch: Boolean,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: ClassUsingAnnotationArgs.Builder.() -> Unit): ClassUsingAnnotationArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<ClassUsingAnnotationArgs>() {

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
         * build [ClassUsingAnnotationArgs]
         *
         * @return [ClassUsingAnnotationArgs]
         */
        override fun build(): ClassUsingAnnotationArgs {
            verifyArgs()
            return ClassUsingAnnotationArgs(
                annotationClass,
                annotationUsingString,
                advancedMatch,
            )
        }

        private fun verifyArgs() {
            if (annotationClass.isEmpty()
                && annotationUsingString.isEmpty()
            ) {
                throw IllegalArgumentException("args cannot all be empty")
            }
        }
    }
}