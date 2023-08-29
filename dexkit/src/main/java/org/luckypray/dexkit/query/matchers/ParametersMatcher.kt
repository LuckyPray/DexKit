@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerParametersMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.base.IntRange

class ParametersMatcher : BaseQuery() {
    var parameters: List<ParameterMatcher?>? = null
        private set
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

    fun parameters(parameters: List<ParameterMatcher?>) = also {
        this.parameters = parameters
    }

    fun count(count: Int) = also {
        this.countRange = IntRange(count)
    }

    fun range(countRange: IntRange) = also {
        this.countRange = countRange
    }

    fun range(range: kotlin.ranges.IntRange) = also {
        countRange = IntRange(range)
    }

    fun range(min: Int, max: Int) = also {
        this.countRange = IntRange(min, max)
    }

    fun add(matcher: ParameterMatcher?) = also {
        parameters = parameters ?: mutableListOf()
        if (parameters !is MutableList) {
            parameters = parameters!!.toMutableList()
        }
        (parameters as MutableList<ParameterMatcher?>).add(matcher)
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
            parameters?.let { fbb.createVectorOfTables(it.map { it?.build(fbb) ?: ParameterMatcher().build(fbb) }.toIntArray()) } ?: 0,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}