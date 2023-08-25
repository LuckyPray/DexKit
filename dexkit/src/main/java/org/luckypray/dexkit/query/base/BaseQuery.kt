@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")package org.luckypray.dexkit.query.base

import com.google.flatbuffers.FlatBufferBuilder

abstract class BaseQuery : IQuery {

    protected abstract fun innerBuild(fbb: FlatBufferBuilder): Int

    @kotlin.internal.InlineOnly
    internal inline fun build(fbb: FlatBufferBuilder): Int {
        return innerBuild(fbb)
    }

    protected fun getEncodeId(dexId: Int, id: Int) = ((dexId.toLong() shl 32) or id.toLong())
}