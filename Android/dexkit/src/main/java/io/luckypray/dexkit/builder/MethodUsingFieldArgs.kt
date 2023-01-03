package io.luckypray.dexkit.builder

import io.luckypray.dexkit.annotations.DexKitExperimentalApi
import io.luckypray.dexkit.util.getOpCode
import io.luckypray.dexkit.enums.FieldUsingType

/**
 * @since 1.1.0
 */
class MethodUsingFieldArgs private constructor(
    val fieldDescriptor: String,
    val fieldDeclareClass: String,
    val fieldName: String,
    val fieldType: String,
    val usingFlag: Int,
    val callerMethodDeclareClass: String,
    val callerMethodName: String,
    val callerMethodReturnType: String,
    val callerMethodParamTypes: Array<String>?,
    val uniqueResult: Boolean,
) : BaseArgs() {


    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        inline fun build(block: Builder.() -> Unit): MethodUsingFieldArgs {
            return Builder().apply(block).build()
        }
    }

    /**
     * @since 1.1.0
     */
    class Builder : BaseArgs.Builder<MethodUsingFieldArgs>() {

        /**
         * **field descriptor**
         *
         * Field description will be parsed to corresponding:
         * [fieldDeclareClass], [fieldName], [fieldType]
         *
         *     e.g. "Lcom/example/MainActivity;->mTextView:Landroid/widget/TextView;"
         */
        var fieldDescriptor: String = ""

        /**
         * **field declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;"
         */
        var fieldDeclareClass: String = ""

        /**
         * **field name**
         *
         * if empty, match any name
         *
         *     e.g. "mTextView"
         */
        var fieldName: String = ""

        /**
         * **field type**
         *
         * if empty, match any type
         *
         *     e.g. "Landroid/widget/TextView;"
         */
        var fieldType: String = ""

        /**
         * **using field type**
         *
         *     FieldUsingType.GET match "iget", "iget-*", "sget", "sget-*" instruction
         *     FieldUsingType.PUT match "iput", "iput-*", "sput", "sput-*" instruction
         *     FieldUsingType.ALL match GET or PUT
         */
        var type: FieldUsingType = FieldUsingType.ALL

        /**
         * **caller method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;" or "com.example.MainActivity"
         */
        var callerMethodDeclareClass: String = ""

        /**
         * **caller method name**
         *
         * if empty, match any name
         */
        var callerMethodName: String = ""

        /**
         * **caller method return type**
         *
         * if empty, match any type
         *
         *     e.g. "V" or "void"
         */
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
        var callerMethodParamTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        var unique: Boolean = true

        /**
         * [Builder.fieldDescriptor]
         */
        fun setFieldDescriptor(fieldDescriptor: String) = this.also {
            this.fieldDescriptor = fieldDescriptor
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
         * [Builder.type]
         */
        fun setUsedFlags(type: FieldUsingType) = this.also {
            this.type = type
        }

        /**
         * [Builder.callerMethodDeclareClass]
         */
        fun setCallerMethodDeclareClass(callerMethodDeclareClass: String) = this.also {
            this.callerMethodDeclareClass = callerMethodDeclareClass
        }

        /**
         * [Builder.callerMethodName]
         */
        fun setCallerMethodName(callerMethodName: String) = this.also {
            this.callerMethodName = callerMethodName
        }

        /**
         * [Builder.callerMethodReturnType]
         */
        fun setCallerMethodReturnType(callerMethodReturnType: String) = this.also {
            this.callerMethodReturnType = callerMethodReturnType
        }

        /**
         * [Builder.callerMethodParamTypes]
         */
        fun setCallerMethodParamTypes(callerMethodParamTypes: Array<String>?) = this.also {
            this.callerMethodParamTypes = callerMethodParamTypes
        }

        /**
         * [Builder.unique]
         */
        fun setUnique(uniqueResult: Boolean) = this.also {
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
                fieldDescriptor,
                fieldDeclareClass,
                fieldName,
                fieldType,
                type.toByteFlag(),
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