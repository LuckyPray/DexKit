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
         * search package path, use '/' to cut paths;
         *
         *     e.g. "com/example"
         */
        @set:JvmSynthetic
        var findPackage: String = ""

        /**
         * [Builder.findPackage]
         */
        @Suppress("UNCHECKED_CAST")
        fun findPath(findPath: String): B {
            this.findPackage = findPath
            return this as B
        }

        abstract fun build(): A
    }
}
