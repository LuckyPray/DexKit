@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerMethodsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class MethodsMatcher : BaseQuery() {
    var methodsMatcher: MutableList<MethodMatcher>? = null
        private set

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var rangeMatcher: IntRange? = null
        private set

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     */
    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            count(value)
        }

    /**
     * Need to match methods.
     * ----------------
     * 要匹配的方法列表
     *
     * @param methods methods / 方法列表
     * @return [MethodsMatcher]
     */
    fun methods(methods: Collection<MethodMatcher>) = also {
        this.methodsMatcher = methods.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [MethodsMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param count method count / 方法数量
     * @return [MethodsMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param range method count range / 方法数量范围
     * @return [MethodsMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param range method count range / 方法数量范围
     * @return [MethodsMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param min min method count / 最小方法数量
     * @param max max method count / 最大方法数量
     * @return [MethodsMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param min min method count / 最小方法数量
     * @return [MethodsMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Method count to match.
     * ----------------
     * 要匹配的方法数量。
     *
     * @param max max method count / 最大方法数量
     * @return [MethodsMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add method to match.
     * ----------------
     * 添加要匹配的方法。
     *
     * @param method method / 方法
     * @return [MethodsMatcher]
     */
    fun add(method: MethodMatcher) = also {
        methodsMatcher = methodsMatcher ?: mutableListOf()
        methodsMatcher!!.add(method)
    }

    // region DSL

    /**
     * @see add
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: MethodMatcher.() -> Unit) = also {
        add(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = MethodsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerMethodsMatcher.createMethodsMatcher(
            fbb,
            methodsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}