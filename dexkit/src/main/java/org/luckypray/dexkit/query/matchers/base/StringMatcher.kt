@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerStringMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.enums.StringMatchType

class StringMatcher : BaseQuery, IAnnotationEncodeValue {
    @set:JvmSynthetic
    var value: String? = null
    @set:JvmSynthetic
    var matchType: StringMatchType = StringMatchType.Contains
    @set:JvmSynthetic
    var ignoreCase: Boolean = false

    constructor()

    /**
     * Create a new [StringMatcher].
     * ----------------
     * 创建一个新的 [StringMatcher]。
     *
     * @param value string / 字符串
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatcher]
     */
    @JvmOverloads
    constructor(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) {
        this.value = value
        this.matchType = matchType
        this.ignoreCase = ignoreCase
    }

    /**
     * Set the matched string.
     * ----------------
     * 设置待匹配的字符串。
     *
     * @param value string / 字符串
     * @return [StringMatcher]
     */
    fun value(value: String) = also {
        this.value = value
    }

    /**
     * Set the match type.
     * ----------------
     * 设置匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [StringMatcher]
     */
    fun matchType(matchType: StringMatchType) = also {
        this.matchType = matchType
    }

    /**
     * Set whether to ignore case.
     * ----------------
     * 设置是否忽略大小写。
     *
     * @param ignoreCase ignore case / 忽略大小写
     * @return [StringMatcher]
     */
    fun ignoreCase(ignoreCase: Boolean) = also {
        this.ignoreCase = ignoreCase
    }

    companion object {
        /**
         * Create a new [StringMatcher].
         * ----------------
         * 创建一个新的 [StringMatcher]。
         *
         * @param value string / 字符串
         * @param matchType match type / 匹配类型
         * @param ignoreCase ignore case / 忽略大小写
         * @return [StringMatcher]
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            value: String,
            matchType: StringMatchType = StringMatchType.Contains,
            ignoreCase: Boolean = false
        ) = StringMatcher(value, matchType, ignoreCase)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        value ?: throw IllegalArgumentException("value must not be null")
        if (value!!.isEmpty() && matchType != StringMatchType.Equals) {
            throw IllegalAccessException("value '$value' is empty, matchType must be equals")
        }
        val root = InnerStringMatcher.createStringMatcher(
            fbb,
            fbb.createString(value),
            matchType.value,
            ignoreCase
        )
        fbb.finish(root)
        return root
    }
}