package io.luckypray.dexkit.builder

/**
 * @since 1.1.0
 */
abstract class BaseArgs {

    abstract class Builder<T : BaseArgs> {
        abstract fun build(): T
    }
}
