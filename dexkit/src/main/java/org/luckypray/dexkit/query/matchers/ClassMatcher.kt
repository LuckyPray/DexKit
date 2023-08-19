@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerClassMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class ClassMatcher : BaseQuery() {
    private var sourceName: StringMatcher? = null
    private var className: StringMatcher? = null
    private var modifiers: AccessFlagsMatcher? = null
    private var superClass: ClassMatcher? = null
    private var interfaces: InterfacesMatcher? = null
    //TODO
//    var annotation: AnnotationsMatcher? = null
    private var fields: FieldsMatcher? = null
    private var methods: MethodsMatcher? = null
    private var usingStrings: List<StringMatcher>? = null

    fun sourceName(matcher: StringMatcher) = also {
        this.sourceName = matcher
    }

    @JvmOverloads
    fun sourceName(
        sourceName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also { this.sourceName = StringMatcher(sourceName, matchType, ignoreCase) }

    fun className(matcher: StringMatcher) = also { this.className = matcher }

    @JvmOverloads
    fun className(
        className: String,
        matchType: StringMatchType = StringMatchType.Equal,
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
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.superClass = ClassMatcher().className(StringMatcher(className, matchType, ignoreCase))
    }

    fun interfaces(interfaces: InterfacesMatcher) = also {
        this.interfaces = interfaces
    }

    fun fields(fields: FieldsMatcher) = also {
        this.fields = fields
    }

    fun methods(methods: MethodsMatcher) = also {
        this.methods = methods
    }

    fun usingStringsMatcher(usingStrings: List<StringMatcher>) = also {
        this.usingStrings = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStrings = usingStrings.map { StringMatcher(it) }
    }

    fun usingStrings(vararg usingStrings: String) = also {
        this.usingStrings = usingStrings.map { StringMatcher(it) }
    }

    // region DSL

    fun superClass(init: ClassMatcher.() -> Unit) = also {
        superClass(ClassMatcher().apply(init))
    }

    fun interfaces(init: InterfacesMatcher.() -> Unit) = also {
        interfaces(InterfacesMatcher().apply(init))
    }

    fun fields(init: FieldsMatcher.() -> Unit) = also {
        fields(FieldsMatcher().apply(init))
    }

    fun methods(init: MethodsMatcher.() -> Unit) = also {
        methods(MethodsMatcher().apply(init))
    }

    fun usingStringsMatcher(init: StringMatcherList.() -> Unit) = also {
        usingStringsMatcher(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        fun create() = ClassMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerClassMatcher.createClassMatcher(
            fbb,
            sourceName?.build(fbb) ?: 0,
            className?.build(fbb) ?: 0,
            modifiers?.build(fbb) ?: 0,
            superClass?.build(fbb) ?: 0,
            interfaces?.build(fbb) ?: 0,
            // TODO
            0,
            fields?.build(fbb) ?: 0,
            methods?.build(fbb) ?: 0,
            usingStrings?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) }
                ?: 0
        )
        fbb.finish(root)
        return root
    }
}