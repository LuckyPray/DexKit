@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
abstract class BaseArgs(
    open val findPackage: String,
) {

    abstract class Builder<B : Builder<B, A>, A : BaseArgs> {

        /**
         * **find package**
         *
         * search package path
         *
         *     e.g.
         *     "com/example"    // contains "com.example"
         *     "com.example"    // contains "com.example"
         *     ""               // match all
         *     "/"              // match all
         *     "/com/example"   // must start with com.example
         *     "example"        // contains "example"
         */
        @set:JvmSynthetic
        var findPackage: String = ""

        @Suppress("UNCHECKED_CAST")
        fun findPackage(findPackage: String): B {
            this.findPackage = findPackage
            return this as B
        }

        /**
         * [Builder.findPackage]
         */
        @Suppress("UNCHECKED_CAST")
        @Deprecated(
            replaceWith = ReplaceWith("findPackage(findPackage)"),
            message = "use findPackage(findPackage) instead"
        )
        fun findPath(findPath: String): B {
            this.findPackage = findPath
            return this as B
        }

        abstract fun build(): A
    }
}
