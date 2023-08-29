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
import org.luckypray.dexkit.util.OpCodeUtil

class MethodMatcher : BaseQuery() {
    var nameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var classMatcher: ClassMatcher? = null
        private set
    var returnTypeMatcher: ClassMatcher? = null
        private set
    var parameters: ParametersMatcher? = null
        private set
    var annotations: AnnotationsMatcher? = null
        private set
    var opCodesMatcher: OpCodesMatcher? = null
        private set
    var usingStringsMatcher: List<StringMatcher>? = null
        private set
    var usingFields: List<UsingFieldMatcher>? = null
        private set
    // TODO use Object?
    var usingNumbers: List<NumberEncodeValueMatcher>? = null
        private set
    var invokeMethods: MethodsMatcher? = null
        private set
    var callMethods: MethodsMatcher? = null
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
    var returnType: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError("Use returnType.className")
        @JvmSynthetic
        set(value) {
            returnType(value)
        }
    var parameterTypes: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError("Use parameters.parameterTypes")
        @JvmSynthetic
        set(value) {
            parameterTypes(value)
        }
    var opCodes: List<Int>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodesMatcher = opCodesMatcher ?: OpCodesMatcher()
            opCodesMatcher!!.opCodes = value
        }
    var opNames: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodesMatcher = opCodesMatcher ?: OpCodesMatcher()
            opCodesMatcher!!.opCodes = value.map { OpCodeUtil.getOpCode(it) }
        }
    var opMatchType: OpCodeMatchType
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodesMatcher = opCodesMatcher ?: OpCodesMatcher()
            opCodesMatcher!!.matchType = value
        }
    var opSize: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodesMatcher = opCodesMatcher ?: OpCodesMatcher()
            opCodesMatcher!!.sizeRange = IntRange(value)
        }
    var opRange: kotlin.ranges.IntRange
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodesMatcher = opCodesMatcher ?: OpCodesMatcher()
            opCodesMatcher!!.sizeRange = IntRange(value)
        }
    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            usingStrings(value)
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
        declaredClassName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.classMatcher = ClassMatcher().className(declaredClassName, matchType, ignoreCase)
    }

    fun returnType(returnType: ClassMatcher) = also {
        this.returnTypeMatcher = returnType
    }

    @JvmOverloads
    fun returnType(
        returnTypeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.returnTypeMatcher = ClassMatcher().className(returnTypeName, matchType, ignoreCase)
    }

    fun parameters(parameters: ParametersMatcher) = also {
        this.parameters = parameters
    }

    fun parameterTypes(parameterType: List<String?>) = also {
        this.parameters = ParametersMatcher().apply {
            parameterType.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    fun parameterTypes(vararg parameterTypes: String?) = also {
        this.parameters = ParametersMatcher().apply {
            parameters(listOf())
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
        this.parameters!!.apply { count(count) }
    }

    fun parameterCount(countRange: IntRange) = also {
        this.parameters ?: let { this.parameters = ParametersMatcher() }
        this.parameters!!.apply { range(countRange) }
    }

    fun parameterCount(range: kotlin.ranges.IntRange) = also {
        this.parameters ?: let { this.parameters = ParametersMatcher() }
        this.parameters!!.apply { range(range) }
    }

    fun parameterCount(min: Int, max: Int) = also {
        this.parameters ?: let { this.parameters = ParametersMatcher() }
        this.parameters!!.apply { range(min, max) }
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
        this.annotations!!.count(count)
    }

    fun annotationCount(range: IntRange) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.range(range)
    }

    fun annotationCount(range: kotlin.ranges.IntRange) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.range(range)
    }

    fun annotationCount(min: Int, max: Int) = also {
        this.annotations = this.annotations ?: AnnotationsMatcher()
        this.annotations!!.range(min, max)
    }

    fun opCodes(opCodes: OpCodesMatcher) = also {
        this.opCodesMatcher = opCodes
    }

    fun opCodes(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher(opCodes, matchType, opCodeSize)
    }

    fun opNames(
        opNames: List<String>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher.createForOpNames(opNames, matchType, opCodeSize)
    }

    fun usingStringsMatcher(usingStrings: List<StringMatcher>) = also {
        this.usingStringsMatcher = usingStrings
    }

    fun usingStrings(usingStrings: List<String>) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }
    }

    fun usingStrings(vararg usingStrings: String) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }
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
    inline fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
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
            nameMatcher?.build(fbb) ?: 0,
            modifiersMatcher?.build(fbb) ?: 0,
            classMatcher?.build(fbb) ?: 0,
            returnTypeMatcher?.build(fbb) ?: 0,
            parameters?.build(fbb) ?: 0,
            annotations?.build(fbb) ?: 0,
            opCodesMatcher?.build(fbb) ?: 0,
            usingStringsMatcher?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
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