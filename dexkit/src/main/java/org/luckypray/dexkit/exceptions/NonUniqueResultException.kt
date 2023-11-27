package org.luckypray.dexkit.exceptions

class NonUniqueResultException : RuntimeException {

    /**
     * Constructs a new [NonUniqueResultException] exception
     */
    constructor() : super()

    /**
     * Constructs a [NonUniqueResultException]
     */
    constructor(resultCount: Int) : super("query did not return a unique result: $resultCount")

    /**
     * Constructs a new [NonUniqueResultException] exception
     * with the specified detail message.
     * @param message the detail message.
     */
    constructor(message: String) : super(message)
}