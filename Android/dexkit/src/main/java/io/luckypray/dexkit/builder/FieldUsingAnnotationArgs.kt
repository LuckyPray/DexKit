@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi

/**
 * @since 1.1.0
 */
@DexKitExperimentalApi
class FieldUsingAnnotationArgs private constructor(
    val annotationClass: String,
    val annotationUsingString: String,
    val advancedMatch: Boolean,
    val fieldDeclareClass: String,
    val fieldName: String,
    val fieldType: String,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): FieldUsingAnnotationArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<FieldUsingAnnotationArgs>() {

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
         * **field declare class**
         *
         * if empty, match any class
         */
        @set:JvmSynthetic
        var fieldDeclareClass: String = ""

        /**
         * **field name**
         *
         * if empty, match any name
         */
        @set:JvmSynthetic
        var fieldName: String = ""

        /**
         * **field type**
         *
         * if empty, match any type
         */
        @set:JvmSynthetic
        var fieldType: String = ""

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
         * [Builder.fieldDeclareClass]
         */
        fun fieldDeclareClass(fieldDeclareClass: String) = this.also {
            this.fieldDeclareClass = fieldDeclareClass
        }

        /**
         * [Builder.fieldName]
         */
        fun fieldName(fieldName: String) = this.also {
            this.fieldName = fieldName
        }

        /**
         * [Builder.fieldType]
         */
        fun fieldType(fieldType: String) = this.also {
            this.fieldType = fieldType
        }

        /**
         * build [FieldUsingAnnotationArgs]
         *
         * @return [FieldUsingAnnotationArgs]
         */
        override fun build(): FieldUsingAnnotationArgs {
            verifyArgs()
            return FieldUsingAnnotationArgs(
                annotationClass,
                annotationUsingString,
                advancedMatch,
                fieldDeclareClass,
                fieldName,
                fieldType,
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