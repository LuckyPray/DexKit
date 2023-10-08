@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerClassMatcher
import org.luckypray.dexkit.query.StringMatcherList
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.util.DexSignUtil
import org.luckypray.dexkit.wrap.DexClass

class ClassMatcher : BaseQuery, IAnnotationEncodeValue {
    var sourceMatcher: StringMatcher? = null
        private set
    var classNameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var superClassMatcher: ClassMatcher? = null
        private set
    var interfacesMatcher: InterfacesMatcher? = null
        private set
    var annotationsMatcher: AnnotationsMatcher? = null
        private set
    var fieldsMatcher: FieldsMatcher? = null
        private set
    var methodsMatcher: MethodsMatcher? = null
        private set
    var usingStringsMatcher: MutableList<StringMatcher>? = null
        private set

    constructor()

    constructor(clazz: Class<*>) {
        className(DexSignUtil.getClassDescriptor(clazz))
    }

    constructor(descriptor: String) {
        descriptor(descriptor)
    }

    /**
     * The class descriptor, specifies a unique class.
     * ----------------
     * 类描述符，指定唯一的类。
     *
     *     descriptor = "Lorg/luckypray/dexkit/demo/MainActivity;"
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
     * The class source file name, if smali exists `.source`, for example: `.source "MainActivity.java"`.
     * ----------------
     * 类源文件名，如果 smali 存在 `.source`，例如：`.source "MainActivity.java"`。
     *
     *     source = "MainActivity.java"
     */
    var source: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            source(value)
        }

    /**
     * Fully qualified class name.
     * ----------------
     * 完全限定类名。
     *
     *     className = "org.luckypray.dexkit.demo.MainActivity"
     *     className = "org/luckypray/dexkit/demo/MainActivity"
     */
    var className: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            className(value)
        }

    /**
     * Class modifiers. Match using [java.lang.reflect.Modifier] mask bits,
     * default match type is contains, if you need to match exactly,
     * please use [modifiers] overloaded function.
     * ----------------
     * 类修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配，
     * 默认匹配关系为包含，如果需要完全限定匹配请使用 [modifiers] 重载函数。
     *
     * java:
     *
     *     modifiers = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL
     *
     * kotlin:
     *
     *     modifiers = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL
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
     * Fully qualified super class name.
     * ----------------
     * 完全限定的父类名。
     *
     *     superClass = "androidx.appcompat.app.AppCompatActivity"
     *     superClass = "androidx/appcompat/app/AppCompatActivity"
     */
    var superClass: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            superClass(value)
        }

    /**
     * Using string list. Default match type is contains, if you need to match exactly,
     * please use [usingStrings] or [addUsingString] overloaded function for each string.
     * ----------------
     * 使用字符串列表。默认匹配关系为包含，如需为每个字符串设置匹配关系，
     * 请使用 [usingStrings] 或者 [addUsingString] 重载函数。
     */
    var usingStrings: Collection<String>
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        set(value) {
            usingStrings(value)
        }

    /**
     * The class descriptor, specifies a unique class.
     * ----------------
     * 类描述符，指定唯一的类。
     *
     *     descriptor("Lorg/luckypray/dexkit/demo/MainActivity;")
     *
     * @param descriptor class descriptor / 类描述符
     * @return [ClassMatcher]
     */
    fun descriptor(descriptor: String) = also {
        val dexClass = DexClass(descriptor)
        className(dexClass.typeName)
    }

    /**
     * The class source file name matcher.
     * ----------------
     * 类源文件名匹配器。
     *
     *     source(StringMatcher("MainActivity.java", StringMatchType.Equals, false))
     */
    fun source(matcher: StringMatcher) = also {
        this.sourceMatcher = matcher
    }

    /**
     * The class source file name, if smali exists `.source`, for example: `.source "MainActivity.java"`.
     * ----------------
     * 类源文件名，如果 smali 存在 `.source`，例如：`.source "MainActivity.java"`。
     *
     *     source("MainActivity.java", StringMatchType.Equals, false)
     *
     * @param source source file name / 源文件名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun source(
        source: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.sourceMatcher = StringMatcher(source, matchType, ignoreCase)
    }

    /**
     * Class name string matcher.
     * ----------------
     * 类名字符串匹配器。
     *
     *     className(StringMatcher("org.luckypray.dexkit.demo.MainActivity", StringMatchType.Equals, false))
     *
     * @param matcher string matcher / 字符串匹配器
     * @return [ClassMatcher]
     */
    fun className(matcher: StringMatcher) = also {
        this.classNameMatcher = matcher
    }

    /**
     * Class name string matcher.
     * ----------------
     * 类名字符串匹配器。
     *
     *     className("org.luckypray.dexkit.demo.MainActivity", StringMatchType.Equals, false)
     *
     * @param className class name / 类名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun className(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.classNameMatcher = StringMatcher(className, matchType, ignoreCase)
    }

    /**
     * Class modifiers matcher.
     * ----------------
     * 类修饰符匹配器。
     *
     *     modifiers(AccessFlagsMatcher(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, MatchType.Equals))
     *
     * @param matcher modifiers matcher / 修饰符匹配器
     * @return [ClassMatcher]
     */
    fun modifiers(matcher: AccessFlagsMatcher) = also {
        this.modifiersMatcher = matcher
    }

    /**
     * Class modifiers. Match using [java.lang.reflect.Modifier] mask bits.
     * ----------------
     * 类修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配。
     *
     *     modifiers(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL, MatchType.Equals)
     *
     * @param modifiers [java.lang.reflect.Modifier] mask bits / [java.lang.reflect.Modifier] 的常量位或值
     * @param matchType match type / 匹配类型
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun modifiers(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.modifiersMatcher = AccessFlagsMatcher(modifiers, matchType)
    }

    /**
     * Super class matcher.
     * ----------------
     * 父类匹配器。
     *
     *     superClass(ClassMatcher.create().className("androidx.appcompat.app.AppCompatActivity"))
     *
     * @param superClass super class matcher / 父类匹配器
     * @return [ClassMatcher]
     */
    fun superClass(superClass: ClassMatcher) = also {
        this.superClassMatcher = superClass
    }

    /**
     * Super class name string matcher.
     * ----------------
     * 父类名字符串匹配器。
     *
     *     superClass("androidx.appcompat.app.AppCompatActivity", StringMatchType.Equals, false)
     *
     * @param className super class name / 父类名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun superClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.superClassMatcher = ClassMatcher().className(StringMatcher(className, matchType, ignoreCase))
    }

    /**
     * Class implements interfaces matcher.
     * ----------------
     * 类实现的接口匹配器。
     *
     *     interfaces(InterfacesMatcher().add(ClassMatcher().className("android.view.View")))
     *
     * @param interfaces interfaces matcher / 接口匹配器
     * @return [ClassMatcher]
     */
    fun interfaces(interfaces: InterfacesMatcher) = also {
        this.interfacesMatcher = interfaces
    }

    /**
     * Add class implements interface matcher.
     * ----------------
     * 添加类实现接口的匹配器。
     *
     *     addInterface(ClassMatcher().className("android.view.View"))
     *
     * @param interfaceMatcher interfaces matcher / 接口匹配器
     * @return [ClassMatcher]
     */
    fun addInterface(interfaceMatcher: ClassMatcher) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.add(interfaceMatcher)
    }

    /**
     * Add class implements interface name matcher.
     * ----------------
     * 添加类实现接口的名的匹配器。
     *
     *     addInterface("android.view.View", StringMatchType.Equals, false)
     *
     * @param className interface class name / 接口类名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun addInterface(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.add(ClassMatcher().className(className, matchType, ignoreCase))
    }

    /**
     * Class implements interface count.
     * ----------------
     * 类实现的接口数量。
     *
     *     interfaceCount(1)
     *
     * @param count interface count / 接口数量
     * @return [ClassMatcher]
     */
    fun interfaceCount(count: Int) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(count)
    }

    /**
     * Class implements interface count range.
     * ----------------
     * 类实现的接口数量范围。
     *
     *     interfaceCount(IntRange(1, 2))
     *
     * @param range interface count range / 接口数量范围
     * @return [ClassMatcher]
     */
    fun interfaceCount(range: IntRange) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(range)
    }

    /**
     * Class implements interface count range.
     * ----------------
     * 类实现的接口数量范围。
     *
     *     interfaceCount(1..2)
     *
     * @param range interface count range / 接口数量范围
     * @return [ClassMatcher]
     */
    fun interfaceCount(range: kotlin.ranges.IntRange) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(range)
    }

    /**
     * Class implements interface count range.
     * ----------------
     * 类实现的接口数量范围。
     *
     *     interfaceCount(1, 2)
     *
     * @param min min interface count / 最小接口数量
     * @param max max interface count / 最大接口数量
     * @return [ClassMatcher]
     */
    fun interfaceCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.interfacesMatcher = this.interfacesMatcher ?: InterfacesMatcher()
        this.interfacesMatcher!!.count(min, max)
    }


    /**
     * Class declared annotations matcher.
     * ----------------
     * 类上声明的注解匹配器。
     *
     *     annotations(AnnotationsMatcher().add(AnnotationMatcher().type("org.luckypray.dexkit.demo.Annotation")))
     *
     * @param annotations annotations matcher / 注解匹配器
     * @return [ClassMatcher]
     */
    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotationsMatcher = annotations
    }

    /**
     * Add class annotation matcher. only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 添加类注解的匹配器。仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     addAnnotation(AnnotationMatcher().type("org.luckypray.dexkit.demo.Annotation"))
     *
     * @param annotationMatcher annotation matcher / 注解匹配器
     * @return [ClassMatcher]
     */
    fun addAnnotation(annotationMatcher: AnnotationMatcher) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.add(annotationMatcher)
    }

    /**
     * Class annotation count, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 类注解数量，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     * @param count annotation count / 注解数量
     * @return [ClassMatcher]
     */
    fun annotationCount(count: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(count)
    }

    /**
     * Class annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 类注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
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
     * Class annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 类注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
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
     * Class annotation count range, only contains non-system annotations.
     * That is, annotations that are not declared as `.annotation system` in smali.
     * ----------------
     * 类注解数量范围，仅包含非系统注解。即 smali 中非 `.annotation system` 声明的注解。
     *
     *     annotationCount(1, 2)
     *
     * @param min min annotation count / 最小注解数量
     * @param max max annotation count / 最大注解数量
     * @return [ClassMatcher]
     */
    fun annotationCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(min, max)
    }

    /**
     * Class declared fields matcher.
     * ----------------
     * 类中声明的字段列表匹配器。
     *
     *     fields(FieldsMatcher().add(FieldMatcher().name("field")))
     *
     * @param fields fields matcher / 字段匹配器
     * @return [ClassMatcher]
     */
    fun fields(fields: FieldsMatcher) = also {
        this.fieldsMatcher = fields
    }

    /**
     * Add class field matcher.
     * ----------------
     * 添加类字段的匹配器。
     *
     *     addField(FieldMatcher().name("field"))
     *
     * @param fieldMatcher field matcher / 字段匹配器
     * @return [ClassMatcher]
     */
    fun addField(fieldMatcher: FieldMatcher) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.add(fieldMatcher)
    }

    /**
     * Add class field name matcher.
     * ----------------
     * 添加类字段的名的匹配器。
     *
     *     addField("field", false)
     *
     * @param fieldName field name / 字段名
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun addFieldForName(
        fieldName: String,
        ignoreCase: Boolean = false
    ) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.add(FieldMatcher().name(fieldName, ignoreCase))
    }

    /**
     * Add class field type matcher.
     * ----------------
     * 添加类字段的类型的匹配器。
     *
     *     addFieldForType("org.luckypray.dexkit.demo.Annotation", StringMatchType.Equals, false)
     *
     * @param typeName field type name / 字段类型名
     * @param matchType string match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ClassMatcher]
     */
    @JvmOverloads
    fun addFieldForType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.add(FieldMatcher().type(typeName, matchType, ignoreCase))
    }

    /**
     * Class field count.
     * ----------------
     * 类字段数量。
     *
     * @param count field count / 字段数量
     * @return [ClassMatcher]
     */
    fun fieldCount(count: Int) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(count)
    }

    /**
     * Class field count range.
     * ----------------
     * 类字段数量范围。
     *
     *     fieldCount(IntRange(1, 2))
     *
     * @param range field count range / 字段数量范围
     * @return [ClassMatcher]
     */
    fun fieldCount(range: IntRange) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(range)
    }

    /**
     * Class field count range.
     * ----------------
     * 类字段数量范围。
     *
     *     fieldCount(1..2)
     *
     * @param range field count range / 字段数量范围
     * @return [ClassMatcher]
     */
    fun fieldCount(range: kotlin.ranges.IntRange) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(range)
    }

    /**
     * Class field count range.
     * ----------------
     * 类字段数量范围。
     *
     *     fieldCount(1, 2)
     *
     * @param min min field count / 最小字段数量
     * @param max max field count / 最大字段数量
     * @return [ClassMatcher]
     */
    fun fieldCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.fieldsMatcher = this.fieldsMatcher ?: FieldsMatcher()
        this.fieldsMatcher!!.count(min, max)
    }

    /**
     * Methods matcher, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 方法列表匹配器，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     *     methods(MethodsMatcher().add(MethodMatcher().name("method")))
     *
     * @param methods methods matcher / 方法匹配器
     * @return [ClassMatcher]
     */
    fun methods(methods: MethodsMatcher) = also {
        this.methodsMatcher = methods
    }

    /**
     * Add class method matcher, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 添加类方法的匹配器，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     *     addMethod(MethodMatcher().name("method"))
     *
     * @param methodMatcher method matcher / 方法匹配器
     * @return [ClassMatcher]
     */
    fun addMethod(methodMatcher: MethodMatcher) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.add(methodMatcher)
    }

    /**
     * Class method count, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 类方法数量，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     * @param count method count / 方法数量
     * @return [ClassMatcher]
     */
    fun methodCount(count: Int) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(count)
    }

    /**
     * Class method count range, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 类方法数量范围，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     *     methodCount(IntRange(1, 2))
     *
     * @param range method count range / 方法数量范围
     * @return [ClassMatcher]
     */
    fun methodCount(range: IntRange) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(range)
    }

    /**
     * Class method count range, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 类方法数量范围，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     *     methodCount(1..2)
     *
     * @param range method count range / 方法数量范围
     * @return [ClassMatcher]
     */
    fun methodCount(range: kotlin.ranges.IntRange) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(range)
    }

    /**
     * Class method count range, contains constructor (method name: `<init>`) and static block (method name: `<clinit>`).
     * ----------------
     * 类方法数量范围，包含构造函数（方法名：<init>）以及静态代码块（方法名: `<clinit>`）。
     *
     *     methodCount(1, 2)
     *
     * @param min min method count / 最小方法数量
     * @param max max method count / 最大方法数量
     * @return [ClassMatcher]
     */
    fun methodCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.methodsMatcher = this.methodsMatcher ?: MethodsMatcher()
        this.methodsMatcher!!.count(min, max)
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
        usingStrings: Collection<String>,
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
        addUsingString(StringMatcher(usingString, matchType, ignoreCase))
    }

    // region DSL

    /**
     * @see superClass
     */
    @kotlin.internal.InlineOnly
    inline fun superClass(init: ClassMatcher.() -> Unit) = also {
        superClass(ClassMatcher().apply(init))
    }

    /**
     * @see interfaces
     */
    @kotlin.internal.InlineOnly
    inline fun interfaces(init: InterfacesMatcher.() -> Unit) = also {
        interfaces(InterfacesMatcher().apply(init))
    }

    /**
     * @see addInterface
     */
    @kotlin.internal.InlineOnly
    inline fun addInterface(init: ClassMatcher.() -> Unit) = also {
        addInterface(ClassMatcher().apply(init))
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
     * @see fields
     */
    @kotlin.internal.InlineOnly
    inline fun fields(init: FieldsMatcher.() -> Unit) = also {
        fields(FieldsMatcher().apply(init))
    }

    /**
     * @see addField
     */
    @kotlin.internal.InlineOnly
    inline fun addField(init: FieldMatcher.() -> Unit) = also {
        addField(FieldMatcher().apply(init))
    }

    /**
     * @see methods
     */
    @kotlin.internal.InlineOnly
    inline fun methods(init: MethodsMatcher.() -> Unit) = also {
        methods(MethodsMatcher().apply(init))
    }

    /**
     * @see addMethod
     */
    @kotlin.internal.InlineOnly
    inline fun addMethod(init: MethodMatcher.() -> Unit) = also {
        addMethod(MethodMatcher().apply(init))
    }

    /**
     * @see usingStrings
     */
    @kotlin.internal.InlineOnly
    inline fun usingStrings(init: StringMatcherList.() -> Unit) = also {
        usingStrings(StringMatcherList().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = ClassMatcher()

        @JvmStatic
        fun create(clazz: Class<*>) = ClassMatcher(clazz)

        /**
         * @see ClassMatcher.descriptor
         */
        @JvmStatic
        fun create(descriptor: String) = ClassMatcher(descriptor)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (classNameMatcher?.value?.isEmpty() == true) {
            throw IllegalStateException("className not be empty")
        }
        val root = InnerClassMatcher.createClassMatcher(
            fbb,
            sourceMatcher?.build(fbb) ?: 0,
            classNameMatcher?.build(fbb) ?: 0,
            modifiersMatcher?.build(fbb) ?: 0,
            superClassMatcher?.build(fbb) ?: 0,
            interfacesMatcher?.build(fbb) ?: 0,
            annotationsMatcher?.build(fbb) ?: 0,
            fieldsMatcher?.build(fbb) ?: 0,
            methodsMatcher?.build(fbb) ?: 0,
            usingStringsMatcher?.map { it.build(fbb) }?.toIntArray()
                ?.let { fbb.createVectorOfTables(it) } ?: 0
        )
        fbb.finish(root)
        return root
    }
}