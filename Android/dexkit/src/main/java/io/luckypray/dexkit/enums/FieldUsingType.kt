package io.luckypray.dexkit.enums

/**
 * @since 1.1.0
 */
enum class FieldUsingType {
    /**
     * using field for opcode:
     *
     *     iget, iget-*, sget, sget-*
     */
    GET,

    /**
     * using field for opcode:
     *
     *     iput, iput-*, sput, sput-*
     */
    PUT,

    /**
     * using field [GET] or [PUT]
     *
     *     iget, iget-*, sget, sget-*, iput, iput-*, sput, sput-*
     */
    ALL;

    fun toByteFlag(): Int {
        return when (this) {
            GET -> 0x01
            PUT -> 0x02
            ALL -> 0x03
        }
    }
}