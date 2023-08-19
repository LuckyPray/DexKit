@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerFieldMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class FieldMatcher : BaseQuery() {
    private var name: StringMatcher? = null
    private var modifiers: AccessFlagsMatcher? = null
    private var declaredClass: ClassMatcher? = null
    private var type: ClassMatcher? = null
    //TODO
//    var annotation: AnnotationsMatcher? = null
    private var getMethods: MethodsMatcher? = null
    private var setMethods: MethodsMatcher? = null

    fun name(name: StringMatcher) = also {
        this.name = name
    }

    fun name(name: String) = also {
        this.name = StringMatcher(name, StringMatchType.Equal)
    }

    fun modifiers(modifiers: AccessFlagsMatcher) = also {
        this.modifiers = modifiers
    }

    @JvmOverloads
    fun modifiers(modifiers: Int, matchType: MatchType = MatchType.Equal) = also {
        this.modifiers = AccessFlagsMatcher(modifiers, matchType)
    }

    fun declaredClass(declaredClass: ClassMatcher) = also {
        this.declaredClass = declaredClass
    }

    @JvmOverloads
    fun declaredClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.declaredClass = ClassMatcher().className(className, matchType, ignoreCase)
    }

    fun type(type: ClassMatcher) = also {
        this.type = type
    }

    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.type = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    fun getMethods(getMethods: MethodsMatcher) = also {
        this.getMethods = getMethods
    }

    fun setMethods(setMethods: MethodsMatcher) = also {
        this.setMethods = setMethods
    }

    // region DSL

    fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        this.declaredClass(ClassMatcher().apply(init))
    }

    fun type(init: ClassMatcher.() -> Unit) = also {
        this.type(ClassMatcher().apply(init))
    }

    fun getMethods(init: MethodsMatcher.() -> Unit) = also {
        this.getMethods(MethodsMatcher().apply(init))
    }

    fun setMethods(init: MethodsMatcher.() -> Unit) = also {
        this.setMethods(MethodsMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldMatcher.createFieldMatcher(
            fbb,
            name?.build(fbb) ?: 0,
            modifiers?.build(fbb) ?: 0,
            declaredClass?.build(fbb) ?: 0,
            type?.build(fbb) ?: 0,
            // TODO
            0,
            getMethods?.build(fbb) ?: 0,
            setMethods?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}