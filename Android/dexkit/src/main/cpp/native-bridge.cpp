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

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitHelper_initDexKit(JNIEnv *env, jobject thiz,
                                                 jobject class_loader) {
    if (!class_loader) {
        return;
    }
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jclass cClassloader = env->FindClass("java/lang/ClassLoader");
    jmethodID mGetResource = env->GetMethodID(cClassloader, "findResource",
                                              "(Ljava/lang/String;)Ljava/net/URL;");
    jstring manifestPath = env->NewStringUTF("AndroidManifest.xml");
    jobject url = env->CallObjectMethod(class_loader, mGetResource, manifestPath);
    jclass cURL = env->FindClass("java/net/URL");
    jmethodID mGetPath = env->GetMethodID(cURL, "getPath", "()Ljava/lang/String;");
    auto file = (jstring) env->CallObjectMethod(url, mGetPath);
    const char *cStr = env->GetStringUTFChars(file, nullptr);
    std::string filePathStr(cStr);
    std::string hostApkPath = filePathStr.substr(5, filePathStr.size() - 26);
    LOGI("hostApkPath: %s", hostApkPath.c_str());
    auto dexkit = new dexkit::DexKit(hostApkPath);
    env->ReleaseStringUTFChars(file, cStr);
    env->SetLongField(thiz, fToken, (jlong) dexkit);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitHelper_initDexKitByPath(JNIEnv *env, jobject thiz, jstring apk_path) {
    if (!apk_path) {
        return;
    }
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    const char *cStr = env->GetStringUTFChars(apk_path, nullptr);
    std::string filePathStr(cStr);
    auto dexkit = new dexkit::DexKit(filePathStr);
    env->ReleaseStringUTFChars(apk_path, cStr);
    env->SetLongField(thiz, fToken, (jlong) dexkit);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_luckypray_dexkit_DexKitHelper_close(JNIEnv *env, jobject thiz) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    ReleaseDexKitInstance(env, token);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitHelper_batchFindClassesUsingStrings(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jobject map,
                                                                   jboolean advanced_match,
                                                                   jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return BatchFindClassesUsingStrings(env, token, map, advanced_match, dex_priority);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitHelper_batchFindMethodsUsingStrings(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jobject map,
                                                                  jboolean advanced_match,
                                                                  jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return BatchFindMethodsUsingStrings(env, token, map, advanced_match, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethodBeInvoked(JNIEnv *env, jobject thiz,
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
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethodBeInvoked(env, token, method_descriptor, method_declare_class,
                               method_declare_name, method_return_type, method_param_types,
                               caller_method_declare_class, caller_method_declare_name,
                               caller_method_return_type, caller_method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethodInvoking(JNIEnv *env, jobject thiz,
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
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethodInvoking(env, token, method_descriptor, method_declare_class,
                              method_declare_name, method_return_type, method_param_types,
                              be_called_method_declare_class, be_called_method_declare_name,
                              be_called_method_return_type, be_called_method_param_types,
                              dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethodUsingField(JNIEnv *env, jobject thiz,
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
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethodUsingField(env, token, field_descriptor, field_declare_class, field_name,
                               field_type, used_flags, caller_method_declare_class,
                               caller_method_name, caller_method_return_type,
                               caller_method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethodUsingString(JNIEnv *env, jobject thiz,
                                                            jstring used_string,
                                                            jboolean advanced_match,
                                                            jstring method_declare_class,
                                                            jstring method_name,
                                                            jstring method_return_type,
                                                            jobjectArray method_param_types,
                                                            jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethodUsingString(env, token, used_string, advanced_match, method_declare_class,
                                method_name, method_return_type, method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethod(JNIEnv *env, jobject thiz,
                                                 jstring method_declare_class,
                                                 jstring method_name,
                                                 jstring method_return_type,
                                                 jobjectArray method_param_types,
                                                 jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethod(env, token, method_declare_class, method_name, method_return_type,
                      method_param_types, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findSubClasses(JNIEnv *env, jobject thiz,
                                                     jstring parent_class,
                                                     jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindSubClasses(env, token, parent_class, dex_priority);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_io_luckypray_dexkit_DexKitHelper_findMethodOpPrefixSeq(JNIEnv *env, jobject thiz,
                                                            jintArray op_prefix_seq,
                                                            jstring method_declare_class,
                                                            jstring method_name,
                                                            jstring method_return_type,
                                                            jobjectArray method_param_types,
                                                            jintArray dex_priority) {
    jclass cHelper = env->GetObjectClass(thiz);
    jfieldID fToken = env->GetFieldID(cHelper, "token", "J");
    jlong token = env->GetLongField(thiz, fToken);
    return FindMethodOpPrefixSeq(env, token, op_prefix_seq, method_declare_class, method_name,
                                 method_return_type, method_param_types, dex_priority);
}