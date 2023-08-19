@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerInterfacesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class InterfacesMatcher : BaseQuery() {
    private var interfaces: List<ClassMatcher>? = null
    private var matchType: MatchType = MatchType.Contains
    private var interfaceCount: IntRange? = null

    fun interfaces(interfaces: List<ClassMatcher>) = also {
        this.interfaces = interfaces
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun interfaceCount(range: IntRange) = also {
        this.interfaceCount = range
    }

    fun interfaceCount(count: Int) = also {
        this.interfaceCount = IntRange(count)
    }

    fun interfaceCount(min: Int, max: Int) = also {
        this.interfaceCount = IntRange(min, max)
    }

    fun add(interfaceMatcher: ClassMatcher) = also {
        interfaces = interfaces ?: mutableListOf()
        if (interfaces !is MutableList) {
            interfaces = interfaces!!.toMutableList()
        }
        (interfaces as MutableList<ClassMatcher>).add(interfaceMatcher)
    }

    @JvmOverloads
    fun add(
        className: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) {
        add(ClassMatcher().apply { className(className, matchType, ignoreCase) })
    }

    // region DSL

    fun add(init: ClassMatcher.() -> Unit) = also {
        add(ClassMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = InterfacesMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerInterfacesMatcher.createInterfacesMatcher(
            fbb,
            interfaces?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) }
                ?: 0,
            matchType.value,
            interfaceCount?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}