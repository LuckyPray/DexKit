@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerMethodsMatcher
import org.luckypray.dexkit.query.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class MethodsMatcher : BaseQuery() {
    private var methods: List<MethodMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var methodCount: IntRange? = null

    fun methods(methods: List<MethodMatcher>) = also { this.methods = methods }
    fun matchType(matchType: MatchType) = also { this.matchType = matchType }
    fun methodCount(methodCount: IntRange) = also { this.methodCount = methodCount }
    fun methodCount(count: Int) = also { this.methodCount = IntRange(count) }
    fun methodCount(min: Int, max: Int) = also { this.methodCount = IntRange(min, max) }
    fun addMatcher(method: MethodMatcher) = also {
        methods = methods ?: mutableListOf()
        if (methods !is MutableList) {
            methods = methods!!.toMutableList()
        }
        (methods as MutableList<MethodMatcher>).add(method)
    }

    fun addMatcher(methodName: String) = also {
        addMatcher(MethodMatcher().apply { name(methodName) })
    }

    // region DSL

    fun MethodsMatcher.addMatcher(init: MethodMatcher.() -> Unit) = also {
        addMatcher(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = MethodsMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerMethodsMatcher.createMethodsMatcher(
            fbb,
            methods?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            matchType.value,
            methodCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}