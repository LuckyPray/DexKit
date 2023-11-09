/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerUsingFieldMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.UsingType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.wrap.DexField
import java.lang.reflect.Field

class UsingFieldMatcher : BaseQuery {
    var matcher: FieldMatcher? = null
        private set

    /**
     * Using type. Default is [UsingType.Any].
     */
    @set:JvmSynthetic
    var usingType: UsingType = UsingType.Any

    constructor()
    constructor(field: Field, usingType: UsingType = UsingType.Any) {
        this.matcher = FieldMatcher(field)
        this.usingType = usingType
    }
    constructor(fieldDescriptor: String, usingType: UsingType = UsingType.Any) {
        this.matcher = FieldMatcher(fieldDescriptor)
        this.usingType = usingType
    }

    /**
     * The field descriptor.
     * ----------------
     * 字段描述符。
     *
     *     descriptor = "Lorg/luckypray/dexkit/demo/MainActivity;->mText:Ljava/lang/String;"
     */
    var fieldDescriptor: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            fieldDescriptor(value)
        }

    /**
     * The field name.
     * ----------------
     * 字段名称。
     */
    var fieldName: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            fieldName(value)
        }

    /**
     * Field modifiers. Match using [java.lang.reflect.Modifier] mask bits,
     * default match type is contains, if you need to match exactly,
     * please use [fieldModifiers] overloaded function.
     * ----------------
     * 字段修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配，
     * 默认匹配关系为包含，如果需要完全限定匹配请使用 [fieldModifiers] 重载函数。
     *
     *     modifiers = Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL
     */
    var fieldModifiers: Int
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            fieldModifiers(value)
        }

    /**
     * The field declared class fully qualified name.
     * ----------------
     * 字段声明类的完全限定名。
     *
     *     declaredClass = "org.luckypray.dexkit.demo.MainActivity"
     */
    var fieldDeclaredClass: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            fieldDeclaredClass(value)
        }

    /**
     * The field type fully qualified name.
     * ----------------
     * 字段类型的完全限定名。
     *
     *     type = "java.lang.String"
     */
    var fieldType: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            fieldType(value)
        }

    /**
     * Need to match field.
     * ----------------
     * 要匹配的字段
     *
     * @param matcher field / 字段
     * @return [UsingFieldMatcher]
     */
    fun field(matcher: FieldMatcher) = also {
        this.matcher = matcher
    }

    /**
     * Using type.
     * ----------------
     * 使用类型。
     *
     * @param usingType using type / 使用类型
     * @return [UsingFieldMatcher]
     */
    fun usingType(usingType: UsingType) = also {
        this.usingType = usingType
    }

    /**
     * The field descriptor, specifies a unique field.
     * ----------------
     * 字段描述符，指定唯一的字段。
     *
     *     fieldDescriptor("Lorg/luckypray/dexkit/demo/MainActivity;->mText:Ljava/lang/String;")
     *
     * @param descriptor field descriptor / 字段描述符
     * @return [FieldMatcher]
     */
    fun fieldDescriptor(descriptor: String) = also {
        val dexField = DexField(descriptor)
        fieldName(dexField.name)
        fieldDeclaredClass(dexField.className)
        fieldType(dexField.typeName)
    }

    /**
     * The field name string matcher
     * ----------------
     * 字段名称字符串匹配器
     *
     *     fieldName(StringMatcher().value("mText"))
     *
     * @param name field name / 字段名称
     * @return [FieldMatcher]
     */
    fun fieldName(name: StringMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.name(name)
    }

    /**
     * The field name.
     * ----------------
     * 字段名称。
     *
     *     fieldName("mText")
     *
     * @param name field name / 字段名称
     * @param matchType match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun fieldName(
        name: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.name(name, matchType, ignoreCase)
    }

    /**
     * Field modifiers matcher.
     * ----------------
     * 字段修饰符匹配器。
     *
     *     fieldModifiers(AccessFlagsMatcher().flags(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL))
     *
     * @param modifiers modifiers matcher / 修饰符匹配器
     * @return [FieldMatcher]
     */
    fun fieldModifiers(modifiers: AccessFlagsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.modifiers(modifiers)
    }

    /**
     * Field modifiers. Match using [java.lang.reflect.Modifier] mask bits.
     * ----------------
     * 字段修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配。
     *
     *     fieldModifiers(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL)
     *
     * @param modifiers modifiers / 修饰符
     * @param matchType match type / 匹配关系
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun fieldModifiers(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.modifiers(modifiers, matchType)
    }

    /**
     * The field declared class matcher.
     * ----------------
     * 字段声明类匹配器。
     *
     *     fieldDeclaredClass(ClassMatcher().className("org.luckypray.dexkit.demo.MainActivity"))
     *
     * @param declaredClass declared class matcher / 声明类匹配器
     * @return [FieldMatcher]
     */
    fun fieldDeclaredClass(declaredClass: ClassMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.declaredClass(declaredClass)
    }

    /**
     * The field declared class matcher.
     * ----------------
     * 字段声明类匹配器。
     *
     *     fieldDeclaredClass(MainActivity::class.java)
     *
     * @param clazz declared class / 声明类
     * @return [FieldMatcher]
     */
    fun fieldDeclaredClass(clazz: Class<*>) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.declaredClass(clazz)
    }

    /**
     * The field declared class name matcher.
     * ----------------
     * 字段声明类的名称匹配器。
     *
     *     fieldDeclaredClass("org.luckypray.dexkit.demo.MainActivity", StringMatchType.Equals, true)
     *
     * @param className declared class name / 声明类名称
     * @param matchType match type / 匹配关系
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun fieldDeclaredClass(
        className: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.declaredClass(className, matchType, ignoreCase)
    }

    /**
     * The field type matcher.
     * ----------------
     * 字段类型匹配器。
     *
     *     fieldType(ClassMatcher().className("java.lang.String"))
     *
     * @param type type matcher / 类型匹配器
     * @return [FieldMatcher]
     */
    fun fieldType(type: ClassMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.type(type)
    }

    /**
     * The field type class matcher.
     * ----------------
     * 字段类型类匹配器。
     *
     * @param clazz type class / 类型类
     * @return [FieldMatcher]
     */
    fun fieldType(clazz: Class<*>) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.type(clazz)
    }

    /**
     * The field type name matcher.
     * ----------------
     * 字段类型名称匹配器。
     *
     *     type("java.lang.String", StringMatchType.Equals, true)
     *
     * @param typeName type name / 类型名称
     * @param matchType match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun fieldType(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.type(typeName, matchType, ignoreCase)
    }

    /**
     * The field annotations matcher.
     * ----------------
     * 字段注解匹配器。
     *
     *     fieldAnnotations(AnnotationsMatcher().count(1))
     *
     * @param annotations annotations matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun fieldAnnotations(annotations: AnnotationsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotations(annotations)
    }

    /**
     * Add field annotation matcher.
     * ----------------
     * 添加字段注解匹配器。
     *
     *     addFieldAnnotation(AnnotationMatcher().type("org.luckypray.dexkit.demo.annotations.Router"))
     *
     * @param annotation annotation matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun addFieldAnnotation(annotation: AnnotationMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addAnnotation(annotation)
    }

    /**
     * Field annotation count.
     * ----------------
     * 字段注解数量。
     *
     * @param count annotation count / 注解数量
     * @return [FieldMatcher]
     */
    fun fieldAnnotationCount(count: Int) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(count)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     fieldAnnotationCount(IntRange(1, 2))
     *
     * @param range annotation count range / 注解数量范围
     * @return [FieldMatcher]
     */
    fun fieldAnnotationCount(range: IntRange) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(range)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     fieldAnnotationCount(1..2)
     *
     * @param range annotation count range / 注解数量范围
     * @return [FieldMatcher]
     */
    fun fieldAnnotationCount(range: kotlin.ranges.IntRange) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(range)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     fieldAnnotationCount(1, 2)
     *
     * @param min min annotation count / 最小注解数量
     * @param max max annotation count / 最大注解数量
     * @return [FieldMatcher]
     */
    fun fieldAnnotationCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(min, max)
    }

    /**
     * Read this field value's methods matcher.
     * ----------------
     * 读取该字段值的方法匹配器。
     *
     *     getFieldMethods(MethodsMatcher().add(MethodMatcher().name("getText")))
     *
     * @param getMethods methods matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun getFieldMethods(getMethods: MethodsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.getMethods(getMethods)
    }

    /**
     * Add read this field value's method matcher.
     * ----------------
     * 添加读取该字段值的方法匹配器。
     *
     *     addGetFieldMethod(MethodMatcher().name("getText"))
     *
     * @param getMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addGetFieldMethod(getMethod: MethodMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addGetMethod(getMethod)
    }

    /**
     * Read this field value's methods matcher.
     * ----------------
     * 读取该字段值的方法匹配器。
     *
     *     addGetFieldMethod("Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;")
     *
     * @param methodDescriptor method descriptor / 方法描述符
     * @return [FieldMatcher]
     */
    fun addGetFieldMethod(methodDescriptor: String) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addGetMethod(MethodMatcher(methodDescriptor))
    }

    /**
     * Write this field value's methods matcher.
     * ----------------
     * 写入该字段值的方法匹配器。
     *
     *     putFieldMethods(MethodsMatcher().add(MethodMatcher().name("setText")))
     *
     * @param putMethods methods matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun putFieldMethods(putMethods: MethodsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.putMethods(putMethods)
    }

    /**
     * Add write this field value's method matcher.
     * ----------------
     * 添加写入该字段值的方法匹配器。
     *
     *     addPutFieldMethod(MethodMatcher().name("setText"))
     *
     * @param putMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addPutFieldMethod(putMethod: MethodMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addPutMethod(putMethod)
    }

    /**
     * Write this field value's methods matcher.
     * ----------------
     * 写入该字段值的方法匹配器。
     *
     *     addPutFieldMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V")
     *
     * @param methodDescriptor method descriptor / 方法描述符
     * @return [FieldMatcher]
     */
    fun addPutFieldMethod(methodDescriptor: String) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addPutMethod(MethodMatcher(methodDescriptor))
    }

    // region DSL

    /**
     * @see field
     */
    @kotlin.internal.InlineOnly
    inline fun field(init: FieldMatcher.() -> Unit) = also {
        field(FieldMatcher().apply(init))
    }
    /**
     * @see fieldDeclaredClass
     */
    @kotlin.internal.InlineOnly
    inline fun fieldDeclaredClass(init: ClassMatcher.() -> Unit) = also {
        fieldDeclaredClass(ClassMatcher().apply(init))
    }

    /**
     * @see fieldType
     */
    @kotlin.internal.InlineOnly
    inline fun fieldType(init: ClassMatcher.() -> Unit) = also {
        fieldType(ClassMatcher().apply(init))
    }

    /**
     * @see fieldAnnotations
     */
    @kotlin.internal.InlineOnly
    inline fun fieldAnnotations(init: AnnotationsMatcher.() -> Unit) = also {
        fieldAnnotations(AnnotationsMatcher().apply(init))
    }

    /**
     * @see addFieldAnnotation
     */
    @kotlin.internal.InlineOnly
    inline fun addFieldAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addFieldAnnotation(AnnotationMatcher().apply(init))
    }

    /**
     * @see getFieldMethods
     */
    @kotlin.internal.InlineOnly
    inline fun getFieldMethods(init: MethodsMatcher.() -> Unit) = also {
        getFieldMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addGetFieldMethod
     */
    @kotlin.internal.InlineOnly
    inline fun addGetFieldMethod(init: MethodMatcher.() -> Unit) = also {
        addGetFieldMethod(MethodMatcher().apply(init))
    }

    /**
     * @see putFieldMethods
     */
    @kotlin.internal.InlineOnly
    inline fun putFieldMethods(init: MethodsMatcher.() -> Unit) = also {
        putFieldMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addPutFieldMethod
     */
    @kotlin.internal.InlineOnly
    inline fun addPutFieldMethod(init: MethodMatcher.() -> Unit) = also {
        addPutFieldMethod(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = UsingFieldMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        matcher ?: throw IllegalArgumentException("UsingFieldMatcher matcher not set")
        val root = InnerUsingFieldMatcher.createUsingFieldMatcher(
            fbb,
            matcher!!.build(fbb),
            usingType.value
        )
        fbb.finish(root)
        return root
    }
}