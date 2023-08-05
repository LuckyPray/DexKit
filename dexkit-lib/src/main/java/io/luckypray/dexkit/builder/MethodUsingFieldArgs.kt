@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.enums.FieldUsingType

/**
 * @since 1.1.0
 */
class MethodUsingFieldArgs private constructor(
    override val findPackage: String,
    override val sourceFile: String,
    val fieldDescriptor: String,
    val fieldDeclareClass: String,
    val fieldName: String,
    val fieldType: String,
    val usingFlag: Int,
    val callerMethodDescriptor: String,
    val callerMethodDeclareClass: String,
    val callerMethodName: String,
    val callerMethodReturnType: String,
    val callerMethodParamTypes: Array<String>?,
    val uniqueResult: Boolean,
) : BaseSourceArgs(findPackage, sourceFile) {


    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodUsingFieldArgs {
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
    class Builder : BaseSourceArgs.Builder<Builder, MethodUsingFieldArgs>() {

        /**
         * **field descriptor**
         *
         * Field description will be parsed to corresponding:
         * [fieldDeclareClass], [fieldName], [fieldType]
         *
         *     e.g. "Lcom/example/MainActivity;->mTextView:Landroid/widget/TextView;"
         */
        @set:JvmSynthetic
        var fieldDescriptor: String = ""

        /**
         * **field declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;"
         */
        @set:JvmSynthetic
        var fieldDeclareClass: String = ""

        /**
         * **field name**
         *
         * if empty, match any name
         *
         *     e.g. "mTextView"
         */
        @set:JvmSynthetic
        var fieldName: String = ""

        /**
         * **field type**
         *
         * if empty, match any type
         *
         *     e.g. "Landroid/widget/TextView;"
         */
        @set:JvmSynthetic
        var fieldType: String = ""

        /**
         * **using field type**
         *
         *     FieldUsingType.GET match "iget", "iget-*", "sget", "sget-*" instruction
         *     FieldUsingType.PUT match "iput", "iput-*", "sput", "sput-*" instruction
         *     FieldUsingType.ALL match GET or PUT
         */
        @set:JvmSynthetic
        var usingType: FieldUsingType = FieldUsingType.ALL

        /**
         * **caller method descriptor**
         *
         * Caller method description will be parsed to corresponding: [callerMethodDeclareClass], [callerMethodName], [callerMethodReturnType], [callerMethodParamTypes]
         *
         *    e.g. "Lcom/example/MainActivity;->onCreate(Landroid/os/Bundle;)V"
         */
        @set:JvmSynthetic
        var callerMethodDescriptor: String = ""

        /**
         * **caller method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;" or "com.example.MainActivity"
         */
        @set:JvmSynthetic
        var callerMethodDeclareClass: String = ""

        /**
         * **caller method name**
         *
         * if empty, match any name
         */
        @set:JvmSynthetic
        var callerMethodName: String = ""

        /**
         * **caller method return type**
         *
         * if empty, match any type
         *
         *     e.g. "V" or "void"
         */
        @set:JvmSynthetic
        var callerMethodReturnType: String = ""

        /**
         * **caller method param types**
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
        var callerMethodParamTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        @set:JvmSynthetic
        var unique: Boolean = true

        /**
         * [Builder.fieldDescriptor]
         */
        fun fieldDescriptor(fieldDescriptor: String) = this.also {
            this.fieldDescriptor = fieldDescriptor
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
         * [Builder.usingType]
         */
        fun usingType(type: FieldUsingType) = this.also {
            this.usingType = type
        }

        /**
         * [Builder.callerMethodDescriptor]
         */
        fun callerMethodDescriptor(callerMethodDescriptor: String) = this.also {
            this.callerMethodDescriptor = callerMethodDescriptor
        }

        /**
         * [Builder.callerMethodDeclareClass]
         */
        fun callerMethodDeclareClass(callerMethodDeclareClass: String) = this.also {
            this.callerMethodDeclareClass = callerMethodDeclareClass
        }

        /**
         * [Builder.callerMethodName]
         */
        fun callerMethodName(callerMethodName: String) = this.also {
            this.callerMethodName = callerMethodName
        }

        /**
         * [Builder.callerMethodReturnType]
         */
        fun callerMethodReturnType(callerMethodReturnType: String) = this.also {
            this.callerMethodReturnType = callerMethodReturnType
        }

        /**
         * [Builder.callerMethodParamTypes]
         */
        fun callerMethodParamTypes(callerMethodParamTypes: Array<String>?) = this.also {
            this.callerMethodParamTypes = callerMethodParamTypes
        }

        /**
         * [Builder.unique]
         */
        fun unique(uniqueResult: Boolean) = this.also {
            this.unique = uniqueResult
        }

        /**
         * build [MethodUsingFieldArgs]
         *
         * @return [MethodUsingFieldArgs]
         */
        override fun build(): MethodUsingFieldArgs {
            verifyArgs()
            return MethodUsingFieldArgs(
                findPackage,
                sourceFile,
                fieldDescriptor,
                fieldDeclareClass,
                fieldName,
                fieldType,
                usingType.toByteFlag(),
                callerMethodDescriptor,
                callerMethodDeclareClass,
                callerMethodName,
                callerMethodReturnType,
                callerMethodParamTypes,
                unique,
            )
        }

        private fun verifyArgs() {
            if (fieldDescriptor.isEmpty()
                && fieldDeclareClass.isEmpty()
                && fieldName.isEmpty()
                && fieldType.isEmpty()
            ) {
                throw IllegalArgumentException("fieldDescriptor, fieldDeclareClass, fieldName, fieldType cannot be all empty")
            }
        }
    }
}