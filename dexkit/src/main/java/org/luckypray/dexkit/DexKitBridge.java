package org.luckypray.dexkit;

import androidx.annotation.NonNull;

import com.google.flatbuffers.FlatBufferBuilder;

import org.luckypray.dexkit.descriptor.member.DexClassDescriptor;
import org.luckypray.dexkit.descriptor.member.DexFieldDescriptor;
import org.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DexKitBridge implements Closeable {

    private long token;

    private DexKitBridge(String apkPath) {
        token = nativeInitDexKit(apkPath);
    }

    private DexKitBridge(byte[][] dexBytesArray) {
        token = nativeInitDexKitByBytesArray(dexBytesArray);
    }

    private DexKitBridge(ClassLoader classLoader, boolean useMemoryDexFile) {
        token = nativeInitDexKitByClassLoader(classLoader, useMemoryDexFile);
    }

    private boolean isValid() {
        return token != 0L;
    }

    @Override
    public void close() throws IOException {
        if (isValid()) {
            nativeRelease(token);
            token = 0L;
        }
    }

    public void setThreadNum(int num) {
        nativeSetThreadNum(token, num);
    }

    public void exportDexFile(String outPath) {
        nativeExportDexFile(token, outPath);
    }

    @NonNull
    protected Map<String, List<DexClassDescriptor>> batchFindClassUsingStrings(FlatBufferBuilder fbb) {
        // TODO
        return new HashMap<>();
    }

    @NonNull
    protected Map<String, List<DexClassDescriptor>> batchFindMethodUsingStrings(FlatBufferBuilder fbb) {
        // TODO
        return new HashMap<>();
    }

    @NonNull
    protected List<DexClassDescriptor> findClass(FlatBufferBuilder fbb) {
        // TODO
        return new ArrayList<>();
    }

    @NonNull
    protected List<DexMethodDescriptor> findMethod(FlatBufferBuilder fbb) {
        // TODO
        return new ArrayList<>();
    }

    @NonNull
    protected List<DexFieldDescriptor> findField(FlatBufferBuilder fbb) {
        // TODO
        return new ArrayList<>();
    }

    public static DexKitBridge create(String apkPath) {
        return new DexKitBridge(apkPath);
    }

    public static DexKitBridge create(byte[][] dexBytesArray) {
        return new DexKitBridge(dexBytesArray);
    }

    public static DexKitBridge create(ClassLoader classLoader, boolean useMemoryDexFile) {
        return new DexKitBridge(classLoader, useMemoryDexFile);
    }

    private static native long nativeInitDexKit(String apkPath);

    private static native long nativeInitDexKitByBytesArray(byte[][] dexBytesArray);


    private static native long nativeInitDexKitByClassLoader(ClassLoader classLoader, boolean useMemoryDexFile);


    private static native void nativeSetThreadNum(long token, int num);


    private static native int nativeGetDexNum(long token);


    private static native void nativeRelease(long token);


    private static native void nativeExportDexFile(long token, String outPath);


    private static native byte[] nativeBatchFindClassUsingStrings(long token, byte[] bytes);


    private static native byte[] nativeBatchFindMethodUsingStrings(long token, byte[] bytes);


    private static native byte[] nativeFindClass(long token, byte[] bytes);


    private static native byte[] nativeFindMethod(long token, byte[] bytes);


    private static native byte[] nativeFindField(long token, byte[] bytes);

    @Override
    protected void finalize() throws Throwable {
        close();
    }
}
