#include <jni.h>
#include <android/log.h>
#include "dex_kit.h"
#include "dex_kit_jni_helper.h"

#define TAG "DexKit"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)

#define DEXKIT_JNI extern "C" JNIEXPORT JNICALL

DEXKIT_JNI jlong
Java_io_luckypray_dexkit_DexKitBridge_nativeInitDexKit(JNIEnv *env, jclass clazz,
                                                       jstring apk_path) {
    if (!apk_path) {
        return 0;
    }
    const char *cStr = env->GetStringUTFChars(apk_path, nullptr);
    LOGI("apkPath -> %s", cStr);
    std::string filePathStr(cStr);
    auto dexkit = new dexkit::DexKit(filePathStr);
    env->ReleaseStringUTFChars(apk_path, cStr);
    return (jlong) dexkit;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitBridge_nativeSetThreadNum(JNIEnv *env, jclass clazz,
                                                         jlong native_ptr, jint thread_num) {
    SetThreadNum(env, native_ptr, thread_num);
}

DEXKIT_JNI jint
Java_io_luckypray_dexkit_DexKitBridge_nativeGetDexNum(JNIEnv *env, jclass clazz,
                                                      jlong native_ptr) {
    return GetDexNum(env, native_ptr);
}

DEXKIT_JNI void
Java_io_luckypray_dexkit_DexKitBridge_nativeRelease(JNIEnv *env, jclass clazz,
                                                    jlong native_ptr) {
    ReleaseDexKitInstance(env, native_ptr);
}

DEXKIT_JNI jobject
Java_io_luckypray_dexkit_DexKitBridge_nativeBatchFindClassesUsingStrings(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jlong native_ptr,
                                                                         jobject map,
                                                                         jboolean advanced_match,
                                                                         jintArray dex_priority) {
    return BatchFindClassesUsingStrings(env, native_ptr, map, advanced_match, dex_priority);
}

DEXKIT_JNI jobject
Java_io_luckypray_dexkit_DexKitBridge_nativeBatchFindMethodsUsingStrings(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jlong native_ptr,
                                                                         jobject map,
                                                                         jboolean advanced_match,
                                                                         jintArray dex_priority) {
    return BatchFindMethodsUsingStrings(env, native_ptr, map, advanced_match, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodCaller(JNIEnv *env, jclass clazz,
                                                             jlong native_ptr,
                                                             jstring method_descriptor,
                                                             jstring method_declare_class,
                                                             jstring method_declare_name,
                                                             jstring method_return_type,
                                                             jobjectArray method_param_types,
                                                             jstring caller_method_declare_class,
                                                             jstring caller_method_declare_name,
                                                             jstring caller_method_return_type,
                                                             jobjectArray caller_method_param_types,
                                                             jintArray dex_priority) {
    return FindMethodCaller(env, native_ptr, method_descriptor, method_declare_class,
                            method_declare_name, method_return_type, method_param_types,
                            caller_method_declare_class, caller_method_declare_name,
                            caller_method_return_type, caller_method_param_types, dex_priority);
}

DEXKIT_JNI jobject
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodInvoking(JNIEnv *env, jclass clazz,
                                                               jlong native_ptr,
                                                               jstring method_descriptor,
                                                               jstring method_declare_class,
                                                               jstring method_declare_name,
                                                               jstring method_return_type,
                                                               jobjectArray method_param_types,
                                                               jstring be_called_method_declare_class,
                                                               jstring be_called_method_declare_name,
                                                               jstring be_called_method_return_type,
                                                               jobjectArray be_called_method_param_types,
                                                               jintArray dex_priority) {
    return FindMethodInvoking(env, native_ptr, method_descriptor, method_declare_class,
                              method_declare_name, method_return_type, method_param_types,
                              be_called_method_declare_class, be_called_method_declare_name,
                              be_called_method_return_type, be_called_method_param_types,
                              dex_priority);
}

DEXKIT_JNI jobject
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingField(JNIEnv *env, jclass clazz,
                                                                 jlong native_ptr,
                                                                 jstring field_descriptor,
                                                                 jstring field_declare_class,
                                                                 jstring field_name,
                                                                 jstring field_type,
                                                                 jint used_flags,
                                                                 jstring caller_method_declare_class,
                                                                 jstring caller_method_name,
                                                                 jstring caller_method_return_type,
                                                                 jobjectArray caller_method_param_types,
                                                                 jintArray dex_priority) {
    return FindMethodUsingField(env, native_ptr, field_descriptor, field_declare_class, field_name,
                                field_type, used_flags, caller_method_declare_class,
                                caller_method_name, caller_method_return_type,
                                caller_method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingString(JNIEnv *env, jclass clazz,
                                                                  jlong native_ptr,
                                                                  jstring used_string,
                                                                  jboolean advanced_match,
                                                                  jstring method_declare_class,
                                                                  jstring method_name,
                                                                  jstring method_return_type,
                                                                  jobjectArray method_param_types,
                                                                  jintArray dex_priority) {
    return FindMethodUsingString(env, native_ptr, used_string, advanced_match, method_declare_class,
                                 method_name, method_return_type, method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethod(JNIEnv *env, jclass clazz,
                                                       jlong native_ptr,
                                                       jstring method_declare_class,
                                                       jstring method_name,
                                                       jstring method_return_type,
                                                       jobjectArray method_param_types,
                                                       jintArray dex_priority) {
    return FindMethod(env, native_ptr, method_declare_class, method_name, method_return_type,
                      method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindSubClasses(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr,
                                                           jstring parent_class,
                                                           jintArray dex_priority) {
    return FindSubClasses(env, native_ptr, parent_class, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodOpPrefixSeq(JNIEnv *env, jclass clazz,
                                                                  jlong native_ptr,
                                                                  jintArray op_prefix_seq,
                                                                  jstring method_declare_class,
                                                                  jstring method_name,
                                                                  jstring method_return_type,
                                                                  jobjectArray method_param_types,
                                                                  jintArray dex_priority) {
    return FindMethodOpPrefixSeq(env, native_ptr, op_prefix_seq, method_declare_class, method_name,
                                 method_return_type, method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingOpCodeSeq(JNIEnv *env, jclass clazz,
                                                                     jlong native_ptr,
                                                                     jintArray op_seq,
                                                                     jstring method_declare_class,
                                                                     jstring method_name,
                                                                     jstring method_return_type,
                                                                     jobjectArray method_param_types,
                                                                     jintArray dex_priority) {
    return FindMethodUsingOpCodeSeq(env, native_ptr, op_seq, method_declare_class, method_name,
                                    method_return_type, method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitBridge_nativeGetMethodOpCodeSeq(JNIEnv *env, jclass clazz,
                                                               jlong native_ptr,
                                                               jstring method_descriptor,
                                                               jstring method_declare_class,
                                                               jstring method_name,
                                                               jstring method_return_type,
                                                               jobjectArray method_param_types,
                                                               jintArray dex_priority) {
    return GetMethodOpCodeSeq(env, native_ptr, method_descriptor, method_declare_class, method_name,
                              method_return_type, method_param_types, dex_priority);
}