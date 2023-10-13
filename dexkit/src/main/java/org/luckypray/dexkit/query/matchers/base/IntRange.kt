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

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerIntRange
import org.luckypray.dexkit.query.base.BaseQuery

class IntRange : BaseQuery {

    @set:JvmSynthetic
    var min: Int = 0

    @set:JvmSynthetic
    var max: Int = Int.MAX_VALUE

    constructor(value: Int) {
        min = value
        max = value
    }

    constructor(min: Int = 0, max: Int = Int.MAX_VALUE) {
        this.min = min
        this.max = max
    }

    constructor(range: kotlin.ranges.IntRange) {
        min = range.first
        max = range.last
    }

    companion object {
        fun create(value: Int) = IntRange(value)
        fun create(min: Int = 0, max: Int = Int.MAX_VALUE) = IntRange(min, max)
    }
    
    override fun innerBuild(fbb: FlatBufferBuilder): Int {
        val root = InnerIntRange.createIntRange(
            fbb,
            min,
            max
        )
        fbb.finish(root)
        return root
    }
}