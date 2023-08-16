package org.luckypray.dexkit

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.BatchUsingStringsMatcher
import org.luckypray.StringMatchType
import org.luckypray.StringMatcher
import java.nio.ByteBuffer


fun main() {
    val fbb = FlatBufferBuilder()
    val usingStrings = fbb.createVectorOfTables(
        intArrayOf(
            StringMatcher.createStringMatcher(
                fbb,
                fbb.createString("abc"),
                StringMatchType.EndWith,
                false
            )
        )
    )
    // 所有变量都要在start前创建!!! 必须！！！或者使用create方法传入完整参数的同时构建
    val unionKey = fbb.createString("abc")
    BatchUsingStringsMatcher.startBatchUsingStringsMatcher(fbb)
    BatchUsingStringsMatcher.addUnionKey(fbb, unionKey)
    BatchUsingStringsMatcher.addUsingStrings(fbb, usingStrings)
    val root = BatchUsingStringsMatcher.endBatchUsingStringsMatcher(fbb)
    fbb.finish(root)

    // serialize to buffer
    val buf = fbb.sizedByteArray()
    println("buf size: ${buf.size}")

    // deserialize from buffer
    val batchUsingStringsMatcher = BatchUsingStringsMatcher.getRootAsBatchUsingStringsMatcher(
        ByteBuffer.wrap(buf)
    )

    println(batchUsingStringsMatcher.unionKey)
    for (i in 0 until batchUsingStringsMatcher.usingStringsLength) {
        batchUsingStringsMatcher.usingStrings(i)?.let {
            println(it.value)
            println(it.matchType)
            println(it.ignoreCase)
        }
    }
}