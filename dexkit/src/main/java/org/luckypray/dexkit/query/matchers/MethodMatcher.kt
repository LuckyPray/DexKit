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
    var nameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var classMatcher: ClassMatcher? = null
        private set
    var returnTypeMatcher: ClassMatcher? = null
        private set
    var paramsMatcher: ParametersMatcher? = null
        private set
    var annotationsMatcher: AnnotationsMatcher? = null
        private set
    var opCodesMatcher: OpCodesMatcher? = null
        private set
    var usingStringsMatcher: MutableList<StringMatcher>? = null
        private set
    var usingFieldsMatcher: MutableList<UsingFieldMatcher>? = null
        private set
    var usingNumbersMatcher: MutableList<NumberEncodeValueMatcher>? = null
        private set
    var invokeMethodsMatcher: MethodsMatcher? = null
        private set
    var callMethodsMatcher: MethodsMatcher? = null
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
            modifiers(value)
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
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            returnType(value)
        }
    var paramTypes: List<String?>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            paramTypes(value)
        }
    var paramCount: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            paramCount(value)
        }
    var opCodes: List<Int>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodes(value)
        }
    var opNames: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opNames(value)
        }
    var usingNumbers: List<Number>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            usingNumbers(value)
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
    fun modifiers(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) = also {
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

    fun params(parameters: ParametersMatcher) = also {
        this.paramsMatcher = parameters
    }

    fun paramTypes(paramTypes: List<String?>) = also {
        this.paramsMatcher = ParametersMatcher().apply {
            paramTypes.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    fun paramTypes(vararg paramTypes: String?) = also {
        this.paramsMatcher = ParametersMatcher().apply {
            params(listOf())
            paramTypes.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    fun addParamType(paramType: String?) = also {
        paramsMatcher = paramsMatcher ?: ParametersMatcher()
        paramsMatcher!!.add(paramType?.let { ParameterMatcher().type(paramType) })
    }

    fun paramCount(count: Int) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(count)
    }

    fun paramCount(range: IntRange) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(range)
    }

    fun paramCount(range: kotlin.ranges.IntRange) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(range)
    }

    fun paramCount(min: Int, max: Int) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(min, max)
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

    fun opCodes(opCodes: OpCodesMatcher) = also {
        this.opCodesMatcher = opCodes
    }

    @JvmOverloads
    fun opCodes(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher(opCodes, matchType, opCodeSize)
    }

    @JvmOverloads
    fun opNames(
        opNames: List<String>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher.createForOpNames(opNames, matchType, opCodeSize)
    }

    fun usingStrings(usingStrings: StringMatcherList) = also {
        this.usingStringsMatcher = usingStrings
    }

    @JvmOverloads
    fun usingStrings(
        usingStrings: List<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it, matchType, ignoreCase) }.toMutableList()
    }

    fun usingStrings(vararg usingStrings: String) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }.toMutableList()
    }

    fun addUsingString(usingString: StringMatcher) = also {
        usingStringsMatcher = usingStringsMatcher ?: mutableListOf()
        usingStringsMatcher!!.add(usingString)
    }

    @JvmOverloads
    fun addUsingString(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        usingStringsMatcher = usingStringsMatcher ?: mutableListOf()
        usingStringsMatcher!!.add(StringMatcher(usingString, matchType, ignoreCase))
    }

    fun usingFields(usingFields: List<UsingFieldMatcher>) = also {
        this.usingFieldsMatcher = usingFields.toMutableList()
    }

    fun addUsingField(usingField: UsingFieldMatcher) = also {
        usingFieldsMatcher = usingFieldsMatcher ?: mutableListOf()
        usingFieldsMatcher!!.add(usingField)
    }

    fun usingNumbers(usingNumbers: NumberEncodeValueMatcherList) = also {
        this.usingNumbersMatcher = usingNumbers
    }

    fun usingNumbers(usingNumbers: List<Number>) = also {
        this.usingNumbersMatcher = usingNumbers.map { NumberEncodeValueMatcher().value(it) }.toMutableList()
    }

    fun usingNumbers(vararg usingNumbers: Number) = also {
        this.usingNumbersMatcher = usingNumbers.map { NumberEncodeValueMatcher().value(it) }.toMutableList()
    }

    fun addUsingNumber(usingNumber: Number) = also {
        usingNumbersMatcher = usingNumbersMatcher ?: mutableListOf()
        usingNumbersMatcher!!.add(NumberEncodeValueMatcher().value(usingNumber))
    }

    fun invokeMethods(invokeMethods: MethodsMatcher) = also {
        this.invokeMethodsMatcher = invokeMethods
    }

    fun addInvoke(invokeMethod: MethodMatcher) = also {
        invokeMethodsMatcher = invokeMethodsMatcher ?: MethodsMatcher()
        invokeMethodsMatcher!!.add(invokeMethod)
    }

    fun callMethods(callMethods: MethodsMatcher) = also {
        this.callMethodsMatcher = callMethods
    }

    fun addCall(callMethod: MethodMatcher) = also {
        callMethodsMatcher = callMethodsMatcher ?: MethodsMatcher()
        callMethodsMatcher!!.add(callMethod)
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
    inline fun params(init: ParametersMatcher.() -> Unit) = also {
        params(ParametersMatcher().apply(init))
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
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun usingFields(init: UsingFieldMatcherList.() -> Unit) = also {
        usingFields(UsingFieldMatcherList().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun addUsingField(init: UsingFieldMatcher.() -> Unit) = also {
        addUsingField(UsingFieldMatcher().apply(init))
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
            paramsMatcher?.build(fbb) ?: 0,
            annotationsMatcher?.build(fbb) ?: 0,
            opCodesMatcher?.build(fbb) ?: 0,
            usingStringsMatcher?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            usingFieldsMatcher?.let { fbb.createVectorOfTables(it.map { it.build(fbb) }.toIntArray()) } ?: 0,
            usingNumbersMatcher?.map { it.type!!.value }?.let { InnerMethodMatcher.createUsingNumbersTypeVector(fbb, it.toUByteArray()) } ?: 0,
            usingNumbersMatcher?.map { it.value!!.build(fbb) }?.let { InnerMethodMatcher.createUsingNumbersVector(fbb, it.toIntArray()) } ?: 0,
            invokeMethodsMatcher?.build(fbb) ?: 0,
            callMethodsMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}