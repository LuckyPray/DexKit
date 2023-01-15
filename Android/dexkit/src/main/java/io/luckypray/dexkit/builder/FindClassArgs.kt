package io.luckypray.dexkit.builder

class FindClassArgs(
    override val findPackage: String,
    val sourceFile: String,
) : BaseArgs(findPackage) {

    companion object {

        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): FindClassArgs {
            return Builder().apply(block).build()
        }

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder : BaseArgs.Builder<FindClassArgs>() {

        /**
         * **source file name**
         */
        @set:JvmSynthetic
        var sourceFile: String = ""

        /**
         * [Builder.sourceFile]
         */
        fun sourceFile(sourceFile: String) = this.also {
            this.sourceFile = sourceFile
        }

        override fun build(): FindClassArgs {
            verify()
            return FindClassArgs(findPackage, sourceFile)
        }

        private fun verify() {
            if (sourceFile.isEmpty()
                && findPackage.isEmpty()
            ) {
                throw IllegalArgumentException("args cannot all be empty")
            }
        }
    }
}