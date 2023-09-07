@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerClassMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import java.lang.IllegalStateException

class ClassMatcher : BaseQuery() {
    var sourceMatcher: StringMatcher? = null
        private set
    var classNameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var superClassMatcher: ClassMatcher? = null
        private set
    var interfacesMatcher: InterfacesMatcher? = null
        private set
    var annotationsMatcher: AnnotationsMatcher? = null
        private set
    var fieldsMatcher: FieldsMatcher? = null
        private set
    var methodsMatcher: MethodsMatcher? = null
        private set
    var usingStringsMatcher: List<StringMatcher>? = null
        private set

    var source: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            source(value)
        }
    var className: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            className(value)
        }
    var modifiers: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            modifiers(value)
        }
    var superClass: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            superClass(value)
        }
    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            usingStrings(value)
        }

    fun source(matcher: StringMatcher) = also {
        this.sourceMatcher = matcher
    }

    @JvmOverloads
    fun source(
        source: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also { this.sourceMatcher = StringMatcher(source, matchType, ignoreCase) }

    fun className(matcher: StringMatcher) = also { this.classNameMatcher = matcher }

    @JvmOverloads
    fun className(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.classNameMatcher = StringMatcher(className, matchType, ignoreCase)
    }

    fun modifiers(matcher: AccessFlagsMatcher) = also {
        this.modifiersMatcher = matcher
    }

    @JvmOverloads
    fun modifiers(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.modifiersMatcher = AccessFlagsMatcher(modifiers, matchType)
    }

    fun superClass(superClass: ClassMatcher) = also {
        this.superClassMatcher = superClass
    }

    @JvmOverloads
    fun superClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.superClassMatcher = ClassMatcher().className(StringMatcher(className, matchType, ignoreCase))
    }

    fun interfaces(interfaces: InterfacesMatcher) = also {
        this.interfacesMatcher = interfaces
    }

    fun addInterface(interfaceMatcher: ClassMatcher) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.add(interfaceMatcher)
    }

    @JvmOverloads
    fun addInterface(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.add(ClassMatcher().className(className, matchType, ignoreCase))
    }

    fun interfaceCount(count: Int) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(count)
    }

    fun interfaceCount(range: IntRange) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(range)
    }

    fun interfaceCount(range: kotlin.ranges.IntRange) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(range)
    }

    fun interfaceCount(min: Int, max: Int) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(min, max)
    }

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotationsMatcher = annotations
    }

    fun addAnnotation(annotationMatcher: AnnotationMatcher) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.add(annotationMatcher)
    }

    fun annotationCount(count: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(count)
    }

    fun annotationCount(range: IntRange) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
    }

    fun annotationCount(range: kotlin.ranges.IntRange) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
    }

    fun annotationCount(min: Int, max: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(min, max)
    }

    fun fields(fields: FieldsMatcher) = also {
        this.fieldsMatcher = fields
    }

    fun addField(fieldMatcher: FieldMatcher) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.add(fieldMatcher)
    }

    fun fieldCount(count: Int) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(count)
    }

    fun fieldCount(range: IntRange) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(range)
    }

    fun fieldCount(range: kotlin.ranges.IntRange) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(range)
    }

    fun fieldCount(min: Int, max: Int) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(min, max)
    }

    fun methods(methods: MethodsMatcher) = also {
        this.methodsMatcher = methods
    }

    fun addMethod(methodMatcher: MethodMatcher) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.add(methodMatcher)
    }

    fun methodCount(count: Int) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(count)
    }

    fun methodCount(range: IntRange) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(range)
    }

    fun methodCount(range: kotlin.ranges.IntRange) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(range)
    }

    fun methodCount(min: Int, max: Int) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(min, max)
    }

    fun usingStringsMatcher(usingStrings: List<StringMatcher>) = also {
        this.usingStringsMatcher = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }
    }

    fun addUsingString(usingString: StringMatcher) = also {
        usingStringsMatcher = usingStringsMatcher ?: StringMatcherList()
        if (usingStringsMatcher !is StringMatcherList) {
            usingStringsMatcher = StringMatcherList(usingStringsMatcher!!)
        }
        (usingStringsMatcher as MutableList<StringMatcher>).add(usingString)
    }

    @JvmOverloads
    fun addUsingString(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        addUsingString(StringMatcher(usingString, matchType, ignoreCase))
    }

    fun usingStrings(vararg usingStrings: String) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun superClass(init: ClassMatcher.() -> Unit) = also {
        superClass(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun interfaces(init: InterfacesMatcher.() -> Unit) = also {
        interfaces(InterfacesMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addInterface(init: ClassMatcher.() -> Unit) = also {
        addInterface(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun fields(init: FieldsMatcher.() -> Unit) = also {
        fields(FieldsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addField(init: FieldMatcher.() -> Unit) = also {
        addField(FieldMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun methods(init: MethodsMatcher.() -> Unit) = also {
        methods(MethodsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addMethod(init: MethodMatcher.() -> Unit) = also {
        addMethod(MethodMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun usingStringsMatcher(init: StringMatcherList.() -> Unit) = also {
        usingStringsMatcher(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ClassMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (classNameMatcher?.getValue()?.isEmpty() == true) {
            throw IllegalStateException("className not be empty")
        }
        val root = InnerClassMatcher.createClassMatcher(
            fbb,
            sourceMatcher?.build(fbb) ?: 0,
            classNameMatcher?.build(fbb) ?: 0,
            modifiersMatcher?.build(fbb) ?: 0,
            superClassMatcher?.build(fbb) ?: 0,
            interfacesMatcher?.build(fbb) ?: 0,
            annotationsMatcher?.build(fbb) ?: 0,
            fieldsMatcher?.build(fbb) ?: 0,
            methodsMatcher?.build(fbb) ?: 0,
            usingStringsMatcher?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0
        )
        fbb.finish(root)
        return root
    }
}