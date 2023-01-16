package io.luckypray.dexkit.builder

class FindClassArgs(
    override val findPackage: String,
    override val sourceFile: String,
) : BaseSourceArgs(findPackage, sourceFile) {

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

    class Builder : BaseSourceArgs.Builder<Builder, FindClassArgs>() {

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