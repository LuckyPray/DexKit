@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class AnnotationsMatcher : BaseQuery() {
    var annotationsMatcher: MutableList<AnnotationMatcher>? = null
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
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
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
     * Need to match annotations.
     * ----------------
     * 要匹配的注解列表
     *
     * @param annotations annotations / 注解列表
     * @return [AnnotationsMatcher]
     */
    fun annotations(annotations: Collection<AnnotationMatcher>) = also {
        this.annotationsMatcher = annotations.toMutableList()
    }

    /**
     * Match type.
     * ----------------
     * 匹配类型。
     *
     * @param matchType match type / 匹配类型
     * @return [AnnotationsMatcher]
     */
    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param count count / 数量
     * @return [AnnotationsMatcher]
     */
    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param range range / 范围
     * @return [AnnotationsMatcher]
     */
    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param range range / 范围
     * @return [AnnotationsMatcher]
     */
    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param min min / 最小值
     * @param max max / 最大值
     * @return [AnnotationsMatcher]
     */
    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param min min / 最小值
     * @return [AnnotationsMatcher]
     */
    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }

    /**
     * Annotation count to match.
     * ----------------
     * 要匹配的注解数量。
     *
     * @param max max / 最大值
     * @return [AnnotationsMatcher]
     */
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    /**
     * Add annotation matcher to match.
     * ----------------
     * 添加要匹配的注解匹配器。
     *
     * @param annotation annotation matcher / 注解匹配器
     * @return [AnnotationsMatcher]
     */
    fun add(annotation: AnnotationMatcher) = also {
        annotationsMatcher = annotationsMatcher ?: mutableListOf()
        annotationsMatcher!!.add(annotation)
    }

    // region DSL

    /**
     * @see add
     */
    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationMatcher.() -> Unit) = also {
        add(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationsMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationsMatcher.createAnnotationsMatcher(
            fbb,
            annotationsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0,
        )
        fbb.finish(root)
        return root;
    }
}