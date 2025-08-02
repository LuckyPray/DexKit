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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerFieldMatcher
import org.luckypray.dexkit.query.base.BaseMatcher
import org.luckypray.dexkit.query.base.IAnnotationEncodeValue
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.StringMatcher
import org.luckypray.dexkit.util.DexSignUtil
import org.luckypray.dexkit.wrap.DexField
import java.lang.reflect.Field

class FieldMatcher : BaseMatcher, IAnnotationEncodeValue {
    var nameMatcher: StringMatcher? = null
        private set
    var modifiersMatcher: AccessFlagsMatcher? = null
        private set
    var classMatcher: ClassMatcher? = null
        private set
    var typeMatcher: ClassMatcher? = null
        private set
    var annotationsMatcher: AnnotationsMatcher? = null
        private set
    var getMethodsMatcher: MethodsMatcher? = null
        private set
    var putMethodsMatcher: MethodsMatcher? = null
        private set

    constructor()

    constructor(field: Field) {
        descriptor(DexSignUtil.getFieldDescriptor(field))
    }

    constructor(descriptor: String) {
        descriptor(descriptor)
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
        this.nameMatcher = name
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
        this.nameMatcher = StringMatcher(name, matchType, ignoreCase)
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
        this.modifiersMatcher = modifiers
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
        this.modifiersMatcher = AccessFlagsMatcher(modifiers, matchType)
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
        this.classMatcher = declaredClass
    }

    /**
     * The field declared class matcher.
     * ----------------
     * 字段声明类匹配器。
     *
     *     declaredClass(MainActivity::class.java)
     *
     * @param clazz declared class / 声明类
     * @return [FieldMatcher]
     */
    fun declaredClass(clazz: Class<*>) = also {
        this.classMatcher = ClassMatcher().className(DexSignUtil.getTypeName(clazz))
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
        this.classMatcher = ClassMatcher().className(className, matchType, ignoreCase)
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
        this.typeMatcher = type
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
        this.typeMatcher = ClassMatcher().className(DexSignUtil.getTypeName(clazz))
    }

    /**
     * The field type name matcher.
     * ----------------
     * 字段类型名称匹配器。
     *
     *     type("java.lang.String", StringMatchType.Equals, true)
     *
     * @param typeName type name / 类型名称
     * @param matchType match type / 匹配关系
     * @param ignoreCase ignore case / 忽略大小写
     * @return [FieldMatcher]
     */
    @JvmOverloads
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        this.typeMatcher = ClassMatcher().className(typeName, matchType, ignoreCase)
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
        this.annotationsMatcher = annotations
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
        this.annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.add(annotation)
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
        this.annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(count)
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
        this.annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
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
        this.annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(range)
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
        this.annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(min, max)
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
        this.getMethodsMatcher = readMethods
    }

    /**
     * Add read this field value's method matcher.
     * ----------------
     * 添加读取该字段值的方法匹配器。
     *
     *     addReadMethod(MethodMatcher().name("getText"))
     *
     * @param readMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addReadMethod(readMethod: MethodMatcher) = also {
        this.getMethodsMatcher = getMethodsMatcher ?: MethodsMatcher()
        this.getMethodsMatcher!!.add(readMethod)
    }

    /**
     * Add read this field value's method matcher.
     * ----------------
     * 添加读取该字段值的方法匹配器。
     *
     *     addReadMethod(MethodMatcher().name("getText"))
     *
     * @param methodDescriptor method descriptor / 方法描述符
     * @return [FieldMatcher]
     */
    fun addReadMethod(methodDescriptor: String) = also {
        this.getMethodsMatcher = getMethodsMatcher ?: MethodsMatcher()
        this.getMethodsMatcher!!.add(MethodMatcher(methodDescriptor))
    }

    /**
     * Write this field value's methods matcher.
     * ----------------
     * 写入该字段值的方法匹配器。
     *
     *     writeMethods(MethodsMatcher().add(MethodMatcher().name("setText")))
     *
     * @param writeMethods methods matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun writeMethods(writeMethods: MethodsMatcher) = also {
        this.putMethodsMatcher = writeMethods
    }

    /**
     * Add write this field value's method matcher.
     * ----------------
     * 添加写入该字段值的方法匹配器。
     *
     *     addWriteMethod(MethodMatcher().name("setText"))
     *
     * @param writeMethod method matcher / 方法匹配器
     * @return [FieldMatcher]
     */
    fun addWriteMethod(writeMethod: MethodMatcher) = also {
        this.putMethodsMatcher = putMethodsMatcher ?: MethodsMatcher()
        this.putMethodsMatcher!!.add(writeMethod)
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
        this.putMethodsMatcher = putMethodsMatcher ?: MethodsMatcher()
        this.putMethodsMatcher!!.add(MethodMatcher(methodDescriptor))
    }

    // region DSL

    /**
     * @see declaredClass
     */
    @JvmSynthetic
    fun declaredClass(init: ClassMatcher.() -> Unit) = also {
        declaredClass(ClassMatcher().apply(init))
    }

    /**
     * @see type
     */
    @JvmSynthetic
    fun type(init: ClassMatcher.() -> Unit) = also {
        type(ClassMatcher().apply(init))
    }

    /**
     * @see annotations
     */
    @JvmSynthetic
    fun annotations(init: AnnotationsMatcher.() -> Unit) = also {
        annotations(AnnotationsMatcher().apply(init))
    }

    /**
     * @see addAnnotation
     */
    @JvmSynthetic
    fun addAnnotation(init: AnnotationMatcher.() -> Unit) = also {
        addAnnotation(AnnotationMatcher().apply(init))
    }

    /**
     * @see readMethods
     */
    @JvmSynthetic
    fun readMethods(init: MethodsMatcher.() -> Unit) = also {
        readMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addReadMethod
     */
    @JvmSynthetic
    fun addReadMethod(init: MethodMatcher.() -> Unit) = also {
        addReadMethod(MethodMatcher().apply(init))
    }

    /**
     * @see writeMethods
     */
    @JvmSynthetic
    fun writeMethods(init: MethodsMatcher.() -> Unit) = also {
        writeMethods(MethodsMatcher().apply(init))
    }

    /**
     * @see addWriteMethod
     */
    @JvmSynthetic
    fun addWriteMethod(init: MethodMatcher.() -> Unit) = also {
        addWriteMethod(MethodMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = FieldMatcher()

        @JvmStatic
        fun create(field: Field) = FieldMatcher(field)

        /**
         * @see FieldMatcher.descriptor
         */
        @JvmStatic
        fun create(descriptor: String) = FieldMatcher(descriptor)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerFieldMatcher.createFieldMatcher(
            fbb,
            nameMatcher?.build(fbb) ?: 0,
            modifiersMatcher?.build(fbb) ?: 0,
            classMatcher?.build(fbb) ?: 0,
            typeMatcher?.build(fbb) ?: 0,
            annotationsMatcher?.build(fbb) ?: 0,
            getMethodsMatcher?.build(fbb) ?: 0,
            putMethodsMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}