@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerMethodMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.FieldMatcherList
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class MethodMatcher : BaseQuery() {
    private var name: StringMatcher? = null
    private var modifiers: AccessFlagsMatcher? = null
    private var declaredClass: ClassMatcher? = null
    private var returnType: ClassMatcher? = null
    private var parameters: ParametersMatcher? = null
    // TODO
//    var annotations: AnnotationsMatcher? = null
    private var opCodes: OpCodesMatcher? = null
    private var usingStrings: List<StringMatcher>? = null
    private var usingFields: List<FieldMatcher>? = null
    // TODO
//    var usingNumbers: List<Number>? = null
    private var invokingMethods: MethodsMatcher? = null
    private var methodCallers: MethodsMatcher? = null

    fun name(name: StringMatcher) = also {
        this.name = name
    }

    fun name(name: String) = also {
        this.name = StringMatcher(name)
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

    fun declaredClass(declaredClassName: String) = also {
        this.declaredClass = ClassMatcher().className(declaredClassName)
    }

    fun returnType(returnType: ClassMatcher) = also {
        this.returnType = returnType
    }

    fun returnType(returnTypeName: String) = also {
        this.returnType = ClassMatcher().className(returnTypeName)
    }

    fun parameters(parameters: ParametersMatcher) = also {
        this.parameters = parameters
    }

    fun opCodes(opCodes: OpCodesMatcher) = also {
        this.opCodes = opCodes
    }

    fun opCodes(
        opCodes: List<Int>,
        matchType: MatchType = MatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = OpCodesMatcher(opCodes, matchType, opCodeSize)
    }

    fun opNames(
        opNames: List<String>,
        matchType: MatchType = MatchType.Contains,
        opCodeSize: IntRange? = null
    ) {
        this.opCodes = OpCodesMatcher.createForOpNames(opNames, matchType, opCodeSize)
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

    fun usingFields(usingFields: List<FieldMatcher>) = also {
        this.usingFields = usingFields
    }

    fun invokingMethods(invokingMethods: MethodsMatcher) = also {
        this.invokingMethods = invokingMethods
    }

    fun methodCallers(methodCallers: MethodsMatcher) = also {
        this.methodCallers = methodCallers
    }

    // region DSL

    fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        this.declaredClass = ClassMatcher().apply(init)
    }

    fun returnType(init: ClassMatcher.() -> Unit) = also {
        this.returnType = ClassMatcher().apply(init)
    }

    fun parameters(init: ParametersMatcher.() -> Unit) = also {
        this.parameters = ParametersMatcher().apply(init)
    }

    fun usingStringsMatcher(init: StringMatcherList.() -> Unit) = also {
        this.usingStrings = StringMatcherList().apply(init)
    }

    fun usingFields(init: FieldMatcherList.() -> Unit) = also {
        this.usingFields = FieldMatcherList().apply(init)
    }

    fun invokingMethods(init: MethodsMatcher.() -> Unit) = also {
        this.invokingMethods = MethodsMatcher().apply(init)
    }

    fun methodCallers(init: MethodsMatcher.() -> Unit) = also {
        this.methodCallers = MethodsMatcher().apply(init)
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = MethodMatcher()
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerMethodMatcher.createMethodMatcher(
            fbb,
            name?.build(fbb) ?: 0,
            modifiers?.build(fbb) ?: 0,
            declaredClass?.build(fbb) ?: 0,
            returnType?.build(fbb) ?: 0,
            parameters?.build(fbb) ?: 0,
            // TODO
            0,
            opCodes?.build(fbb) ?: 0,
            usingStrings?.let {
                fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray())
            } ?: 0,
            usingFields?.let {
                fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray())
            } ?: 0,
            // TODO
            0,
            0,
            invokingMethods?.build(fbb) ?: 0,
            methodCallers?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}