@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerBatchUsingStringsMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class StringMatchersGroup : BaseQuery {

    @set:JvmSynthetic
    var groupName: String? = null
    var stringMatchers = mutableListOf<StringMatcher>()
        private set

    constructor()
    constructor(groupName: String, stringMatchers: List<StringMatcher>) {
        this.groupName = groupName
        this.stringMatchers = stringMatchers.toMutableList()
    }

    /**
     * Using string list. Default match type is contains, if you need to match exactly,
     * please use [usingStrings] or [add] overloaded function for each string.
     * ----------------
     * 使用字符串列表。默认匹配关系为包含，如需为每个字符串设置匹配关系，
     * 请使用 [usingStrings] 或者 [add] 重载函数。
     */
    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            usingStrings(value)
        }

    /**
     * Query group name. Must be unique in a single call.
     * ----------------
     * 查询组名。单次调用中需要保持唯一。
     */
    fun groupName(groupName: String) = also {
        this.groupName = groupName
    }

    /**
     * Using strings matcher.
     * ----------------
     * 使用字符串列表匹配器。
     *
     *     usingStrings(StringMatcherList().add(StringMatcher("string")))
     *
     * @param usingStrings using string list matcher / 使用字符串列表匹配器
     * @return [ClassMatcher]
     */
    fun usingStrings(usingStrings: StringMatcherList) = also {
        this.stringMatchers = usingStrings
    }

    /**
     * Using strings matcher.
     * ----------------
     * 使用字符串匹配器。
     *
     *     usingStrings(List.of("TAG", "Activity"), StringMatchType.Equals, false)
     *
     * @param usingStrings using string list / 使用字符串列表
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun usingStrings(
        usingStrings: List<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.stringMatchers = usingStrings.map { StringMatcher(it, matchType, ignoreCase) }.toMutableList()
    }

    /**
     * Using strings matcher. default match type is contains, if you need to match exactly,
     * please use [usingStrings] or [add] overloaded function for each string.
     * ----------------
     * 使用字符串匹配器。默认匹配关系为包含，如需为每个字符串设置匹配关系，
     * 请使用 [usingStrings] 或者 [add] 重载函数。
     *
     *     usingStrings("TAG", "Activity")
     *
     * @param usingStrings using string list / 使用字符串列表
     * @return [ClassMatcher]
     */
    fun usingStrings(vararg usingStrings: String) = also {
        this.stringMatchers = usingStrings.map { StringMatcher(it) }.toMutableList()
    }

    /**
     * Add string matcher.
     * ----------------
     * 添加字符串匹配器。
     *
     *     add(StringMatcher("string"))
     *
     * @param matcher string matcher / 字符串匹配器
     * @return [ClassMatcher]
     */
    fun add(matcher: StringMatcher) = also {
        stringMatchers.add(matcher)
    }

    /**
     * Add string matcher.
     * ----------------
     * 添加字符串匹配器。
     *
     *     add("string")
     *
     * @param usingString using string / 使用字符串
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun add(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(StringMatcher(usingString, matchType, ignoreCase))
    }

    // region DSL

    /**
     * @see usingStrings
     */
    @kotlin.internal.InlineOnly
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = StringMatchersGroup()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        groupName ?: throw IllegalAccessException("groupName not be null")
        if (stringMatchers.isEmpty()) throw IllegalAccessException("matchers not be empty")
        val root = InnerBatchUsingStringsMatcher.createBatchUsingStringsMatcher(
            fbb,
            fbb.createString(groupName),
            stringMatchers.map { it.build(fbb) }.toIntArray()
                .let { fbb.createVectorOfTables(it) }
        )
        fbb.finish(root)
        return root
    }
}