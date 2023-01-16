@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi
import io.luckypray.dexkit.enums.MatchType

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class ClassUsingAnnotationArgs private constructor(
    override val findPackage: String,
    override val sourceFile: String,
    val annotationClass: String,
    val annotationUsingString: String,
    val matchType: Int,
) : BaseSourceArgs(findPackage, sourceFile) {

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

    class Builder : BaseSourceArgs.Builder<Builder, ClassUsingAnnotationArgs>() {

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
         * build [ClassUsingAnnotationArgs]
         *
         * @return [ClassUsingAnnotationArgs]
         */
        override fun build(): ClassUsingAnnotationArgs {
            verifyArgs()
            return ClassUsingAnnotationArgs(
                findPackage,
                sourceFile,
                annotationClass,
                annotationUsingString,
                matchType.ordinal,
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