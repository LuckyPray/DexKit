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
import org.luckypray.dexkit.query.enums.UsingType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.NumberEncodeValueMatcher
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.query.wrap.DexMethod

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

    /**
     * The method descriptor.
     * ----------------
     * 方法描述符。
     *
     *     descriptor = "Ljava/lang/String;->length()I"
     */
    var descriptor: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            descriptor(value)
        }

    /**
     * The method name.
     * ----------------
     * 方法名。
     */
    var name: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            name(value)
        }

    /**
     * Method modifiers. Match using [java.lang.reflect.Modifier] mask bits,
     * default match type is contains, if you need to match exactly,
     * please use [modifiers] overloaded function.
     * ----------------
     * 方法修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配，
     * 默认匹配关系为包含，如果需要完全限定匹配请使用 [modifiers] 重载函数。
     *
     *     modifiers = Modifier.PUBLIC or Modifier.STATIC
     */
    var modifiers: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            modifiers(value)
        }

    /**
     * The method declared class fully qualified name.
     * ----------------
     * 方法声明类全限定名。
     *
     *     declaredClass = "Ljava/lang/String;"
     */
    var declaredClass: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            declaredClass(value)
        }

    /**
     * The method return type fully qualified name.
     * ----------------
     * 方法返回值类型全限定名。
     *
     *     returnType = "I"
     */
    var returnType: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            returnType(value)
        }

    /**
     * The method parameter types fully qualified name. If set to null,
     * it means matching any parameter type. The list implies the number of parameters.
     * ----------------
     * 方法参数类型全限定名。如果设置为 null 则表示匹配任意参数类型。列表隐含了参数数量
     *
     *     paramTypes = listOf(null, "java.lang.String")
     */
    var paramTypes: List<String?>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            paramTypes(value)
        }

    /**
     * The method parameter count.
     * ----------------
     * 方法参数数量。
     */
    var paramCount: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            paramCount(value)
        }

    /**
     * This method uses consecutive OP instructions.
     * ----------------
     * 该方法使用的连续的 OP 指令。
     *
     *     opCodes = listOf(0x12, 0x13, 0x14)
     */
    var opCodes: List<Int>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opCodes(value)
        }

    /**
     * This method uses consecutive smali instructions.
     * ----------------
     * 该方法使用的连续的 smali 指令名。
     *
     *     opNames = listOf("const-string", "const-string/jumbo")
     */
    var opNames: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            opNames(value)
        }

    /**
     * This method using numbers. To avoid floating point precision error,
     * the number is considered equal when the value error is less than 1e-6.
     * ----------------
     * 该方法使用的数字。为了避免浮点精度误差，数值误差小于 1e-6 时会被判定为相等。
     *
     *     usingNumbers = listOf(0.01, -1, 0.987, 0, 114514)
     */
    var usingNumbers: List<Number>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            usingNumbers(value)
        }

    /**
     * Using string list. Default match type is contains, if you need to match exactly,
     * please use [usingStrings] or [addUsingString] overloaded function for each string.
     * ----------------
     * 使用字符串列表。默认匹配关系为包含，如需为每个字符串设置匹配关系，
     * 请使用 [usingStrings] 或者 [addUsingString] 重载函数。
     */
    var usingStrings: List<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            usingStrings(value)
        }

    /**
     * The method descriptor, specifies a unique method.
     * ----------------
     * 方法描述符，用于指定唯一的方法。
     *
     *     descriptor("Ljava/lang/String;->length()I")
     *
     * @param descriptor method descriptor / 方法描述符
     * @return [MethodMatcher]
     */
    fun descriptor(descriptor: String) = also {
        val dexMethod = DexMethod(descriptor)
        name(dexMethod.name)
        declaredClass(dexMethod.className)
        returnType(dexMethod.returnTypeName)
        paramTypes(dexMethod.paramTypeNames)
    }

    /**
     * The method name matcher
     * ----------------
     * 方法名匹配器
     *
     *     name(StringMatcher().value("length"))
     *
     * @param name method name matcher / 方法名匹配器
     * @return [MethodMatcher]
     */
    fun name(name: StringMatcher) = also {
        this.nameMatcher = name
    }

    /**
     * The method name.
     * ----------------
     * 方法名。
     *
     *     name("length")
     *
     * @param name method name / 方法名
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) = also {
        this.nameMatcher = StringMatcher(name, StringMatchType.Equals, ignoreCase)
    }

    /**
     * Method modifiers matcher.
     * ----------------
     * 方法修饰符匹配器。
     *
     *     modifiers(AccessFlagsMatcher().modifiers(Modifier.PUBLIC or Modifier.STATIC))
     *
     * @param modifiers method modifiers matcher / 方法修饰符匹配器
     * @return [MethodMatcher]
     */
    fun modifiers(modifiers: AccessFlagsMatcher) = also {
        this.modifiersMatcher = modifiers
    }

    /**
     * Method modifiers. Match using [java.lang.reflect.Modifier] mask bits.
     * ----------------
     * 方法修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配。
     *
     *     modifiers(Modifier.PUBLIC or Modifier.STATIC)
     *
     * @param modifiers method modifiers / 方法修饰符
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun modifiers(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.modifiersMatcher = AccessFlagsMatcher(modifiers, matchType)
    }

    /**
     * The method declared class matcher.
     * ----------------
     * 方法声明类匹配器。
     *
     *     declaredClass(ClassMatcher().className("java.lang.String"))
     *
     * @param declaredClass method declared class matcher / 方法声明类匹配器
     * @return [MethodMatcher]
     */
    fun declaredClass(declaredClass: ClassMatcher) = also {
        this.classMatcher = declaredClass
    }

    /**
     * The method declared class fully qualified name.
     * ----------------
     * 方法声明类全限定名。
     *
     *     declaredClass("java.lang.String")
     *
     * @param className method declared class / 方法声明类
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun declaredClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.classMatcher = ClassMatcher().className(className, matchType, ignoreCase)
    }

    /**
     * The method return type matcher.
     * ----------------
     * 方法返回值类型匹配器。
     *
     *     returnType(ClassMatcher().descriptor("[I"))
     */
    fun returnType(type: ClassMatcher) = also {
        this.returnTypeMatcher = type
    }

    /**
     * The method return type fully qualified name.
     * ----------------
     * 方法返回值类型全限定名。
     *
     *     returnType("int[]")
     *
     * @param typeName method return type / 方法返回值类型
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun returnType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.returnTypeMatcher = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    /**
     * The method parameter types matcher.
     * ----------------
     * 方法参数类型匹配器。
     *
     *     params(ParametersMatcher().add(ParameterMatcher().type("int[]")))
     *
     * @param parameters method parameter types matcher / 方法参数类型匹配器
     * @return [MethodMatcher]
     */
    fun params(parameters: ParametersMatcher) = also {
        this.paramsMatcher = parameters
    }

    /**
     * The method parameter types fully qualified name. If set to null,
     * it means matching any parameter type. The list implies the number of parameters.
     * ----------------
     * 方法参数类型全限定名。如果设置为 null 则表示匹配任意参数类型。列表隐含了参数数量。
     *
     *     paramTypes(listOf(null, "java.lang.String"))
     */
    fun paramTypes(paramTypes: List<String?>) = also {
        this.paramsMatcher = ParametersMatcher().apply {
            paramTypes.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    /**
     * The method parameter types fully qualified name. If set to null,
     * it means matching any parameter type. The list implies the number of parameters.
     * ----------------
     * 方法参数类型全限定名。如果设置为 null 则表示匹配任意参数类型。列表隐含了参数数量。
     *
     *     paramTypes(null, "java.lang.String")
     *
     * @param paramTypes method parameter types / 方法参数类型
     * @return [MethodMatcher]
     */
    fun paramTypes(vararg paramTypes: String?) = also {
        this.paramsMatcher = ParametersMatcher().apply {
            params(listOf())
            paramTypes.forEach {
                val paramMatcher = it?.let { ParameterMatcher().type(it) }
                add(paramMatcher)
            }
        }
    }

    /**
     * Add method parameter type fully qualified name. If set to null,
     * it means matching any parameter type.
     * ----------------
     * 方法参数类型全限定名。如果设置为 null 则表示匹配任意参数类型。
     *
     *     addParamType("java.lang.String")
     *
     * @param paramType method parameter type / 方法参数类型
     * @return [MethodMatcher]
     */
    fun addParamType(paramType: String?) = also {
        paramsMatcher = paramsMatcher ?: ParametersMatcher()
        paramsMatcher!!.add(paramType?.let { ParameterMatcher().type(paramType) })
    }

    /**
     * The method parameter count.
     * ----------------
     * 方法参数数量。
     *
     * @param count method parameter count / 方法参数数量
     * @return [MethodMatcher]
     */
    fun paramCount(count: Int) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(count)
    }

    /**
     * The method parameter count range.
     * ----------------
     * 方法参数数量范围。
     *
     *     paramCount(IntRange(1, 3))
     *
     * @param range method parameter count range / 方法参数数量范围
     * @return [MethodMatcher]
     */
    fun paramCount(range: IntRange) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(range)
    }

    /**
     * The method parameter count range.
     * ----------------
     * 方法参数数量范围。
     *
     *     paramCount(1..3)
     *
     * @param range method parameter count range / 方法参数数量范围
     * @return [MethodMatcher]
     */
    fun paramCount(range: kotlin.ranges.IntRange) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(range)
    }

    /**
     * The method parameter count range.
     * ----------------
     * 方法参数数量范围。
     *
     *     paramCount(1, 3)
     *
     * @param min method parameter count min / 方法参数数量最小值
     * @param max method parameter count max / 方法参数数量最大值
     * @return [MethodMatcher]
     */
    fun paramCount(min: Int, max: Int) = also {
        this.paramsMatcher ?: let { this.paramsMatcher = ParametersMatcher() }
        this.paramsMatcher!!.count(min, max)
    }

    /**
     * The method annotations matcher.
     * ----------------
     * 方法注解匹配器。
     *
     *     annotations(AnnotationsMatcher().count(1))
     *
     * @param annotations annotations matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotationsMatcher = annotations
    }

    /**
     * Add method annotation matcher. only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 添加方法注解匹配器。仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     addAnnotation(AnnotationMatcher().type("org.luckypray.dexkit.demo.annotations.Router"))
     *
     * @param annotation annotation matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun addAnnotation(annotation: AnnotationMatcher) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.add(annotation)
    }

    /**
     * Method annotation count, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 方法注解数量，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     * @param count annotation count / 注解数量
     * @return [ClassMatcher]
     */
    fun annotationCount(count: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(count)
    }

    /**
     * Method annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 方法注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     annotationCount(IntRange(1, 2))
     *
     * @param range annotation count range / 注解数量范围
     * @return [ClassMatcher]
     */
    fun annotationCount(range: IntRange) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
    }

    /**
     * Method annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 方法注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     annotationCount(1..2)
     *
     * @param range annotation count range / 注解数量范围
     * @return [ClassMatcher]
     */
    fun annotationCount(range: kotlin.ranges.IntRange) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
    }

    /**
     * Method annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 方法注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     annotationCount(1, 2)
     *
     * @param min min annotation count / 最小注解数量
     * @param max max annotation count / 最大注解数量
     * @return [ClassMatcher]
     */
    fun annotationCount(min: Int, max: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(min, max)
    }

    /**
     * This method uses opcodes matcher
     * ----------------
     * 该方法使用的指令匹配器
     *
     *     opCodes(OpCodesMatcher().opCodes(listOf(0x12, 0x13, 0x14)))
     *
     * @param opCodes opcodes matcher / 指令匹配器
     * @return [MethodMatcher]
     */
    fun opCodes(opCodes: OpCodesMatcher) = also {
        this.opCodesMatcher = opCodes
    }

    /**
     * This method uses consecutive OP instructions.
     * ----------------
     * 该方法使用的连续的 OP 指令。
     *
     *     opCodes(listOf(0x12, 0x13, 0x14), OpCodeMatchType.StartsWith)
     *
     * @param opCodes opcodes / 指令
     * @param matchType match type / 匹配关系
     * @param opCodeSize opcodes size / 指令数量
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun opCodes(
        opCodes: List<Int>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher(opCodes, matchType, opCodeSize)
    }

    /**
     * This method uses consecutive smali instructions.
     * ----------------
     * 该方法使用的连续的 smali 指令名。
     *
     *     opNames(listOf("const-string", "const-string/jumbo"), OpCodeMatchType.StartsWith)
     *
     * @param opNames smali instruction names / smali 指令名
     * @param matchType match type / 匹配关系
     * @param opCodeSize opcodes size / 指令数量
     * @return [MethodMatcher]
     */
    @JvmOverloads
    fun opNames(
        opNames: List<String>,
        matchType: OpCodeMatchType = OpCodeMatchType.Contains,
        opCodeSize: IntRange? = null
    ) = also {
        this.opCodesMatcher = OpCodesMatcher.createForOpNames(opNames, matchType, opCodeSize)
    }

    /**
     * Using strings matcher.
     * ----------------
     * 使用字符串列表匹配器。
     *
     *     usingStrings(StringMatcherList().add(StringMatcher("string")))
     *
     * @param usingStrings using string list matcher / 使用字符串列表匹配器
     * @return [ClassMatcher]
     */
    fun usingStrings(usingStrings: StringMatcherList) = also {
        this.usingStringsMatcher = usingStrings
    }

    /**
     * Using strings matcher.
     * ----------------
     * 使用字符串匹配器。
     *
     *     usingStrings(List.of("TAG", "Activity"), StringMatchType.Equals, false)
     *
     * @param usingStrings using string list / 使用字符串列表
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun usingStrings(
        usingStrings: List<String>,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it, matchType, ignoreCase) }.toMutableList()
    }

    /**
     * Using strings matcher. default match type is contains, if you need to match exactly,
     * please use [usingStrings] or [addUsingString] overloaded function for each string.
     * ----------------
     * 使用字符串匹配器。默认匹配关系为包含，如需为每个字符串设置匹配关系，
     * 请使用 [usingStrings] 或者 [addUsingString] 重载函数。
     *
     *     usingStrings("TAG", "Activity")
     *
     * @param usingStrings using string list / 使用字符串列表
     * @return [ClassMatcher]
     */
    fun usingStrings(vararg usingStrings: String) = also {
        this.usingStringsMatcher = usingStrings.map { StringMatcher(it) }.toMutableList()
    }

    /**
     * Add using string matcher.
     * ----------------
     * 添加使用字符串的匹配器。
     *
     *     addUsingString(StringMatcher("string", StringMatchType.Equals, false))
     *
     * @param usingString using string matcher / 使用字符串匹配器
     * @return [ClassMatcher]
     */
    fun addUsingString(usingString: StringMatcher) = also {
        usingStringsMatcher = usingStringsMatcher ?: mutableListOf()
        usingStringsMatcher!!.add(usingString)
    }

    /**
     * Add using string.
     * ----------------
     * 添加使用字符串。
     *
     *     addUsingString("string", StringMatchType.Equals, false)
     *
     * @param usingString using string / 使用字符串
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun addUsingString(
        usingString: String,
        matchType: StringMatchType = StringMatchType.Contains,
        ignoreCase: Boolean = false
    ) = also {
        usingStringsMatcher = usingStringsMatcher ?: mutableListOf()
        usingStringsMatcher!!.add(StringMatcher(usingString, matchType, ignoreCase))
    }

    /**
     * Using fields matcher.
     * ----------------
     * 使用字段列表匹配器。
     *
     *     usingFields(UsingFieldMatcherList().add(UsingFieldMatcher().matcher(FieldMatcher().name("TAG"))))
     *
     * @param usingFields using fields matcher / 使用字段列表匹配器
     * @return [ClassMatcher]
     */
    fun usingFields(usingFields: List<UsingFieldMatcher>) = also {
        this.usingFieldsMatcher = usingFields.toMutableList()
    }

    /**
     * Using fields matcher.
     * ----------------
     * 使用字段匹配器。
     *
     *     usingFields(FieldMatcher().name("TAG"), UsingType.Any)
     *
     * @param usingField using field matcher / 使用字段匹配器
     * @param usingType using type / 使用类型
     * @return [ClassMatcher]
     */
    fun addUsingField(usingField: UsingFieldMatcher) = also {
        usingFieldsMatcher = usingFieldsMatcher ?: mutableListOf()
        usingFieldsMatcher!!.add(usingField)
    }

    /**
     * Using fields matcher.
     * ----------------
     * 使用字段匹配器。
     *
     *     usingFields(FieldMatcher().name("TAG"), UsingType.Any)
     *
     * @param usingField using field matcher / 使用字段匹配器
     * @param usingType using type / 使用类型
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun addUsingField(usingField: FieldMatcher, usingType: UsingType = UsingType.Any) = also {
        usingFieldsMatcher = usingFieldsMatcher ?: mutableListOf()
        usingFieldsMatcher!!.add(UsingFieldMatcher().apply {
            matcher(usingField)
            usingType(usingType)
        })
    }

    /**
     * Using numbers matcher.
     * ----------------
     * 使用数字列表匹配器。
     *
     *     usingNumbers(NumberEncodeValueMatcherList().add(NumberEncodeValueMatcher().value(114514)))
     *
     * @param usingNumbers using numbers matcher / 使用数字列表匹配器
     * @return [ClassMatcher]
     */
    fun usingNumbers(usingNumbers: NumberEncodeValueMatcherList) = also {
        this.usingNumbersMatcher = usingNumbers
    }

    /**
     * Using numbers matcher. To avoid floating point precision error,
     * the number is considered equal when the value error is less than 1e-6.
     * ----------------
     * 使用数字匹配器。为了避免浮点精度误差，数值误差小于 1e-6 时会被判定为相等。
     *
     *     usingNumbers(listOf(0.01, -1, 0.987, 0, 114514))
     *
     * @param usingNumbers using numbers / 使用数字列表
     * @return [ClassMatcher]
     */
    fun usingNumbers(usingNumbers: List<Number>) = also {
        this.usingNumbersMatcher = usingNumbers.map { NumberEncodeValueMatcher().value(it) }.toMutableList()
    }

    /**
     * Using numbers matcher. To avoid floating point precision error,
     * the number is considered equal when the value error is less than 1e-6.
     * ----------------
     * 使用数字匹配器。为了避免浮点精度误差，数值误差小于 1e-6 时会被判定为相等。
     *
     *     usingNumbers(0.01, -1, 0.987, 0, 114514)
     *
     * @param usingNumbers using numbers / 使用数字列表
     * @return [ClassMatcher]
     */
    fun usingNumbers(vararg usingNumbers: Number) = also {
        this.usingNumbersMatcher = usingNumbers.map { NumberEncodeValueMatcher().value(it) }.toMutableList()
    }

    /**
     * Add using number matcher. To avoid floating point precision error,
     * the number is considered equal when the value error is less than 1e-6.
     * ----------------
     * 添加使用数字匹配器。为了避免浮点精度误差，数值误差小于 1e-6 时会被判定为相等。
     *
     *     addUsingNumber(0.01)
     *
     * @param usingNumber using number / 使用的数字
     * @return [ClassMatcher]
     */
    fun addUsingNumber(usingNumber: Number) = also {
        usingNumbersMatcher = usingNumbersMatcher ?: mutableListOf()
        usingNumbersMatcher!!.add(NumberEncodeValueMatcher().value(usingNumber))
    }

    /**
     * The method invoke methods matcher.
     * ----------------
     * 方法调用方法匹配器。
     *
     *     invokeMethods(MethodsMatcher().add(MethodMatcher().name("length")))
     *
     * @param invokeMethods invoke methods matcher / 方法调用方法匹配器
     * @return [MethodMatcher]
     */
    fun invokeMethods(invokeMethods: MethodsMatcher) = also {
        this.invokeMethodsMatcher = invokeMethods
    }

    /**
     * Add method invoke method matcher.
     * ----------------
     * 添加方法调用方法匹配器。
     *
     *     addInvoke(MethodMatcher().name("length"))
     *
     * @param invokeMethod invoke method matcher / 方法调用方法匹配器
     * @return [MethodMatcher]
     */
    fun addInvoke(invokeMethod: MethodMatcher) = also {
        invokeMethodsMatcher = invokeMethodsMatcher ?: MethodsMatcher()
        invokeMethodsMatcher!!.add(invokeMethod)
    }

    /**
     * This method caller methods matcher.
     * ----------------
     * 该方法调用方法的匹配器。
     *
     *     callMethods(MethodsMatcher().add(MethodMatcher().name("length")))
     *
     * @param callMethods call methods matcher / 方法调用方法匹配器
     * @return [MethodMatcher]
     */
    fun callMethods(callMethods: MethodsMatcher) = also {
        this.callMethodsMatcher = callMethods
    }

    /**
     * Add method caller method matcher.
     * ----------------
     * 添加方法调用方法匹配器。
     *
     *     addCall(MethodMatcher().name("length"))
     *
     * @param callMethod call method matcher / 方法调用方法匹配器
     * @return [MethodMatcher]
     */
    fun addCall(callMethod: MethodMatcher) = also {
        callMethodsMatcher = callMethodsMatcher ?: MethodsMatcher()
        callMethodsMatcher!!.add(callMethod)
    }

    // region DSL

    /**
     * @see declaredClass
     */
    @kotlin.internal.InlineOnly
    inline fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        declaredClass(ClassMatcher().apply(init))
    }

    /**
     * @see returnType
     */
    @kotlin.internal.InlineOnly
    inline fun returnType(init: ClassMatcher.() -> Unit) = also {
        returnType(ClassMatcher().apply(init))
    }

    /**
     * @see params
     */
    @kotlin.internal.InlineOnly
    inline fun params(init: ParametersMatcher.() -> Unit) = also {
        params(ParametersMatcher().apply(init))
    }

    /**
     * @see annotations
     */
    @kotlin.internal.InlineOnly
    inline fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    /**
     * @see addAnnotation
     */
    @kotlin.internal.InlineOnly
    inline fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
    }

    /**
     * @see usingStrings
     */
    @kotlin.internal.InlineOnly
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    /**
     * @see usingFields
     */
    @kotlin.internal.InlineOnly
    inline fun usingFields(init: UsingFieldMatcherList.() -> Unit) = also {
        usingFields(UsingFieldMatcherList().apply(init))
    }

    /**
     * @see addUsingField
     */
    @kotlin.internal.InlineOnly
    inline fun addUsingField(init: UsingFieldMatcher.() -> Unit) = also {
        addUsingField(UsingFieldMatcher().apply(init))
    }

    /**
     * @see usingNumbers
     */
    @kotlin.internal.InlineOnly
    inline fun usingNumbers(init: NumberEncodeValueMatcherList.() -> Unit) = also {
        usingNumbers(NumberEncodeValueMatcherList().apply(init))
    }

    /**
     * @see invokeMethods
     */
    @kotlin.internal.InlineOnly
    inline fun invokeMethods(init: MethodsMatcher.() -> Unit) = also {
        invokeMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addInvoke
     */
    @kotlin.internal.InlineOnly
    inline fun addInvoke(init: MethodMatcher.() -> Unit) = also {
        addInvoke(MethodMatcher().apply(init))
    }

    /**
     * @see callMethods
     */
    @kotlin.internal.InlineOnly
    inline fun callMethods(init: MethodsMatcher.() -> Unit) = also {
        callMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addCall
     */
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
            usingStringsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            usingFieldsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0,
            usingNumbersMatcher?.map { it.type!!.value }?.toUByteArray()
                ?.let { InnerMethodMatcher.createUsingNumbersTypeVector(fbb, it) } ?: 0,
            usingNumbersMatcher?.map { it.value!!.build(fbb) }?.toIntArray()
                ?.let { InnerMethodMatcher.createUsingNumbersVector(fbb, it) } ?: 0,
            invokeMethodsMatcher?.build(fbb) ?: 0,
            callMethodsMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}