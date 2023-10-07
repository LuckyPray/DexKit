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