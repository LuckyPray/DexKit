@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit.query.matchers.base

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.InnerIntRange
import org.luckypray.dexkit.query.base.BaseQuery

class IntRange : BaseQuery {
    private var min: Int
    private var max: Int

    constructor(value: Int) {
        min = value
        max = value
    }

    constructor(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    companion object {
        fun create(value: Int) = IntRange(value)
        fun create(min: Int, max: Int) = IntRange(min, max)
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    override fun build(fbb: FlatBufferBuilder): Int {
        val root = InnerIntRange.createIntRange(
            fbb,
            min,
            max
        )
        fbb.finish(root)
        return root
    }
}