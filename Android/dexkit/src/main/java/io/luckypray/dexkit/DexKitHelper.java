package io.luckypray.dexkit;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.Set;

public class DexKitHelper {

    public static final int FLAG_GETTING = 0x00000001;
    public static final int FLAG_SETTING = 0x00000002;
    public static final int FLAG_USING = FLAG_GETTING | FLAG_SETTING;

    public DexKitHelper(@NonNull ClassLoader classLoader) {
        initDexKit(classLoader);
    }

    public DexKitHelper(@NonNull String apkPath) {
        initDexKitByPath(apkPath);
    }

    private long token;

    private native void initDexKit(@NonNull ClassLoader classLoader);

    private native void initDexKitByPath(@NonNull String apkPath);

    public native void close();

    @NonNull
    public native Map<String, String[]> batchFindClassesUsingStrings(
        @NonNull Map<String, Set<String>> map,
        boolean advancedMatch,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native Map<String, String[]> batchFindMethodsUsingStrings(
        @NonNull Map<String, Set<String>> map,
        boolean advancedMatch,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethodBeInvoked(
        @NonNull String methodDescriptor,
        @NonNull String methodDeclareClass,
        @NonNull String methodName,
        @NonNull String methodReturnType,
        @Nullable String[] methodParameterTypes,
        @NonNull String callerMethodDeclareClass,
        @NonNull String callerMethodName,
        @NonNull String callerMethodReturnType,
        @Nullable String[] callerMethodParameterTypes,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethodInvoking(
        @NonNull String methodDescriptor,
        @NonNull String methodDeclareClass,
        @NonNull String methodName,
        @NonNull String methodReturnType,
        @Nullable String[] methodParameterTypes,
        @NonNull String beCalledMethodDeclareClass,
        @NonNull String beCalledMethodName,
        @NonNull String beCalledMethodReturnType,
        @Nullable String[] beCalledMethodParamTypes,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethodUsingField(
        @NonNull String fieldDescriptor,
        @NonNull String fieldDeclareClass,
        @NonNull String fieldName,
        @NonNull String fieldType,
        int usedFlags,
        @NonNull String callerMethodDeclareClass,
        @NonNull String callerMethodName,
        @NonNull String callerMethodReturnType,
        @Nullable String[] callerMethodParamTypes,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethodUsingString(
        @NonNull String usingString,
        boolean advancedMatch,
        @NonNull String methodDeclareClass,
        @NonNull String methodName,
        @NonNull String methodReturnType,
        @Nullable String[] methodParamTypes,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethod(
        @NonNull String methodDeclareClass,
        @NonNull String methodName,
        @NonNull String methodReturnType,
        @Nullable String[] methodParamTypes,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findSubClasses(
        @NonNull String parentClass,
        @Nullable int[] dexPriority
    );

    @NonNull
    public native String[] findMethodOpPrefixSeq(
        @NonNull int[] opPrefixSeq,
        @NonNull String methodDeclareClass,
        @NonNull String methodName,
        @NonNull String methodReturnType,
        @Nullable String[] methodParamTypes,
        @Nullable int[] dexPriority
    );
}
