@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerMethodsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class MethodsMatcher : BaseQuery() {
    var methods: List<MethodMatcher>? = null
        private set
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains
    var countRange: IntRange? = null
        private set

    var count: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            countRange = IntRange(value)
        }
    var range: kotlin.ranges.IntRange
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            countRange = IntRange(value)
        }

    fun methods(methods: List<MethodMatcher>) = also {
        this.methods = methods
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun countRange(countRange: IntRange) = also {
        this.countRange = countRange
    }

    fun countRange(range: kotlin.ranges.IntRange) = also {
        countRange = IntRange(range)
    }

    fun countRange(count: Int) = also {
        this.countRange = IntRange(count)
    }

    fun countRange(min: Int, max: Int) = also {
        this.countRange = IntRange(min, max)
    }

    fun add(method: MethodMatcher) = also {
        methods = methods ?: mutableListOf()
        if (methods !is MutableList) {
            methods = methods!!.toMutableList()
        }
        (methods as MutableList<MethodMatcher>).add(method)
    }

    fun add(methodName: String) = also {
        add(MethodMatcher().apply { name(methodName) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun MethodsMatcher.add(init: MethodMatcher.() -> Unit) = also {
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
            methods?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}