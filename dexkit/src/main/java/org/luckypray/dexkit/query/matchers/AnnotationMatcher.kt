@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit.query.matchers

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAnnotationMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType
import org.luckypray.dexkit.query.enums.RetentionPolicyType
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.enums.TargetElementType
import org.luckypray.dexkit.query.matchers.base.IntRange
import org.luckypray.dexkit.query.matchers.base.TargetElementTypesMatcher

class AnnotationMatcher : BaseQuery() {
    var typeMatcher: ClassMatcher? = null
        private set
    var targetElementTypesMatcher: TargetElementTypesMatcher? = null
        private set
    @set:JvmSynthetic
    var policy: RetentionPolicyType? = null
    var elementsMatcher: AnnotationElementsMatcher? = null
        private set

    /**
     * Annotation type class fully qualified name.
     * ----------------
     * 注解类型类的完全限定名。
     *
     *     type = "org.luckypray.dexkit.demo.annotations.Router"
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
     * Annotation type class matcher.
     * ----------------
     * 注解类型类匹配器。
     *
     *     type(ClassMatcher().className("org.luckypray.dexkit.demo.annotations.Router"))
     *
     * @param type type class matcher / 类型类匹配器
     * @return [AnnotationMatcher]
     */
    fun type(type: ClassMatcher) = also {
        this.typeMatcher = type
    }

    /**
     * Annotation type class name matcher.
     * ----------------
     * 注解类型类名匹配器
     *
     *     type("org.luckypray.dexkit.demo.annotations.Router", StringMatchType.Equals, true)
     *
     * @param typeName type class name / 类型类名
     * @param matchType match type / 匹配类型
     * @param ignoreCase ignore case / 忽略大小写
     * @return [AnnotationMatcher]
     */
    @JvmOverloads
    fun type(
        typeName: String,
        matchType: StringMatchType = StringMatchType.Equals,
        ignoreCase: Boolean = false
    ) = also {
        typeMatcher = ClassMatcher().className(typeName, matchType, ignoreCase)
    }

    /**
     * Annotation target element types matcher.
     * ----------------
     * 注解目标元素类型匹配器。
     *
     *     @Target(ElementType.TYPE, ElementType.METHOD) <----------- TargetElementType
     *     @Retention(RetentionPolicy.RUNTIME)
     *     public @interface Router {
     *         String value();
     *     }
     *
     * @param targetElementTypes target element types matcher / 注解目标元素类型匹配器
     * @return [AnnotationMatcher]
     */
    fun targetElementTypes(targetElementTypes: TargetElementTypesMatcher) = also {
        this.targetElementTypesMatcher = targetElementTypes
    }

    /**
     * Annotation target element types matcher.
     * ----------------
     * 注解目标元素类型匹配器。
     *
     *     @Target(ElementType.TYPE, ElementType.METHOD) <----------- TargetElementType
     *     @Retention(RetentionPolicy.RUNTIME)
     *     public @interface Router {
     *         String value();
     *     }
     *
     * @param targetElementTypes target element types / 注解目标元素类型
     * @param matchType match type / 匹配类型
     * @return [AnnotationMatcher]
     */
    fun targetElementTypes(
        targetElementTypes: List<TargetElementType>,
        matchType: MatchType = MatchType.Contains
    ) = also {
        this.targetElementTypesMatcher = TargetElementTypesMatcher().apply {
            types(targetElementTypes)
            matchType(matchType)
        }
    }

    /**
     * Annotation RetentionPolicy.
     * ----------------
     * 注解保留策略。
     *
     *     @Target(ElementType.TYPE, ElementType.METHOD)
     *     @Retention(RetentionPolicy.RUNTIME) <----------- RetentionPolicy
     *     public @interface Router {
     *         String value();
     *     }
     *
     * @param policy RetentionPolicy / 保留策略
     * @return [AnnotationMatcher]
     */
    fun policy(policy: RetentionPolicyType) = also {
        this.policy = policy
    }

    /**
     * Annotation elements matcher.
     * ----------------
     * 注解元素匹配器。
     *
     *     @Router(
     *         value = "/main" <----------- AnnotationElement
     *           ^        ^
     *         name     value
     *     )
     *     public class MainActivity extends AppCompatActivity {}
     *
     * @param elements elements matcher / 元素匹配器
     * @return [AnnotationMatcher]
     */
    fun elements(elements: AnnotationElementsMatcher) = also {
        this.elementsMatcher = elements
    }

    /**
     * Add annotation element matcher.
     * ----------------
     * 添加注解元素匹配器。
     *
     *     @Router(
     *         value = "/main" <----------- AnnotationElement
     *           ^        ^
     *         name     value
     *     )
     *     public class MainActivity extends AppCompatActivity {}
     *
     *     addElement(AnnotationElementMatcher().name("value").stringValue("main"))
     */
    fun addElement(element: AnnotationElementMatcher) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).add(element)
    }

    /**
     * Annotation element match type.
     * ----------------
     * 注解元素匹配类型。
     *
     *     elementMatchType(MatchType.Equals)
     *
     * @param matchType match type / 匹配类型
     * @return [AnnotationMatcher]
     */
    fun elementMatchType(matchType: MatchType) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).matchType(matchType)
    }

    /**
     * Annotation element count.
     * ----------------
     * 注解元素数量。
     *
     * @param count element count / 元素数量
     * @return [AnnotationMatcher]
     */
    fun elementCount(count: Int) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(count)
    }

    /**
     * Annotation element count range.
     * ----------------
     * 注解元素数量范围。
     *
     *     elementCount(IntRange(1, 3))
     *
     * @param range element count range / 元素数量范围
     * @return [AnnotationMatcher]
     */
    fun elementCount(range: IntRange) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(range)
    }

    /**
     * Annotation element count range.
     * ----------------
     * 注解元素数量范围。
     *
     *     elementCount(1..3)
     *
     * @param range element count range / 元素数量范围
     * @return [AnnotationMatcher]
     */
    fun elementCount(range: kotlin.ranges.IntRange) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(range)
    }

    /**
     * Annotation element count range.
     * ----------------
     * 注解元素数量范围。
     *
     *     elementCount(1, 3)
     *
     * @param min min element count / 最小元素数量
     * @param max max element count / 最大元素数量
     * @return [AnnotationMatcher]
     */
    fun elementCount(min: Int, max: Int) = also {
        elementsMatcher = elementsMatcher ?: AnnotationElementsMatcher()
        (elementsMatcher as AnnotationElementsMatcher).count(min, max)
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
     * @see targetElementTypes
     */
    @kotlin.internal.InlineOnly
    inline fun targetElementTypes(init: TargetElementTypesMatcher.() -> Unit) = also {
        targetElementTypes(TargetElementTypesMatcher().apply(init))
    }

    /**
     * @see elements
     */
    @kotlin.internal.InlineOnly
    inline fun elements(init: AnnotationElementsMatcher.() -> Unit) = also {
        elements(AnnotationElementsMatcher().apply(init))
    }

    /**
     * @see addElement
     */
    @kotlin.internal.InlineOnly
    inline fun addElement(init: AnnotationElementMatcher.() -> Unit) = also {
        addElement(AnnotationElementMatcher().apply(init))
    }

    // endregion

    companion object {
        @JvmStatic
        fun create() = AnnotationMatcher()
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerAnnotationMatcher.createAnnotationMatcher(
            fbb,
            typeMatcher?.build(fbb) ?: 0,
            targetElementTypesMatcher?.build(fbb) ?: 0,
            policy?.value ?: 0,
            elementsMatcher?.build(fbb) ?: 0
        )
        fbb.finish(root)
        return root
    }
}