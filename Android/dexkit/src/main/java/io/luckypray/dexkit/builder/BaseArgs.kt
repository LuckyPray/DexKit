@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
abstract class BaseArgs(
    open val findPackage: String,
) {

    abstract class Builder<T : BaseArgs> {

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
        fun findPath(findPath: String): Builder<T> {
            this.findPackage = findPath
            return this
        }

        abstract fun build(): T
    }
}
