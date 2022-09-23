#include <jni.h>
#include <android/log.h>
#include <dex_kit.h>
#include "DexKitJniHelper.h"

#define TAG "DexKit"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)

jclass dex_kit_class = nullptr;
jfieldID token_field = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitBridge_initDexKit(JNIEnv *env, jobject thiz, jstring apk_path) {
    if (!apk_path) {
        return;
    }
    dex_kit_class = env->GetObjectClass(thiz);
    token_field = env->GetFieldID(dex_kit_class, "token", "J");
    const char *cStr = env->GetStringUTFChars(apk_path, nullptr);
    LOGI("apkPath -> %s", cStr);
    std::string filePathStr(cStr);
    auto dexkit = new dexkit::DexKit(filePathStr);
    env->ReleaseStringUTFChars(apk_path, cStr);
    env->SetLongField(thiz, token_field, (jlong) dexkit);
}

extern "C"
JNIEXPORT jint JNICALL
Java_io_luckypray_dexkit_DexKitBridge_getDexNum(JNIEnv *env, jobject thiz) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return 0;
    }
    return GetDexNum(env, token);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitBridge_close(JNIEnv *env, jobject thiz) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return;
    }
    ReleaseDexKitInstance(env, token);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitBridge_batchFindClassesUsingStrings(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jobject map,
                                                                   jboolean advanced_match,
                                                                   jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return BatchFindClassesUsingStrings(env, token, map, advanced_match, dex_priority);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitBridge_batchFindMethodsUsingStrings(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jobject map,
                                                                   jboolean advanced_match,
                                                                   jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return BatchFindMethodsUsingStrings(env, token, map, advanced_match, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethodBeInvoked(JNIEnv *env, jobject thiz,
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
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodBeInvoked(env, token, method_descriptor, method_declare_class,
                               method_declare_name, method_return_type, method_param_types,
                               caller_method_declare_class, caller_method_declare_name,
                               caller_method_return_type, caller_method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethodInvoking(JNIEnv *env, jobject thiz,
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
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return FindMethodInvoking(env, token, method_descriptor, method_declare_class,
                              method_declare_name, method_return_type, method_param_types,
                              be_called_method_declare_class, be_called_method_declare_name,
                              be_called_method_return_type, be_called_method_param_types,
                              dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethodUsingField(JNIEnv *env, jobject thiz,
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
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodUsingField(env, token, field_descriptor, field_declare_class, field_name,
                                field_type, used_flags, caller_method_declare_class,
                                caller_method_name, caller_method_return_type,
                                caller_method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethodUsingString(JNIEnv *env, jobject thiz,
                                                            jstring used_string,
                                                            jboolean advanced_match,
                                                            jstring method_declare_class,
                                                            jstring method_name,
                                                            jstring method_return_type,
                                                            jobjectArray method_param_types,
                                                            jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodUsingString(env, token, used_string, advanced_match, method_declare_class,
                                 method_name, method_return_type, method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethod(JNIEnv *env, jobject thiz,
                                                 jstring method_declare_class,
                                                 jstring method_name,
                                                 jstring method_return_type,
                                                 jobjectArray method_param_types,
                                                 jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethod(env, token, method_declare_class, method_name, method_return_type,
                      method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findSubClasses(JNIEnv *env, jobject thiz,
                                                     jstring parent_class,
                                                     jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindSubClasses(env, token, parent_class, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitBridge_findMethodOpPrefixSeq(JNIEnv *env, jobject thiz,
                                                            jintArray op_prefix_seq,
                                                            jstring method_declare_class,
                                                            jstring method_name,
                                                            jstring method_return_type,
                                                            jobjectArray method_param_types,
                                                            jintArray dex_priority) {
    jlong token = env->GetLongField(thiz, token_field);
    if (token == 0) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodOpPrefixSeq(env, token, op_prefix_seq, method_declare_class, method_name,
                                 method_return_type, method_param_types, dex_priority);
}