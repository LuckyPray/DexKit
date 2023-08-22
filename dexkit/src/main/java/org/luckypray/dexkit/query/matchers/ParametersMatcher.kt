@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerParametersMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.matchers.base.IntRange

class ParametersMatcher : BaseQuery() {
    // TODO nullable
    private var parameters: List<ParameterMatcher?>? = null
    private var countRange: IntRange? = null

    fun parameters(parameters: List<ParameterMatcher?>) = also {
        this.parameters = parameters
    }

    fun countRange(countRange: IntRange) = also {
        this.countRange = countRange
    }

    fun countRange(count: Int) = also {
        this.countRange = IntRange(count)
    }

    fun countRange(min: Int, max: Int) = also {
        this.countRange = IntRange(min, max)
    }

    fun add(matcher: ParameterMatcher?) {
        parameters = parameters ?: mutableListOf()
        if (parameters !is MutableList) {
            parameters = parameters!!.toMutableList()
        }
        (parameters as MutableList<ParameterMatcher?>).add(matcher)
    }

    // region DSL

    fun ParametersMatcher.add(init: ParameterMatcher.() -> Unit) = also {
        add(ParameterMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ParametersMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerParametersMatcher.createParametersMatcher(
            fbb,
            parameters?.let { fbb.createVectorOfTables(it.map { it?.build(fbb) ?: 0 }.toIntArray()) } ?: 0,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}