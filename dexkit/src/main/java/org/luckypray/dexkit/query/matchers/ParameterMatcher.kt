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
import org.luckypray.dexkit.InnerParameterMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.util.DexSignUtil

class ParameterMatcher : BaseQuery() {
    var annotationsMatcher: AnnotationsMatcher? = null
        private set
    var typeMatcher: ClassMatcher? = null
        private set

    /**
     * Parameter type class fully qualified name.
     * ----------------
     * 参数类型类的完全限定名。
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
     * Parameter type class matcher.
     * ----------------
     * 参数类型类匹配器。
     *
     *     type(ClassMatcher().className("java.lang.String"))
     *
     * @param type type class matcher / 类型类匹配器
     */
    fun type(type: ClassMatcher) = also {
        this.typeMatcher = type
    }

    /**
     * Parameter type class matcher.
     * ----------------
     * 参数类型类匹配器。
     *
     *     type(String::class.java)
     *
     * @param clazz type class / 类型类
     * @return [ParameterMatcher]
     */
    fun type(clazz: Class<*>) = also {
        this.typeMatcher = ClassMatcher().className(DexSignUtil.getTypeName(clazz))
    }

    /**
     * Parameter type class name matcher.
     * ----------------
     * 参数类型类名匹配器
     *
     *     type("java.lang.String", StringMatchType.Equals, true)
     *
     * @param typeName type class name / 类型类名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [ParameterMatcher]
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
     * The param annotations matcher.
     * ----------------
     * 参数注解匹配器。
     *
     *     annotations(AnnotationsMatcher().count(1))
     *
     * @param annotations annotations matcher / 注解匹配器
     * @return [ParameterMatcher]
     */
    fun annotations(annotations: AnnotationsMatcher) = also {
        this.annotationsMatcher = annotations
    }

    /**
     * Add param annotation matcher.
     * ----------------
     * 添加参数注解匹配器。
     *
     *     addAnnotation(AnnotationMatcher().type("org.luckypray.dexkit.demo.annotations.Router"))
     *
     * @param annotation annotation matcher / 注解匹配器
     * @return [ParameterMatcher]
     */
    fun addAnnotation(annotation: AnnotationMatcher) = also {
        annotationsMatcher = annotationsMatcher ?: AnnotationsMatcher()
        annotationsMatcher!!.add(annotation)
    }

    /**
     * Param annotation count.
     * ----------------
     * 参数注解数量。
     *
     * @param count annotation count / 注解数量
     * @return [ParameterMatcher]
     */
    fun annotationCount(count: Int) = also {
        this.annotationsMatcher = this.annotationsMatcher ?: AnnotationsMatcher()
        this.annotationsMatcher!!.count(count)
    }


    // region DSL

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

    // endregion

    companion object {
        @JvmStatic
        fun create() = ParameterMatcher()
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerParameterMatcher.createParameterMatcher(
            fbb,
            annotationsMatcher?.build(fbb) ?: 0,
            typeMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}