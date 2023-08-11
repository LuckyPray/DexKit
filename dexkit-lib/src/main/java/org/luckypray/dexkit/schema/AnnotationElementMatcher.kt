// automatically generated by the FlatBuffers compiler, do not modify

package org.luckypray.dexkit.schema

import com.google.flatbuffers.BaseVector
import com.google.flatbuffers.BooleanVector
import com.google.flatbuffers.ByteVector
import com.google.flatbuffers.Constants
import com.google.flatbuffers.DoubleVector
import com.google.flatbuffers.FlatBufferBuilder
import com.google.flatbuffers.FloatVector
import com.google.flatbuffers.LongVector
import com.google.flatbuffers.StringVector
import com.google.flatbuffers.Struct
import com.google.flatbuffers.Table
import com.google.flatbuffers.UnionVector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sign

@Suppress("unused")
class AnnotationElementMatcher : Table() {

    fun __init(_i: Int, _bb: ByteBuffer)  {
        __reset(_i, _bb)
    }
    fun __assign(_i: Int, _bb: ByteBuffer) : AnnotationElementMatcher {
        __init(_i, _bb)
        return this
    }
    val name : StringMatcher? get() = name(StringMatcher())
    fun name(obj: StringMatcher) : StringMatcher? {
        val o = __offset(4)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    val value : OptionalAnnotationElementValueMatcher? get() = value(OptionalAnnotationElementValueMatcher())
    fun value(obj: OptionalAnnotationElementValueMatcher) : OptionalAnnotationElementValueMatcher? {
        val o = __offset(6)
        return if (o != 0) {
            obj.__assign(__indirect(o + bb_pos), bb)
        } else {
            null
        }
    }
    companion object {
        fun validateVersion() = Constants.FLATBUFFERS_23_5_26()
        fun getRootAsAnnotationElementMatcher(_bb: ByteBuffer): AnnotationElementMatcher = getRootAsAnnotationElementMatcher(_bb, AnnotationElementMatcher())
        fun getRootAsAnnotationElementMatcher(_bb: ByteBuffer, obj: AnnotationElementMatcher): AnnotationElementMatcher {
            _bb.order(ByteOrder.LITTLE_ENDIAN)
            return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb))
        }
        fun createAnnotationElementMatcher(builder: FlatBufferBuilder, nameOffset: Int, valueOffset: Int) : Int {
            builder.startTable(2)
            addValue(builder, valueOffset)
            addName(builder, nameOffset)
            return endAnnotationElementMatcher(builder)
        }
        fun startAnnotationElementMatcher(builder: FlatBufferBuilder) = builder.startTable(2)
        fun addName(builder: FlatBufferBuilder, name: Int) = builder.addOffset(0, name, 0)
        fun addValue(builder: FlatBufferBuilder, value: Int) = builder.addOffset(1, value, 0)
        fun endAnnotationElementMatcher(builder: FlatBufferBuilder) : Int {
            val o = builder.endTable()
            return o
        }
    }
}