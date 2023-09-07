@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerParametersMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.base.IntRange

class ParametersMatcher : BaseQuery() {
    var parametersMatcher: List<ParameterMatcher?>? = null
        private set
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

    fun parameters(parameters: List<ParameterMatcher?>) = also {
        this.parametersMatcher = parameters
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

    fun count(min: Int, max: Int) = also {
        this.rangeMatcher = IntRange(min, max)
    }

    fun add(matcher: ParameterMatcher?) = also {
        parametersMatcher = parametersMatcher ?: mutableListOf()
        if (parametersMatcher !is MutableList) {
            parametersMatcher = parametersMatcher!!.toMutableList()
        }
        (parametersMatcher as MutableList<ParameterMatcher?>).add(matcher)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun ParametersMatcher.add(init: ParameterMatcher.() -> Unit) = also {
        add(ParameterMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ParametersMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerParametersMatcher.createParametersMatcher(
            fbb,
            parametersMatcher?.let { fbb.createVectorOfTables(it.map { it?.build(fbb) ?: ParameterMatcher().build(fbb) }.toIntArray()) } ?: 0,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}