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
    var descriptor: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            descriptor(value)
        }

    /**
     * The field name.
     * ----------------
     * 字段名称。
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
     * Field modifiers. Match using [java.lang.reflect.Modifier] mask bits,
     * default match type is contains, if you need to match exactly,
     * please use [modifiers] overloaded function.
     * ----------------
     * 字段修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配，
     * 默认匹配关系为包含，如果需要完全限定匹配请使用 [modifiers] 重载函数。
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
     * The field declared class fully qualified name.
     * ----------------
     * 字段声明类的完全限定名。
     *
     *     declaredClass = "org.luckypray.dexkit.demo.MainActivity"
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
     * The field type fully qualified name.
     * ----------------
     * 字段类型的完全限定名。
     *
     *     type = "java.lang.String"
     */
    var type: String
        @JvmSynthetic
        @Deprecated("Property can only be written.", level = DeprecationLevel.ERROR)
        get() = throw NotImplementedError()
        @JvmSynthetic
        set(value) {
            type(value)
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
     *     descriptor("Lorg/luckypray/dexkit/demo/MainActivity;->mText:Ljava/lang/String;")
     *
     * @param descriptor field descriptor / 字段描述符
     * @return [FieldMatcher]
     */
    fun descriptor(descriptor: String) = also {
        val dexField = DexField(descriptor)
        name(dexField.name)
        declaredClass(dexField.className)
        type(dexField.typeName)
    }

    /**
     * The field name string matcher
     * ----------------
     * 字段名称字符串匹配器
     *
     *     name(StringMatcher().value("mText"))
     *
     * @param name field name / 字段名称
     * @return [FieldMatcher]
     */
    fun name(name: StringMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.name(name)
    }

    /**
     * The field name.
     * ----------------
     * 字段名称。
     *
     *     name("mText")
     *
     * @param name field name / 字段名称
     * @param matchType match type / 字符串匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun name(
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
     *     modifiers(AccessFlagsMatcher().flags(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL))
     *
     * @param modifiers modifiers matcher / 修饰符匹配器
     * @return [FieldMatcher]
     */
    fun modifiers(modifiers: AccessFlagsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.modifiers(modifiers)
    }

    /**
     * Field modifiers. Match using [java.lang.reflect.Modifier] mask bits.
     * ----------------
     * 字段修饰符。使用 [java.lang.reflect.Modifier] mask bits 进行匹配。
     *
     *     modifiers(Modifier.PUBLIC or Modifier.STATIC or Modifier.FINAL)
     *
     * @param modifiers modifiers / 修饰符
     * @param matchType match type / 匹配关系
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun modifiers(
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
     *     declaredClass(ClassMatcher().className("org.luckypray.dexkit.demo.MainActivity"))
     *
     * @param declaredClass declared class matcher / 声明类匹配器
     * @return [FieldMatcher]
     */
    fun declaredClass(declaredClass: ClassMatcher) = also {
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
    fun declaredClass(clazz: Class<*>) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.declaredClass(clazz)
    }

    /**
     * The field declared class name matcher.
     * ----------------
     * 字段声明类的名称匹配器。
     *
     *     declaredClass("org.luckypray.dexkit.demo.MainActivity", StringMatchType.Equals, true)
     *
     * @param className declared class name / 声明类名称
     * @param matchType match type / 匹配关系
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun declaredClass(
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
     *     type(ClassMatcher().className("java.lang.String"))
     *
     * @param type type matcher / 类型匹配器
     * @return [FieldMatcher]
     */
    fun type(type: ClassMatcher) = also {
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
    fun type(clazz: Class<*>) = also {
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
    fun type(
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
     *     annotations(AnnotationsMatcher().count(1))
     *
     * @param annotations annotations matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun annotations(annotations: AnnotationsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotations(annotations)
    }

    /**
     * Add field annotation matcher.
     * ----------------
     * 添加字段注解匹配器。
     *
     *     addAnnotation(AnnotationMatcher().type("org.luckypray.dexkit.demo.annotations.Router"))
     *
     * @param annotation annotation matcher / 注解匹配器
     * @return [FieldMatcher]
     */
    fun addAnnotation(annotation: AnnotationMatcher) = also {
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
    fun annotationCount(count: Int) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(count)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     annotationCount(IntRange(1, 2))
     *
     * @param range annotation count range / 注解数量范围
     * @return [FieldMatcher]
     */
    fun annotationCount(range: IntRange) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(range)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     annotationCount(1..2)
     *
     * @param range annotation count range / 注解数量范围
     * @return [FieldMatcher]
     */
    fun annotationCount(range: kotlin.ranges.IntRange) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(range)
    }

    /**
     * Field annotation count range.
     * ----------------
     * 字段注解数量范围。
     *
     *     annotationCount(1, 2)
     *
     * @param min min annotation count / 最小注解数量
     * @param max max annotation count / 最大注解数量
     * @return [FieldMatcher]
     */
    fun annotationCount(min: Int = 0, max: Int = Int.MAX_VALUE) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.annotationCount(min, max)
    }

    /**
     * Read this field value's methods matcher.
     * ----------------
     * 读取该字段值的方法匹配器。
     *
     *     readMethods(MethodsMatcher().add(MethodMatcher().name("getText")))
     *
     * @param readMethods methods matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun readMethods(readMethods: MethodsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.readMethods(readMethods)
    }

    /**
     * Add read this field value's method matcher.
     * ----------------
     * 添加读取该字段值的方法匹配器。
     *
     *     addReadMethods(MethodMatcher().name("getText"))
     *
     * @param readMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addReadMethods(readMethod: MethodMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addReadMethod(readMethod)
    }

    /**
     * Read this field value's methods matcher.
     * ----------------
     * 读取该字段值的方法匹配器。
     *
     *     addReadMethods("Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;")
     *
     * @param methodDescriptor method descriptor / 方法描述符
     * @return [FieldMatcher]
     */
    fun addReadMethods(methodDescriptor: String) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addReadMethod(MethodMatcher(methodDescriptor))
    }

    /**
     * Write this field value's methods matcher.
     * ----------------
     * 写入该字段值的方法匹配器。
     *
     *     writeMethods(MethodsMatcher().add(MethodMatcher().name("setText")))
     *
     * @param putMethods methods matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun writeMethods(putMethods: MethodsMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.writeMethods(putMethods)
    }

    /**
     * Add write this field value's method matcher.
     * ----------------
     * 添加写入该字段值的方法匹配器。
     *
     *     addWriteMethod(MethodMatcher().name("setText"))
     *
     * @param putMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addWriteMethod(putMethod: MethodMatcher) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addWriteMethod(putMethod)
    }

    /**
     * Write this field value's methods matcher.
     * ----------------
     * 写入该字段值的方法匹配器。
     *
     *     addWriteMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V")
     *
     * @param methodDescriptor method descriptor / 方法描述符
     * @return [FieldMatcher]
     */
    fun addWriteMethod(methodDescriptor: String) = also {
        this.matcher = matcher ?: FieldMatcher()
        this.matcher!!.addWriteMethod(MethodMatcher(methodDescriptor))
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
     * @see declaredClass
     */
    @kotlin.internal.InlineOnly
    inline fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        declaredClass(ClassMatcher().apply(init))
    }

    /**
     * @see type
     */
    @kotlin.internal.InlineOnly
    inline fun type(init: ClassMatcher.() -> Unit) = also {
        type(ClassMatcher().apply(init))
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
     * @see readMethods
     */
    @kotlin.internal.InlineOnly
    inline fun readMethods(init: MethodsMatcher.() -> Unit) = also {
        readMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addReadMethods
     */
    @kotlin.internal.InlineOnly
    inline fun addReadMethods(init: MethodMatcher.() -> Unit) = also {
        addReadMethods(MethodMatcher().apply(init))
    }

    /**
     * @see writeMethods
     */
    @kotlin.internal.InlineOnly
    inline fun writeMethods(init: MethodsMatcher.() -> Unit) = also {
        writeMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addWriteMethod
     */
    @kotlin.internal.InlineOnly
    inline fun addWriteMethod(init: MethodMatcher.() -> Unit) = also {
        addWriteMethod(MethodMatcher().apply(init))
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