@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.util.OpCodeUtil.getOpCode

/**
 * @since 1.1.0
 */
class MethodOpcodeArgs private constructor(
    override val findPackage: String,
    val opSeq: IntArray,
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
        inline fun build(block: Builder.() -> Unit): MethodOpcodeArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<MethodOpcodeArgs>() {

        /**
         * **opcodes sequence**
         *
         *     e.g. intArrayOf(Opcodes.ALOAD, Opcodes.INVOKE_VIRTUAL)
         */
        @set:JvmSynthetic
        var opSeq: IntArray = intArrayOf()

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
         * [Builder.opSeq]
         */
        fun opSeq(opSeq: IntArray) = this.also {
            this.opSeq = opSeq
        }

        /**
         * [Builder.opSeq]
         */
        fun opSeq(opSeq: List<Int>) = this.also {
            this.opSeq = opSeq.toIntArray()
        }

        /**
         * [Builder.opSeq] auto convert opFormat to opcodes
         *
         *     e.g. ["const/16", "array-length"] -> [19, 33]
         */
        fun opFormat(opFormat: Array<String>) = this.also {
            this.opSeq = opFormat.map { getOpCode(it) }.toIntArray()
        }

        /**
         * [Builder.opSeq] auto convert opFormat to opcodes
         *
         *     e.g. ["const/16", "array-length"] -> [19, 33]
         */
        fun opFormat(opFormat: List<String>) = this.also {
            this.opSeq = opFormat.map { getOpCode(it) }.toIntArray()
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
         * build [MethodOpcodeArgs]
         *
         * @return [MethodOpcodeArgs]
         */
        override fun build(): MethodOpcodeArgs {
            verifyArgs()
            return MethodOpcodeArgs(
                findPackage,
                opSeq,
                methodDeclareClass,
                methodName,
                methodReturnType,
                methodParamTypes,
            )
        }

        private fun verifyArgs() {
            if (opSeq.isEmpty()) {
                throw IllegalArgumentException("opSeq cannot be empty")
            }
        }
    }
}