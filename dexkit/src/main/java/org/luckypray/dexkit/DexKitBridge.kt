@file:Suppress("MemberVisibilityCanBePrivate", "unused", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package org.luckypray.dexkit

import com.google.flatbuffers.FlatBufferBuilder
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.query.BatchFindClassUsingStrings
import org.luckypray.dexkit.query.BatchFindMethodUsingStrings
import org.luckypray.dexkit.query.ClassDataList
import org.luckypray.dexkit.query.FieldDataList
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.FindField
import org.luckypray.dexkit.query.FindMethod
import org.luckypray.dexkit.query.MethodDataList
import org.luckypray.dexkit.result.AnnotationData
import java.io.Closeable
import java.nio.ByteBuffer

class DexKitBridge : Closeable {

    private var token: Long = 0L
        get() {
            if (field == 0L) {
                throw IllegalStateException("DexKitBridge is not valid")
            }
            return field
        }

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

    fun findClass(findClass: FindClass): ClassDataList {
        val fbb = FlatBufferBuilder()
        findClass.build(fbb)
        return this.findClass(fbb)
    }

    fun findMethod(findMethod: FindMethod): MethodDataList {
        val fbb = FlatBufferBuilder()
        findMethod.build(fbb)
        return this.findMethod(fbb)
    }

    fun findField(fieldMatcher: FindField): FieldDataList {
        val fbb = FlatBufferBuilder()
        fieldMatcher.build(fbb)
        return this.findField(fbb)
    }

    // region DSL

    @kotlin.internal.InlineOnly
    inline fun batchFindClassUsingStrings(init: BatchFindClassUsingStrings.() -> Unit): Map<String, List<ClassData>> {
        return batchFindClassUsingStrings(BatchFindClassUsingStrings().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun batchFindMethodUsingStrings(init: BatchFindMethodUsingStrings.() -> Unit): Map<String, List<MethodData>> {
        return batchFindMethodUsingStrings(BatchFindMethodUsingStrings().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun findClass(init: FindClass.() -> Unit): ClassDataList {
        return findClass(FindClass().apply(init))
    }

    @kotlin.internal.InlineOnly
    inline fun findMethod(init: FindMethod.() -> Unit): MethodDataList {
        return findMethod(FindMethod().apply(init))
    }
    @kotlin.internal.InlineOnly
    inline fun findField(init: FindField.() -> Unit): FieldDataList {
        return findField(FindField().apply(init))
    }

    // endregion

    /**
     * find class by [BatchFindMethodUsingStrings]'s [FlatBufferBuilder]
     */
    @kotlin.internal.InlineOnly
    internal inline fun batchFindClassUsingStrings(fbb: FlatBufferBuilder): Map<String, ClassDataList> {
        val res = nativeBatchFindClassUsingStrings(token, fbb.sizedByteArray())
        val holder = InnerBatchClassMetaArrayHolder.getRootAsBatchClassMetaArrayHolder(ByteBuffer.wrap(res))
        val map = HashMap<String, ClassDataList>()
        for (i in 0 until holder.itemsLength) {
            val items = holder.items(i)!!
            val key = items.unionKey!!
            val batchFindMeta = ClassDataList()
            for (j in 0 until items.classesLength) {
                batchFindMeta.add(ClassData.from(this@DexKitBridge, items.classes(j)!!))
            }
            batchFindMeta.sortBy { it.dexDescriptor }
            map[key] = batchFindMeta
        }
        return map
    }

    /**
     * find class by [BatchFindClassUsingStrings]'s [FlatBufferBuilder]
     */
    @kotlin.internal.InlineOnly
    internal inline fun batchFindMethodUsingStrings(fbb: FlatBufferBuilder): Map<String, MethodDataList> {
        val res = nativeBatchFindMethodUsingStrings(token, fbb.sizedByteArray())
        val holder = InnerBatchMethodMetaArrayHolder.getRootAsBatchMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val map = HashMap<String, MethodDataList>()
        for (i in 0 until holder.itemsLength) {
            val items = holder.items(i)!!
            val key = items.unionKey!!
            val batchFindMeta = MethodDataList()
            for (j in 0 until items.methodsLength) {
                batchFindMeta.add(MethodData.from(this@DexKitBridge, items.methods(j)!!))
            }
            batchFindMeta.sortBy { it.dexDescriptor }
            map[key] = batchFindMeta
        }
        return map
    }

    /**
     * find class by [FindClass]'s [FlatBufferBuilder]
     */
    @kotlin.internal.InlineOnly
    internal inline fun findClass(fbb: FlatBufferBuilder): ClassDataList {
        val res = nativeFindClass(token, fbb.sizedByteArray())
        val holder = InnerClassMetaArrayHolder.getRootAsClassMetaArrayHolder(ByteBuffer.wrap(res))
        val list = ClassDataList()
        for (i in 0 until holder.classesLength) {
            list.add(ClassData.from(this@DexKitBridge, holder.classes(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    /**
     * find method by [FindMethod]'s [FlatBufferBuilder]
     */
    @kotlin.internal.InlineOnly
    internal inline fun findMethod(fbb: FlatBufferBuilder): MethodDataList {
        val res = nativeFindMethod(token, fbb.sizedByteArray())
        val holder = InnerMethodMetaArrayHolder.getRootAsMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val list = MethodDataList()
        for (i in 0 until holder.methodsLength) {
            list.add(MethodData.from(this@DexKitBridge, holder.methods(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    /**
     * find method by [FindMethod]'s [FlatBufferBuilder]
     */
    @kotlin.internal.InlineOnly
    internal inline fun findField(fbb: FlatBufferBuilder): FieldDataList {
        val res = nativeFindField(token, fbb.sizedByteArray())
        val holder = InnerFieldMetaArrayHolder.getRootAsFieldMetaArrayHolder(ByteBuffer.wrap(res))
        val list = FieldDataList()
        for (i in 0 until holder.fieldsLength) {
            list.add(FieldData.from(this@DexKitBridge, holder.fields(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getClassByIds(encodeIdArray: LongArray): ClassDataList {
        val res = nativeGetClassByIds(token, encodeIdArray)
        val holder = InnerClassMetaArrayHolder.getRootAsClassMetaArrayHolder(ByteBuffer.wrap(res))
        val list = ClassDataList()
        for (i in 0 until holder.classesLength) {
            list.add(ClassData.from(this@DexKitBridge, holder.classes(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getMethodByIds(encodeIdArray: LongArray): MethodDataList {
        val res = nativeGetMethodByIds(token, encodeIdArray)
        val holder = InnerMethodMetaArrayHolder.getRootAsMethodMetaArrayHolder(ByteBuffer.wrap(res))
        val list = MethodDataList()
        for (i in 0 until holder.methodsLength) {
            list.add(MethodData.from(this@DexKitBridge, holder.methods(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getFieldByIds(encodeIdArray: LongArray): FieldDataList {
        val res = nativeGetFieldByIds(token, encodeIdArray)
        val holder = InnerFieldMetaArrayHolder.getRootAsFieldMetaArrayHolder(ByteBuffer.wrap(res))
        val list = FieldDataList()
        for (i in 0 until holder.fieldsLength) {
            list.add(FieldData.from(this@DexKitBridge, holder.fields(i)!!))
        }
        list.sortBy { it.dexDescriptor }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getClassAnnotations(classId: Long): List<AnnotationData> {
        val res = nativeGetClassAnnotations(token, classId)
        val holder = InnerAnnotationMetaArrayHolder.getRootAsAnnotationMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<AnnotationData>()
        for (i in 0 until holder.annotationsLength) {
            list.add(AnnotationData.from(this@DexKitBridge, holder.annotations(i)!!))
        }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getFieldAnnotations(fieldId: Long): List<AnnotationData> {
        val res = nativeGetFieldAnnotations(token, fieldId)
        val holder = InnerAnnotationMetaArrayHolder.getRootAsAnnotationMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<AnnotationData>()
        for (i in 0 until holder.annotationsLength) {
            list.add(AnnotationData.from(this@DexKitBridge, holder.annotations(i)!!))
        }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getMethodAnnotations(methodId: Long): List<AnnotationData> {
        val res = nativeGetMethodAnnotations(token, methodId)
        val holder = InnerAnnotationMetaArrayHolder.getRootAsAnnotationMetaArrayHolder(ByteBuffer.wrap(res))
        val list = mutableListOf<AnnotationData>()
        for (i in 0 until holder.annotationsLength) {
            list.add(AnnotationData.from(this@DexKitBridge, holder.annotations(i)!!))
        }
        return list
    }

    @kotlin.internal.InlineOnly
    internal inline fun getParameterNames(encodeId: Long): List<String?>? {
        return nativeGetParameterNames(token, encodeId)?.map { it }
    }

    @kotlin.internal.InlineOnly
    internal inline fun getParameterAnnotations(methodId: Long): List<List<AnnotationData>> {
        val res = nativeGetParameterAnnotations(token, methodId)
        val holder = InnerParametersAnnotationMetaArrayHoler.getRootAsParametersAnnotationMetaArrayHoler(ByteBuffer.wrap(res))
        val list = mutableListOf<List<AnnotationData>>()
        for (i in 0 until holder.annotationsArrayLength) {
            val item = holder.annotationsArray(i)!!
            val annotations = mutableListOf<AnnotationData>()
            for (j in 0 until item.annotationsLength) {
                annotations.add(AnnotationData.from(this@DexKitBridge, item.annotations(j)!!))
            }
            list.add(annotations)
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
        private external fun nativeGetClassByIds(nativePtr: Long, ids: LongArray): ByteArray

        @JvmStatic
        private external fun nativeGetMethodByIds(nativePtr: Long, ids: LongArray): ByteArray

        @JvmStatic
        private external fun nativeGetFieldByIds(nativePtr: Long, ids: LongArray): ByteArray

        @JvmStatic
        private external fun nativeGetClassAnnotations(nativePtr: Long, classId: Long): ByteArray

        @JvmStatic
        private external fun nativeGetFieldAnnotations(nativePtr: Long, fieldId: Long): ByteArray

        @JvmStatic
        private external fun nativeGetMethodAnnotations(nativePtr: Long, methodId: Long): ByteArray

        @JvmStatic
        private external fun nativeGetParameterNames(nativePtr: Long, methodId: Long): Array<String?>?

        @JvmStatic
        private external fun nativeGetParameterAnnotations(nativePtr: Long, methodId: Long): ByteArray

    }

    protected fun finalize() {
        close()
    }
}
