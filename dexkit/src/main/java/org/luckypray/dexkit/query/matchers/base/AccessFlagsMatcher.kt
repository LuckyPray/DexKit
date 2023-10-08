@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerAccessFlagsMatcher
import org.luckypray.dexkit.query.base.BaseQuery
import org.luckypray.dexkit.query.enums.MatchType

class AccessFlagsMatcher  : BaseQuery {
    /**
     * Access flags to match.
     * ----------------
     * 要匹配的访问标志。
     *
     *     modifiers = Modifier.PUBLIC or Modifier.STATIC
     */
    @set:JvmSynthetic
    var modifiers: Int = 0

    /**
     * Match type. Default is [MatchType.Contains].
     * ----------------
     * 匹配类型。默认为 [MatchType.Contains]。
     */
    @set:JvmSynthetic
    var matchType: MatchType = MatchType.Contains

    constructor()

    /**
     * Create a new [AccessFlagsMatcher].
     * ----------------
     * 创建一个新的 [AccessFlagsMatcher]。
     *
     * @param modifiers access flags / 访问标志
     * @param matchType match type / 匹配类型
     * @return [AccessFlagsMatcher]
     */
    @JvmOverloads
    constructor(
        modifiers: Int,
        matchType: MatchType = MatchType.Contains
    ) {
        this.modifiers = modifiers
        this.matchType = matchType
    }

    companion object {
        /**
         * Create a new [AccessFlagsMatcher].
         * ----------------
         * 创建一个新的 [AccessFlagsMatcher]。
         *
         * @param modifiers access flags / 访问标志
         * @param matchType match type / 匹配类型
         * @return [AccessFlagsMatcher]
         */
        @JvmOverloads
        fun create(
            modifiers: Int,
            matchType: MatchType = MatchType.Contains
        ) = AccessFlagsMatcher(modifiers, matchType)
    }

    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        if (modifiers == 0) throw IllegalArgumentException("modifiers must not be 0")
        val root = InnerAccessFlagsMatcher.createAccessFlagsMatcher(
            fbb,
            modifiers.toUInt(),
            matchType.value
        )
        fbb.finish(root)
        return root
    }
}