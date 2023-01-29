package io.luckypray.dexkit.builder

abstract class BaseSourceArgs(
    override val findPackage: String,
    open val sourceFile: String,
) : BaseArgs(findPackage) {

    abstract class Builder<B : Builder<B, A>, A : BaseSourceArgs> : BaseArgs.Builder<B, A>() {

        /**
         * **source file name**
         *
         * be compiled source file name
         *
         *     .source "MainActivity.kt"
         */
        @set:JvmSynthetic
        var sourceFile: String = ""

        /**
         * [Builder.sourceFile]
         */
        @Suppress("UNCHECKED_CAST")
        fun sourceFile(sourceFile: String): B {
            this.sourceFile = sourceFile
            return this as B
        }
    }
}