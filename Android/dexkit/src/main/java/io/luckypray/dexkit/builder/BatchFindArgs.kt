@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package io.luckypray.dexkit.builder

import io.luckypray.dexkit.enums.MatchType

/**
 * @since 1.1.0
 */
class BatchFindArgs private constructor(
    override val findPackage: String,
    val queryMap: Map<String, Set<String>>,
    val matchType: Int,
) : BaseArgs(findPackage) {

    companion object {

        /**
         * @since 1.1.0
         */
        @JvmStatic
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        @kotlin.internal.InlineOnly
        inline fun build(block: Builder.() -> Unit): BatchFindArgs {
            return Builder().apply(block).build()
        }

        /**
         * @since 1.1.0
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * @since 1.1.0
     */
    class Builder : BaseArgs.Builder<BatchFindArgs>() {
        /**
         * query map, key is unique key, value is class/method using strings
         */
        @set:JvmSynthetic
        var queryMap = mapOf<String, Set<String>>()

        /**
         * match type, type of string to match
         *
         * default [MatchType.SIMILAR_REGEX], similar regex matches, only support: '^', '$'
         */
        @set:JvmSynthetic
        var matchType: MatchType = MatchType.SIMILAR_REGEX

        /**
         * [Builder.queryMap]
         */
        fun queryMap(queryMap: Map<String, Iterable<String>>) = this.also {
            this.queryMap = queryMap.mapValues { toSet(it.value) }
        }

        /**
         * add query to [Builder.queryMap]
         *
         * @param [key] class name or unique key
         * @param [usingStrings] class/method using strings
         */
        fun addQuery(key: String, usingStrings: Iterable<String>) = this.also {
            (queryMap as? MutableMap ?: queryMap.toMutableMap()).let {
                it[key] = toSet(usingStrings)
                queryMap = it
            }
        }

        /**
         * add query to [Builder.queryMap]
         *
         * @param [key] class name or unique key
         * @param [usingStrings] class/method using strings
         */
        fun addQuery(key: String, usingStrings: Array<String>) = this.also {
            addQuery(key, usingStrings.toSet())
        }

        /**
         * [Builder.matchType]
         */
        fun matchType(matchType: MatchType) = this.also {
            this.matchType = matchType
        }

        /**
         * build [BatchFindArgs]
         *
         * @return [BatchFindArgs]
         */
        override fun build(): BatchFindArgs = BatchFindArgs(
            findPackage,
            queryMap,
            matchType.ordinal
        )

        private fun toSet(strings: Iterable<String>) = strings as? Set ?: strings.toSet()
    }
}