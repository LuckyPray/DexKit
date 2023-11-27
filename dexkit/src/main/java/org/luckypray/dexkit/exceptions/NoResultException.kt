package org.luckypray.dexkit.exceptions

class NoResultException : RuntimeException {
    /**
     * Constructs a new `NoResultException` exception
     * with `null` as its detail message.
     */
    constructor() : super()

    /**
     * Constructs a new `NoResultException` exception
     * with the specified detail message.
     * @param   message   the detail message.
     */
    constructor(message: String) : super(message)
}