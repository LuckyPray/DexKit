@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.luckypray.dexkit

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.alias.InnerBatchClassMetaArrayHolder
import org.luckypray.dexkit.alias.InnerBatchMethodMetaArrayHolder
import org.luckypray.dexkit.alias.InnerClassMetaArrayHolder
import org.luckypray.dexkit.alias.InnerFieldMetaArrayHolder
import org.luckypray.dexkit.alias.InnerMethodMetaArrayHolder
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.query.BatchFindClassUsingStrings
import org.luckypray.dexkit.query.BatchFindMethodUsingStrings
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import java.io.Closeable
import java.nio.ByteBuffer

class DexKitBridge : Closeable {

    private var token: Long = 0L

    private constructor(apkPath: String) {
        token = nativeInitDexKit(apkPath)
    }

    private constructor(dexBytesArray: Array<ByteArray>) {
        token = nativeInitDexKitByBytesArray(dexBytesArray)
    }

    private constructor(classLoader: ClassLoader, useMemoryDexFile: Boolean) {
        token = nativeInitDexKitByClassLoader(classLoader, useMemoryDexFile)
    }

    /**
     * DexKit is valid only when token is not 0
     */
    val isValid
        get() = token != 0L

    /**
     * release native resource
     */
    @Synchronized
    override fun close() {
        if (isValid) {
            nativeRelease(token)
            token = 0L
        }
    }

    /**
     * set DexKit work thread number
     *
     * @param [num] work thread number
     */
    fun setThreadNum(num: Int) {
        nativeSetThreadNum(token, num)
    }

    /**
     * Get all parsed dex counts.
     *
     * @return The number of dex parsed by DexKit
     */
    fun getDexNum(): Int {
        return nativeGetDexNum(token)
    }

    /**
     * write all dex file to [outPath]
     *
     * @param [outPath] dex file output path
     *
     * @since 1.1.0
     */
    fun exportDexFile(outPath: String) {
        nativeExportDexFile(token, outPath)
    }

    fun batchFindClassUsingStrings(batchFind: BatchFindClassUsingStrings): Map<String, List<ClassData>> {
        val fbb = FlatBufferBuilder()
        batchFind.build(fbb)
        return this.batchFindClassUsingStrings(fbb)
    }

    fun batchFindMethodUsingStrings(batchFind: BatchFindMethodUsingStrings): Map<String, List<MethodData>> {
        val fbb = FlatBufferBuilder()
        batchFind.build(fbb)
        return this.batchFindMethodUsingStrings(fbb)
    }

    fun findClass(findClass: FindClass): List<ClassData> {
        val fbb = FlatBufferBuilder()
        findClass.build(fbb)
        return this.findClass(fbb)
    }

    fun findMethod(findMethod: FindMethod): List<MethodData> {
        val fbb = FlatBufferBuilder()
        findMethod.build(fbb)
        return this.findMethod(fbb)
    }

    fun findField(fieldMatcher: FindField): List<FieldData> {
        val fbb = FlatBufferBuilder()
        fieldMatcher.build(fbb)
        return this.findField(fbb)
    }

    // region DSL

    fun batchFindClassUsingStrings(init: BatchFindClassUsingStrings.() -> Unit): Map<String, List<ClassData>> {
        return batchFindClassUsingStrings(BatchFindClassUsingStrings().apply(init))
    }

    fun batchFindMethodUsingStrings(init: BatchFindMethodUsingStrings.() -> Unit): Map<String, List<MethodData>> {
        return batchFindMethodUsingStrings(BatchFindMethodUsingStrings().apply(init))
    }

    fun findClass(init: FindClass.() -> Unit): List<ClassData> {
        return findClass(FindClass().apply(init))
    }

    fun findMethod(init: FindMethod.() -> Unit): List<MethodData> {
        return findMethod(FindMethod().apply(init))
    }
    fun findField(init: FindField.() -> Unit): List<FieldData> {
        return findField(FindField().apply(init))
    }

    // endregion

    /**
     * find class by [BatchFindMethodUsingStrings]'s [FlatBufferBuilder]
     */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun batchFindClassUsingStrings(fbb: FlatBufferBuilder): Map<String, List<ClassData>> {
        val res = nativeBatchFindClassUsingStrings(token, fbb.sizedByteArray())
        val holder = InnerBatchClassMetaArrayHolder.getRootAsBatchClassMetaArrayHolder(ByteBuffer.wrap(res))
        val map = HashMap<String, MutableList<ClassData>>()
        for (i in 0 until holder.itemsLength) {
            val items = holder.items(i)!!
            val key = items.unionKey!!
            val batchFindMeta = mutableListOf<ClassData>().apply {
                for (j in 0 until items.classesLength) {
                    add(ClassData(this@DexKitBridge, items.classes(j)!!))
                }
            }
            map[key] = batchFindMeta
        }
        return HashMap()
    }

    /**
     * find class by [BatchFindClassUsingStrings]'s [FlatBufferBuilder]
     */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun batchFindMethodUsingStrings(fbb: FlatBufferBuilder): Map<String, List<MethodData>> {
        val res = nativeBatchFindMethodUsingStrings(token, fbb.sizedByteArray())
        val holder = InnerBatchMethodMetaArrayHolder.getRootAsBatchMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val map = HashMap<String, MutableList<MethodData>>()
        for (i in 0 until holder.itemsLength) {
            val items = holder.items(i)!!
            val key = items.unionKey!!
            val batchFindMeta = mutableListOf<MethodData>().apply {
                for (j in 0 until items.methodsLength) {
                    add(MethodData(this@DexKitBridge, items.methods(j)!!))
                }
            }
            map[key] = batchFindMeta
        }
        return map
    }

