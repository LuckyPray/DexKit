@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFieldMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class FieldMatcher : BaseQuery() {
    var nameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var classMatcher: ClassMatcher? = null
        private set
    var typeMatcher: ClassMatcher? = null
        private set
    var annotations: AnnotationsMatcher? = null
        private set
    var getMethods: MethodsMatcher? = null
        private set
    var putMethods: MethodsMatcher? = null
        private set

    var name: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            name(value)
        }
    var modifiers: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            modifiersMatcher = modifiersMatcher ?: AccessFlagsMatcher()
            modifiersMatcher!!.modifiers = value
        }
    var modifiersMatchType: MatchType
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            modifiersMatcher = modifiersMatcher ?: AccessFlagsMatcher()
            modifiersMatcher!!.matchType = value
        }
    var declaredClass: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            declaredClass(value)
        }
    var type: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            type(value)
        }

    fun name(name: StringMatcher) = also {
        this.nameMatcher = name
    }

    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.nameMatcher = StringMatcher(name, StringMatchType.Equals, ignoreCase)
    }

    fun modifiers(modifiers: AccessFlagsMatcher) = also {
        this.modifiersMatcher = modifiers
    }

    @JvmOverloads
    fun modifiers(modifiers: Int, matchType: MatchType = MatchType.Equal) = also {
        this.modifiersMatcher = AccessFlagsMatcher(modifiers, matchType)
    }

    fun declaredClass(declaredClass: ClassMatcher) = also {
        this.classMatcher = declaredClass
    }

    @JvmOverloads
    fun declaredClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.classMatcher = ClassMatcher().className(className, matchType, ignoreCase)
    }

    fun type(type: ClassMatcher) = also {
        this.typeMatcher = type
    }

    @JvmOverloads
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.typeMatcher = ClassMatcher().className(typeName, matchType, ignoreCase)
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

    @kotlin.internal.InlineOnly
    inline fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        declaredClass(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun type(init: ClassMatcher.() -> Unit) = also {
        type(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun getMethods(init: MethodsMatcher.() -> Unit) = also {
        getMethods(MethodsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addGetMethod(init: MethodMatcher.() -> Unit) = also {
        addGetMethod(MethodMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun putMethods(init: MethodsMatcher.() -> Unit) = also {
        putMethods(MethodsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addPutMethod(init: MethodMatcher.() -> Unit) = also {
        addPutMethod(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldMatcher.createFieldMatcher(
            fbb,
            nameMatcher?.build(fbb) ?: 0,
            modifiersMatcher?.build(fbb) ?: 0,
            classMatcher?.build(fbb) ?: 0,
            typeMatcher?.build(fbb) ?: 0,
            annotations?.build(fbb) ?: 0,
            getMethods?.build(fbb) ?: 0,
            putMethods?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}