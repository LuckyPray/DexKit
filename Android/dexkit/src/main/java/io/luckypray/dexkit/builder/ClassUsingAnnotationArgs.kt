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
        inline fun build(block: ClassUsingAnnotationArgs.Builder.() -> Unit): ClassUsingAnnotationArgs {
            return Builder().apply(block).build()
        }
    }

    class Builder : BaseArgs.Builder<ClassUsingAnnotationArgs>() {

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
         * **advanced match**
         *
         * if true, match annotation using string
         */
        var advancedMatch: Boolean = true

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