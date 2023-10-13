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
import org.luckypray.dexkit.query.enums.UsingType

class UsingFieldMatcher : BaseQuery() {
    var matcher: FieldMatcher? = null
        private set

    /**
     * Using type. Default is [UsingType.Any].
     */
    @set:JvmSynthetic
    var usingType: UsingType = UsingType.Any

    /**
     * Need to match field.
     * ----------------
     * 要匹配的字段
     *
     * @param matcher field / 字段
     * @return [UsingFieldMatcher]
     */
    fun matcher(matcher: FieldMatcher) = also {
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

    // region DSL

    /**
     * @see matcher
     */
    @kotlin.internal.InlineOnly
    inline fun matcher(init: FieldMatcher.() -> Unit) = also {
        matcher(FieldMatcher().apply(init))
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