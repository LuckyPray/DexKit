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
        inline fun build(block: Builder.() -> Unit): FieldUsingAnnotationArgs {
            return Builder().apply(block).build()
        }
    }

    class Builder : BaseArgs.Builder<FieldUsingAnnotationArgs>() {

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
         * **field declare class**
         *
         * if empty, match any class
         */
        var fieldDeclareClass: String = ""

        /**
         * **field name**
         *
         * if empty, match any name
         */
        var fieldName: String = ""

        /**
         * **field type**
         *
         * if empty, match any type
         */
        var fieldType: String = ""

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
         * [Builder.fieldDeclareClass]
         */
        fun setFieldDeclareClass(fieldDeclareClass: String) = this.also {
            this.fieldDeclareClass = fieldDeclareClass
        }

        /**
         * [Builder.fieldName]
         */
        fun setFieldName(fieldName: String) = this.also {
            this.fieldName = fieldName
        }

        /**
         * [Builder.fieldType]
         */
        fun setFieldType(fieldType: String) = this.also {
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