    /**
     * find class by [FindClass]'s [FlatBufferBuilder]
     */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun findClass(fbb: FlatBufferBuilder): List<ClassData> {
        val res = nativeFindClass(token, fbb.sizedByteArray())
        val holder = InnerClassMetaArrayHolder.getRootAsClassMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<ClassData>()
        for (i in 0 until holder.classesLength) {
            list.add(ClassData(this@DexKitBridge, holder.classes(i)!!))
        }
        return list
    }

    /**
     * find method by [FindMethod]'s [FlatBufferBuilder]
     */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun findMethod(fbb: FlatBufferBuilder): List<MethodData> {
        val res = nativeFindMethod(token, fbb.sizedByteArray())
        val holder = InnerMethodMetaArrayHolder.getRootAsMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<MethodData>()
        for (i in 0 until holder.methodsLength) {
            list.add(MethodData(this@DexKitBridge, holder.methods(i)!!))
        }
        return list
    }

    /**
     * find method by [FindMethod]'s [FlatBufferBuilder]
     */
    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun findField(fbb: FlatBufferBuilder): List<FieldData> {
        val res = nativeFindField(token, fbb.sizedByteArray())
        val holder = InnerFieldMetaArrayHolder.getRootAsFieldMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<FieldData>()
        for (i in 0 until holder.fieldsLength) {
            list.add(FieldData(this@DexKitBridge, holder.fields(i)!!))
        }
        return list
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun getClassByIds(ids: IntArray): List<ClassData> {
        val res = nativeGetClassByIds(token, ids)
        val holder = InnerClassMetaArrayHolder.getRootAsClassMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<ClassData>()
        for (i in 0 until holder.classesLength) {
            list.add(ClassData(this@DexKitBridge, holder.classes(i)!!))
        }
        return list
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun getMethodByIds(ids: IntArray): List<MethodData> {
        val res = nativeGetMethodByIds(token, ids)
        val holder = InnerMethodMetaArrayHolder.getRootAsMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<MethodData>()
        for (i in 0 until holder.methodsLength) {
            list.add(MethodData(this@DexKitBridge, holder.methods(i)!!))
        }
        return list
    }

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
    @kotlin.internal.InlineOnly
    internal fun getFieldByIds(ids: IntArray): List<FieldData> {
        val res = nativeGetFieldByIds(token, ids)
        val holder = InnerFieldMetaArrayHolder.getRootAsFieldMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<FieldData>()
        for (i in 0 until holder.fieldsLength) {
            list.add(FieldData(this@DexKitBridge, holder.fields(i)!!))
        }
        return list
    }

    companion object {
        @JvmStatic
        fun create(apkPath: String): DexKitBridge? {
            val helper = DexKitBridge(apkPath)
            return if (helper.isValid) helper else null
        }

        @JvmStatic
        fun create(dexBytesArray: Array<ByteArray>): DexKitBridge? {
            val helper = DexKitBridge(dexBytesArray)
            return if (helper.isValid) helper else null
        }

        /**
         *
         * @param loader class loader
         * @param useMemoryDexFile
         * if true, will try to use cookie to load memory dex file. else will use dex file path.
         * if cookies file contains CompactDex, will use apkPath to load dex.
         * if contains OatDex, Some functions may not work properly.
         */
        @JvmStatic
        fun create(loader: ClassLoader, useMemoryDexFile: Boolean): DexKitBridge? {
            val helper = DexKitBridge(loader, useMemoryDexFile)
            return if (helper.isValid) helper else null
        }

        @JvmStatic
        private external fun nativeInitDexKit(apkPath: String): Long

        @JvmStatic
        private external fun nativeInitDexKitByBytesArray(dexBytesArray: Array<ByteArray>): Long

        @JvmStatic
        private external fun nativeInitDexKitByClassLoader(
            loader: ClassLoader,
            useMemoryDexFile: Boolean
        ): Long

        @JvmStatic
        private external fun nativeSetThreadNum(nativePtr: Long, threadNum: Int)

        @JvmStatic
        private external fun nativeGetDexNum(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeRelease(nativePtr: Long)

        @JvmStatic
        private external fun nativeExportDexFile(nativePtr: Long, outDir: String)

        @JvmStatic
        private external fun nativeBatchFindClassUsingStrings(nativePtr: Long, bytes: ByteArray): ByteArray

        @JvmStatic
        private external fun nativeBatchFindMethodUsingStrings(nativePtr: Long, bytes: ByteArray): ByteArray

        @JvmStatic
        private external fun nativeFindClass(nativePtr: Long, bytes: ByteArray): ByteArray

        @JvmStatic
        private external fun nativeFindMethod(nativePtr: Long, bytes: ByteArray): ByteArray

        @JvmStatic
        private external fun nativeFindField(nativePtr: Long, bytes: ByteArray): ByteArray

        @JvmStatic
        private external fun nativeGetClassByIds(nativePtr: Long, ids: IntArray): ByteArray

        @JvmStatic
        private external fun nativeGetMethodByIds(nativePtr: Long, ids: IntArray): ByteArray

        @JvmStatic
        private external fun nativeGetFieldByIds(nativePtr: Long, ids: IntArray): ByteArray

    }

    protected fun finalize() {
        close()
    }
}
