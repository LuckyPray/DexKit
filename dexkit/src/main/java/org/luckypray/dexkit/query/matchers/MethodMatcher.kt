@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerMethodMatcher
import org.luckypray.dexkit.query.NumberEncodeValueMatcherList
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.UsingFieldMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.OpCodeMatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.NumberEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher

class MethodMatcher : BaseQuery() {
    private var name: StringMatcher? = null
    private var modifiers: AccessFlagsMatcher? = null
    private var declaredClass: ClassMatcher? = null
    private var returnType: ClassMatcher? = null
    private var parameters: ParametersMatcher? = null
    private var annotations: AnnotationsMatcher? = null
    private var opCodes: OpCodesMatcher? = null
    private var usingStrings: List<StringMatcher>? = null
    private var usingFields: List<UsingFieldMatcher>? = null
    private var usingNumbers: List<NumberEncodeValueMatcher>? = null
    private var invokeMethods: MethodsMatcher? = null
    private var callMethods: MethodsMatcher? = null

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
        declaredClassName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.declaredClass = ClassMatcher().className(declaredClassName, matchType, ignoreCase)
    }

    fun returnType(returnType: ClassMatcher) = also {
        this.returnType = returnType
    }

    @JvmOverloads
    fun returnType(
        returnTypeName: String,
        matchType: StringMatchType = StringMatchType.Equal,
        ignoreCase: Boolean = false
    ) = also {
        this.returnType = ClassMatcher().className(returnTypeName, matchType, ignoreCase)
    }

    fun parameters(parameters: ParametersMatcher) = also {
        this.parameters = parameters
    }

    fun parameterTypes(vararg parameterTypes: String?) = also {
        this.parameters = ParametersMatcher().apply {
            parameterTypes.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    fun addParameterType(parameterType: String?) = also {
        parameters = parameters ?: ParametersMatcher()
        parameters!!.add(parameterType?.let { ParameterMatcher().type(parameterType) })
    }

    fun parameterCount(count: Int) = also {
        this.parameters ?: let { this.parameters = ParametersMatcher() }
        this.parameters!!.apply { countRange(count) }
    }

    fun parameterCount(min: Int, max: Int) = also {
        this.parameters ?: let { this.parameters = ParametersMatcher() }
        this.parameters!!.apply { countRange(min, max) }
    }

    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotations = annotations
    }

    fun opCodes(opCodes: OpCodesMatcher) = also {
        this.opCodes = opCodes
    }

    fun opCodes(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodes = OpCodesMatcher(opCodes, matchType, opCodeSize)
    }

    fun opNames(
        opNames: List<String>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
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

    fun usingFields(usingFields: List<UsingFieldMatcher>) = also {
        this.usingFields = usingFields
    }

    fun usingNumbers(usingNumbers: List<NumberEncodeValueMatcher>) = also {
        this.usingNumbers = usingNumbers
    }

    fun invokeMethods(invokeMethods: MethodsMatcher) = also {
        this.invokeMethods = invokeMethods
    }

    fun addInvoke(invokeMethod: MethodMatcher) = also {
        invokeMethods = invokeMethods ?: MethodsMatcher()
        invokeMethods!!.add(invokeMethod)
    }

    fun callMethods(callMethods: MethodsMatcher) = also {
        this.callMethods = callMethods
    }

    fun addCall(callMethod: MethodMatcher) = also {
        callMethods = callMethods ?: MethodsMatcher()
        callMethods!!.add(callMethod)
    }

    // region DSL

    
    @kotlin.internal.InlineOnly
    inline fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        declaredClass(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun returnType(init: ClassMatcher.() -> Unit) = also {
        returnType(ClassMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun parameters(init: ParametersMatcher.() -> Unit) = also {
        parameters(ParametersMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun usingStringsMatcher(init: StringMatcherList.() -> Unit) = also {
        usingStringsMatcher(StringMatcherList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun usingFields(init: UsingFieldMatcherList.() -> Unit) = also {
        usingFields(UsingFieldMatcherList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun usingNumbers(init: NumberEncodeValueMatcherList.() -> Unit) = also {
        usingNumbers(NumberEncodeValueMatcherList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun invokeMethods(init: MethodsMatcher.() -> Unit) = also {
        invokeMethods(MethodsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addInvoke(init: MethodMatcher.() -> Unit) = also {
        addInvoke(MethodMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun callMethods(init: MethodsMatcher.() -> Unit) = also {
        callMethods(MethodsMatcher().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addCall(init: MethodMatcher.() -> Unit) = also {
        addCall(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = MethodMatcher()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerMethodMatcher.createMethodMatcher(
            fbb,
            name?.build(fbb) ?: 0,
            modifiers?.build(fbb) ?: 0,
            declaredClass?.build(fbb) ?: 0,
            returnType?.build(fbb) ?: 0,
            parameters?.build(fbb) ?: 0,
            annotations?.build(fbb) ?: 0,
            opCodes?.build(fbb) ?: 0,
            usingStrings?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            usingFields?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            usingNumbers?.map { it.type!!.value }?.let { InnerMethodMatcher.createUsingNumbersTypeVector(fbb, it.toUByteArray()) } ?: 0,
            usingNumbers?.map { it.value!!.build(fbb) }?.let { InnerMethodMatcher.createUsingNumbersVector(fbb, it.toIntArray()) } ?: 0,
            invokeMethods?.build(fbb) ?: 0,
            callMethods?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}