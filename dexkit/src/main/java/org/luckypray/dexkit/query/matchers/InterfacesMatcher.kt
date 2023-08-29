@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerInterfacesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class InterfacesMatcher : BaseQuery() {
    var interfaces: List<ClassMatcher>? = null
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

    fun interfaces(interfaces: List<ClassMatcher>) = also {
        this.interfaces = interfaces
    }

    fun matchType(matchType: MatchType) = also {
        this.matchType = matchType
    }

    fun count(count: Int) = also {
        this.countRange = IntRange(count)
    }

    fun range(range: IntRange) = also {
        this.countRange = range
    }

    fun range(range: kotlin.ranges.IntRange) = also {
        countRange = IntRange(range)
    }

    fun range(min: Int, max: Int) = also {
        this.countRange = IntRange(min, max)
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
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        add(ClassMatcher().apply { className(className, matchType, ignoreCase) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun add(init: ClassMatcher.() -> Unit) = also {
        add(ClassMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = InterfacesMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerInterfacesMatcher.createInterfacesMatcher(
            fbb,
            interfaces?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) }
                ?: 0,
            matchType.value,
            countRange?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}