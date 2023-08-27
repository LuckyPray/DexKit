@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerClassMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import java.lang.IllegalStateException

class ClassMatcher : BaseQuery() {
    private var source: StringMatcher? = null
    private var className: StringMatcher? = null
    private var modifiers: AccessFlagsMatcher? = null
    private var superClass: ClassMatcher? = null
    private var interfaces: InterfacesMatcher? = null
    private var annotations: AnnotationsMatcher? = null
    private var fields: FieldsMatcher? = null
    private var methods: MethodsMatcher? = null
    private var usingStrings: List<StringMatcher>? = null

    fun sourceName(matcher: StringMatcher) = also {
        this.source = matcher
    }

    @JvmOverloads
    fun source(
        source: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also { this.source = StringMatcher(source, matchType, ignoreCase) }

    fun className(matcher: StringMatcher) = also { this.className = matcher }

    @JvmOverloads
    fun className(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.className = StringMatcher(className, matchType, ignoreCase)
    }

    fun modifiers(matcher: AccessFlagsMatcher) = also {
        this.modifiers = matcher
    }

    @JvmOverloads
    fun modifiers(modifiers: Int, matchType: MatchType = MatchType.Equal) = also {
        this.modifiers = AccessFlagsMatcher(modifiers, matchType)
    }

    fun superClass(superClass: ClassMatcher) = also {
        this.superClass = superClass
    }

    @JvmOverloads
    fun superClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.superClass = ClassMatcher().className(StringMatcher(className, matchType, ignoreCase))
    }

    fun interfaces(interfaces: InterfacesMatcher) = also {
        this.interfaces = interfaces
    }

    fun addInterface(interfaceMatcher: ClassMatcher) = also {
        this.interfaces = this.interfaces ?: InterfacesMatcher()
        this.interfaces!!.add(interfaceMatcher)
    }

    @JvmOverloads
    fun addInterface(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.interfaces = this.interfaces ?: InterfacesMatcher()
        this.interfaces!!.add(ClassMatcher().className(className, matchType, ignoreCase))
    }

    fun interfaceCount(count: Int) = also {
        this.interfaces = this.interfaces ?: InterfacesMatcher()
        this.interfaces!!.countRange(count)
    }

    fun interfaceCount(min: Int, max: Int) = also {
        this.interfaces = this.interfaces ?: InterfacesMatcher()
        this.interfaces!!.countRange(min, max)
    }

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotations = annotations
    }

    fun addAnnotation(annotationMatcher: AnnotationMatcher) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.add(annotationMatcher)
    }

    fun annotationCount(count: Int) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.countRange(count)
    }

    fun annotationCount(min: Int, max: Int) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.countRange(min, max)
    }

    fun fields(fields: FieldsMatcher) = also {
        this.fields = fields
    }

    fun addField(fieldMatcher: FieldMatcher) = also {
        this.fields = this.fields ?: FieldsMatcher()
        this.fields!!.add(fieldMatcher)
    }

    fun fieldCount(count: Int) = also {
        this.fields = this.fields ?: FieldsMatcher()
        this.fields!!.countRange(count)
    }

    fun fieldCount(min: Int, max: Int) = also {
        this.fields = this.fields ?: FieldsMatcher()
        this.fields!!.countRange(min, max)
    }

    fun methods(methods: MethodsMatcher) = also {
        this.methods = methods
    }

    fun addMethod(methodMatcher: MethodMatcher) = also {
        this.methods = this.methods ?: MethodsMatcher()
        this.methods!!.add(methodMatcher)
    }

    fun methodCount(count: Int) = also {
        this.methods = this.methods ?: MethodsMatcher()
        this.methods!!.countRange(count)
    }

    fun methodCount(min: Int, max: Int) = also {
        this.methods = this.methods ?: MethodsMatcher()
        this.methods!!.countRange(min, max)
    }

    fun usingStringsMatcher(usingStrings: List<StringMatcher>) = also {
        this.usingStrings = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStrings = usingStrings.map { StringMatcher(it) }
    }

    fun addUsingString(usingString: StringMatcher) = also {
        usingStrings = usingStrings ?: StringMatcherList()
        if (usingStrings !is StringMatcherList) {
            usingStrings = StringMatcherList(usingStrings!!)
        }
        (usingStrings as MutableList<StringMatcher>).add(usingString)
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
        this.usingStrings = usingStrings.map { StringMatcher(it) }
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
        if (className?.getValue()?.isEmpty() == true) {
            throw IllegalStateException("className not be empty")
        }
        val root = InnerClassMatcher.createClassMatcher(
            fbb,
            source?.build(fbb) ?: 0,
            className?.build(fbb) ?: 0,
            modifiers?.build(fbb) ?: 0,
            superClass?.build(fbb) ?: 0,
            interfaces?.build(fbb) ?: 0,
            annotations?.build(fbb) ?: 0,
            fields?.build(fbb) ?: 0,
            methods?.build(fbb) ?: 0,
            usingStrings?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0
        )
        fbb.finish(root)
        return root
    }
}