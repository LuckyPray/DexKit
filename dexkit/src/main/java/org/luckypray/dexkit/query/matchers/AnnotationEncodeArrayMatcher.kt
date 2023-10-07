@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationEncodeArrayMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AnnotationEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class AnnotationEncodeArrayMatcher : BaseQuery() {
    var encodeValuesMatcher: MutableList<AnnotationEncodeValueMatcher>? = null
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

    fun matchers(matchers: List<AnnotationEncodeValueMatcher>) = also {
        this.encodeValuesMatcher = matchers.toMutableList()
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

    fun add(matcher: AnnotationEncodeValueMatcher) = also {
        encodeValuesMatcher = encodeValuesMatcher ?: mutableListOf()
        encodeValuesMatcher!!.add(matcher)
    }

    fun addNumber(value: Number) = also {
        add(AnnotationEncodeValueMatcher().apply {
            when (value) {
                is Byte -> byteValue(value)
                is Short -> shortValue(value)
                is Int -> intValue(value)
                is Long -> longValue(value)
                is Float -> floatValue(value)
                is Double -> doubleValue(value)
            }
        })
    }

    fun addByte(value: Byte) = also {
        add(AnnotationEncodeValueMatcher().apply { byteValue(value) })
    }

    fun addShort(value: Short) = also {
        add(AnnotationEncodeValueMatcher().apply { shortValue(value) })
    }

    fun addInt(value: Int) = also {
        add(AnnotationEncodeValueMatcher().apply { intValue(value) })
    }

    fun addLong(value: Long) = also {
        add(AnnotationEncodeValueMatcher().apply { longValue(value) })
    }

    fun addFloat(value: Float) = also {
        add(AnnotationEncodeValueMatcher().apply { floatValue(value) })
    }

    fun addDouble(value: Double) = also {
        add(AnnotationEncodeValueMatcher().apply { doubleValue(value) })
    }

    fun addString(value: StringMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { stringValue(value) })
    }

    @JvmOverloads
    fun addString(
        value: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(AnnotationEncodeValueMatcher().apply { stringValue(value, matchType, ignoreCase) })
    }

    fun addClass(value: ClassMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { classValue(value) })
    }

    @JvmOverloads
    fun addClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        add(AnnotationEncodeValueMatcher().apply {
            classValue { className(className, matchType, ignoreCase) }
        })
    }

    fun addEnum(value: FieldMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { enumValue(value) })
    }

    fun addArray(value: AnnotationEncodeArrayMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { arrayValue(value) })
    }

    fun addAnnotation(value: AnnotationMatcher) = also {
        add(AnnotationEncodeValueMatcher().apply { annotationValue(value) })
    }

    fun addBool(value: Boolean) = also {
        add(AnnotationEncodeValueMatcher().apply { boolValue(value) })
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun add(init: AnnotationEncodeValueMatcher.() -> Unit) = also {
        add(AnnotationEncodeValueMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addClass(init: ClassMatcher.() -> Unit) = also {
        addClass(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addEnum(init: FieldMatcher.() -> Unit) = also {
        addEnum(FieldMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addArray(init: AnnotationEncodeArrayMatcher.() -> Unit) = also {
        addArray(AnnotationEncodeArrayMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationElementsMatcher()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationEncodeArrayMatcher.createAnnotationEncodeArrayMatcher(
            fbb,
            encodeValuesMatcher?.map { it.type!!.value }?.toUByteArray()
                ?.let { InnerAnnotationEncodeArrayMatcher.createValuesTypeVector(fbb, it) } ?: 0,
            encodeValuesMatcher?.map { it.value!!.build(fbb) }?.toIntArray()
                ?.let { InnerAnnotationEncodeArrayMatcher.createValuesVector(fbb, it) } ?: 0,
            matchType.value,
            rangeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}