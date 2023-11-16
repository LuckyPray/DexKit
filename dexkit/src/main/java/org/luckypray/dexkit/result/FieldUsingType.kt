package org.luckypray.dexkit.result;

/**
 * Field using type
 */
enum class FieldUsingType {

    /**
     * Read field
     * ----------------
     * 读取了字段
     */
    Read,

    /**
     * Write field
     * ----------------
     * 写入了字段
     */
    Write,
    ;

    fun isRead() = this == Read

    fun isWrite() = this == Write
}