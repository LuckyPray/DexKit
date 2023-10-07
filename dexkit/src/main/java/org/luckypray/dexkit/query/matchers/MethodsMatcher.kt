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
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var rangeMatcher: IntRange? = null
        private set

    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            count(value)
        }

    fun methods(methods: List<MethodMatcher>) = also {
        this.methodsMatcher = methods.toMutableList()
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun count(count: Int) = also {
        this.rangeMatcher = IntRange(count)
    }

    fun count(range: IntRange) = also {
        this.rangeMatcher = range
    }

    fun count(range: kotlin.ranges.IntRange) = also {
        rangeMatcher = IntRange(range)
    }

    fun count(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun countMin(min: Int) = also {
        this.rangeMatcher = IntRange(min, Int.MAX_VALUE)
    }
    
    fun countMax(max: Int) = also {
        this.rangeMatcher = IntRange(0, max)
    }

    fun add(method: MethodMatcher) = also {
        methodsMatcher = methodsMatcher ?: mutableListOf()
        methodsMatcher!!.add(method)
    }

    fun add(methodName: String) = also {
        add(MethodMatcher().apply { name(methodName) })
    }

    // region DSL

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