@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerParametersMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.matchers.base.IntRange

class ParametersMatcher : BaseQuery() {
    // TODO nullable
    private var parameters: List<ParameterMatcher>? = null
    private var parametersCount: IntRange? = null

    fun parameters(parameters: List<ParameterMatcher>) = also {
        this.parameters = parameters
    }

    fun parametersCount(parametersCount: IntRange) = also {
        this.parametersCount = parametersCount
    }

    fun parametersCount(count: Int) = also {
        this.parametersCount = IntRange(count)
    }

    fun parametersCount(min: Int, max: Int) = also {
        this.parametersCount = IntRange(min, max)
    }

    fun addMatcher(matcher: ParameterMatcher) {
        parameters = parameters ?: mutableListOf()
        if (parameters !is MutableList) {
            parameters = parameters!!.toMutableList()
        }
        (parameters as MutableList<ParameterMatcher>).add(matcher)
    }

    // region DSL

    fun ParametersMatcher.addMatcher(init: ParameterMatcher.() -> Unit) = also {
        addMatcher(ParameterMatcher().apply(init))
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
            parameters?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) }
                ?: 0,
            parametersCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}