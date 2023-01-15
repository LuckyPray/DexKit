@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.enums.MatchType

/**
 * @since 1.1.0
 */
class MethodUsingStringArgs private constructor(
    val usingString: String,
    val matchType: Int,
    val methodDeclareClass: String,
    val methodName: String,
    val methodReturnType: String,
    val methodParamTypes: Array<String>?,
    val unique: Boolean,
) : BaseArgs() {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): MethodUsingStringArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<MethodUsingStringArgs>() {

        /**
         * **using string**
         *
         *     e.g. "Hello World"
         */
        @set:JvmSynthetic
        var usingString: String = ""

        /**
         * match type, type of string to match
         *
         * default [MatchType.SIMILAR_REGEX], similar regex matches, only support: '^', '$'
         */
        @set:JvmSynthetic
        var matchType: MatchType = MatchType.SIMILAR_REGEX

        /**
         * **caller method declare class**
         *
         * if empty, match any class
         *
         *     e.g. "Lcom/example/MainActivity;" or "com.example.MainActivity"
         */
        @set:JvmSynthetic
        var methodDeclareClass: String = ""

        /**
         * **caller method name**
         *
         * if empty, match any name
         */
        @set:JvmSynthetic
        var methodName: String = ""

        /**
         * **caller method return type**
         *
         * if empty, match any type
         *
         *     e.g. "V" or "void"
         */
        @set:JvmSynthetic
        var methodReturnType: String = ""

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
        var methodParamTypes: Array<String>? = null

        /**
         * **unique result**
         *
         * If true, the results will be unique. If you need to get the number of calls, set it to false.
         */
        @set:JvmSynthetic
        var unique: Boolean = true

        /**
         * [Builder.usingString]
         */
        fun usingString(usingString: String) = this.also {
            this.usingString = usingString
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
        fun methodParamTypes(methodParamTypes: Array<String>?) = this.also {
            this.methodParamTypes = methodParamTypes
        }

        /**
         * [Builder.unique]
         */
        fun unique(unique: Boolean) = this.also {
            this.unique = unique
        }

        /**
         * build [MethodUsingStringArgs]
         *
         * @return [MethodUsingStringArgs]
         */
        override fun build(): MethodUsingStringArgs {
            verifyArgs()
            return MethodUsingStringArgs(
                usingString,
                matchType.ordinal,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParamTypes,
                unique,
            )
        }

        private fun verifyArgs() {
            if (usingString.isEmpty()) {
                throw IllegalArgumentException("usingString cannot be empty")
            }
        }
    }
}