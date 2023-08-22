@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFieldMatcher
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
    private var annotations: AnnotationsMatcher? = null
    private var getMethods: MethodsMatcher? = null
    private var putMethods: MethodsMatcher? = null

    fun name(name: StringMatcher) = also {
        this.name = name
    }

    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.name = StringMatcher(name, StringMatchType.Equal, ignoreCase)
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

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotations = annotations
    }

    fun addAnnotation(annotation: AnnotationMatcher) = also {
        this.annotations = annotations ?: AnnotationsMatcher()
        this.annotations!!.add(annotation)
    }

    fun annotationCount(count: Int) = also {
        this.annotations = annotations ?: AnnotationsMatcher()
        this.annotations!!.countRange(count)
    }

    fun annotationCount(min: Int, max: Int) = also {
        this.annotations = annotations ?: AnnotationsMatcher()
        this.annotations!!.countRange(min, max)
    }

    fun getMethods(getMethods: MethodsMatcher) = also {
        this.getMethods = getMethods
    }

    fun addGetMethod(getMethod: MethodMatcher) = also {
        this.getMethods = getMethods ?: MethodsMatcher()
        this.getMethods!!.add(getMethod)
    }

    fun putMethods(putMethods: MethodsMatcher) = also {
        this.putMethods = putMethods
    }

    fun addPutMethod(putMethod: MethodMatcher) = also {
        this.putMethods = putMethods ?: MethodsMatcher()
        this.putMethods!!.add(putMethod)
    }

    // region DSL

    fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        this.declaredClass(ClassMatcher().apply(init))
    }

    fun type(init: ClassMatcher.() -> Unit) = also {
        this.type(ClassMatcher().apply(init))
    }

    fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        this.annotations(AnnotationsMatcher().apply(init))
    }

    fun getMethods(init: MethodsMatcher.() -> Unit) = also {
        this.getMethods(MethodsMatcher().apply(init))
    }

    fun addGetMethod(init: MethodMatcher.() -> Unit) = also {
        addGetMethod(MethodMatcher().apply(init))
    }

    fun putMethods(init: MethodsMatcher.() -> Unit) = also {
        this.putMethods(MethodsMatcher().apply(init))
    }

    fun addPutMethod(init: MethodMatcher.() -> Unit) = also {
        addPutMethod(MethodMatcher().apply(init))
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
            annotations?.build(fbb) ?: 0,
            getMethods?.build(fbb) ?: 0,
            putMethods?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}