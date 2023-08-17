package org.luckypray.dexkit.query

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.DexKitDsl

@DexKitDsl
abstract class BaseQuery {
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal abstract fun build(fbb: FlatBufferBuilder): Int
}