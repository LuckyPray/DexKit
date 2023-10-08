@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerInterfacesMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.IntRange

class InterfacesMatcher : BaseQuery() {
    var interfacesMatcher: MutableList<ClassMatcher>? = null
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

    fun interfaces(interfaces: Collection<ClassMatcher>) = also {
        this.interfacesMatcher = interfaces.toMutableList()
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

    fun add(interfaceMatcher: ClassMatcher) = also {
        interfacesMatcher = interfacesMatcher ?: mutableListOf()
        interfacesMatcher!!.add(interfaceMatcher)
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
            interfacesMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}