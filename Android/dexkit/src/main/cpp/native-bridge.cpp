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

#define DEXKIT_JNI JNIEXPORT JNICALL extern "C"

static jfieldID path_list_field = nullptr;
static jfieldID element_field = nullptr;
static jfieldID dex_file_field = nullptr;
static jfieldID cookie_field = nullptr;
static jfieldID file_name_field = nullptr;
static bool is_initialized = false;

struct DexFile {
    const void *begin_{};
    size_t size_{};

    virtual ~DexFile() = default;
};

static bool IsCompactDexFile(const void *image) {
    const auto *header = reinterpret_cast<const struct dex::Header *>(image);
    if (header->magic[0] == 'c' && header->magic[1] == 'd' &&
        header->magic[2] == 'e' && header->magic[3] == 'x') {
        return true;
    }
    return false;
}

static bool CheckPoint(void *addr) {
    int nullfd = open("/dev/random", O_WRONLY);
    bool valid = true;
    if (write(nullfd, (void *) addr, sizeof(addr)) < 0) {
        valid = false;
    }
    close(nullfd);
    return valid;
}

void init(JNIEnv *env) {
    if (is_initialized) {
        return;
    }
    auto dex_class_loader = env->FindClass("dalvik/system/BaseDexClassLoader");
    path_list_field = env->GetFieldID(dex_class_loader, "pathList",
                                      "Ldalvik/system/DexPathList;");
    auto dex_path_list = env->FindClass("dalvik/system/DexPathList");
    element_field = env->GetFieldID(dex_path_list, "dexElements",
                                    "[Ldalvik/system/DexPathList$Element;");
    auto element = env->FindClass("dalvik/system/DexPathList$Element");
    dex_file_field =
            env->GetFieldID(element, "dexFile", "Ldalvik/system/DexFile;");
    auto dex_file = env->FindClass("dalvik/system/DexFile");
    cookie_field = env->GetFieldID(dex_file, "mCookie", "Ljava/lang/Object;");
    file_name_field = env->GetFieldID(dex_file, "mFileName", "Ljava/lang/String;");

    is_initialized = true;
}

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

DEXKIT_JNI jlong
Java_io_luckypray_dexkit_DexKitBridge_nativeInitDexKitByClassLoader(JNIEnv *env, jclass clazz,
                                                                    jobject class_loader,
                                                                    jboolean use_memory_dex_file) {
    if (!class_loader) {
        return 0;
    }
    init(env);
    auto path_list = env->GetObjectField(class_loader, path_list_field);
    if (!path_list)
        return 0;
    auto elements = (jobjectArray) env->GetObjectField(path_list, element_field);
    if (!elements)
        return 0;
    LOGD("elements size -> %d", env->GetArrayLength(elements));
    auto dexkit = new dexkit::DexKit();
    for (auto i = 0, len = env->GetArrayLength(elements); i < len; ++i) {
        auto element = env->GetObjectArrayElement(elements, i);
        if (!element) continue;
        auto java_dex_file = env->GetObjectField(element, dex_file_field);
        if (!java_dex_file) continue;
        auto cookie = (jlongArray) env->GetObjectField(java_dex_file, cookie_field);
        if (!cookie) continue;
        auto dex_file_length = env->GetArrayLength(cookie);
        const auto *dex_files = reinterpret_cast<const DexFile **>(
                env->GetLongArrayElements(cookie, nullptr));
        LOGI("dex_file_length -> %d", dex_file_length);
        std::vector<const DexFile *> dex_images;
        if (use_memory_dex_file) {
            for (int j = 0; j < dex_file_length; ++j) {
                const auto *dex_file = dex_files[j];
                if (!CheckPoint((void *) dex_file) ||
                    !CheckPoint((void *) dex_file->begin_) ||
                    dex_file->size_ < sizeof(dex::Header)) {
                    LOGD("dex_file %d is invalid", j);
                    continue;
                }
                if (IsCompactDexFile(dex_file->begin_)) {
                    LOGD("skip compact dex");
                    dex_images.clear();
                    break;
                } else {
                    LOGD("push standard dex file %d, image size: %zu", j, dex_file->size_);
                    dex_images.emplace_back(dex_file);
                }
            }
        }
        if (dex_images.empty()) {
            auto file_name_obj = (jstring) env->GetObjectField(java_dex_file, file_name_field);
            if (!file_name_obj) continue;
            auto file_name = env->GetStringUTFChars(file_name_obj, nullptr);
            LOGD("contains compact dex, use path load: %s", file_name);
            dexkit->AddPath(file_name);
        } else {
            std::vector<std::unique_ptr<dexkit::MemMap>> images;
            for (auto &image: dex_images) {
                auto mmap = dexkit::MemMap(image->size_);
                memcpy(mmap.addr(), image->begin_, image->size_);
                images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
            }
            dexkit->AddImages(std::move(images));
        }
    }
    return (jlong) dexkit;
}

DEXKIT_JNI void
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

DEXKIT_JNI void
Java_io_luckypray_dexkit_DexKitBridge_nativeExportDexFile(JNIEnv *env, jclass clazz,
                                                          jlong native_ptr, jstring out_dir) {
    ExportDexFile(env, native_ptr, out_dir);
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
                                                             jboolean unique_result,
                                                             jintArray dex_priority) {
    return FindMethodCaller(env, native_ptr, method_descriptor, method_declare_class,
                            method_declare_name, method_return_type, method_param_types,
                            caller_method_declare_class, caller_method_declare_name,
                            caller_method_return_type, caller_method_param_types, unique_result,
                            dex_priority);
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
                                                               jboolean unique_result,
                                                               jintArray dex_priority) {
    return FindMethodInvoking(env, native_ptr, method_descriptor, method_declare_class,
                              method_declare_name, method_return_type, method_param_types,
                              be_called_method_declare_class, be_called_method_declare_name,
                              be_called_method_return_type, be_called_method_param_types,
                              unique_result, dex_priority);
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
                                                                 jboolean unique_result,
                                                                 jintArray dex_priority) {
    return FindMethodUsingField(env, native_ptr, field_descriptor, field_declare_class, field_name,
                                field_type, used_flags, caller_method_declare_class,
                                caller_method_name, caller_method_return_type,
                                caller_method_param_types, unique_result, dex_priority);
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
                                                                  jboolean unique_result,
                                                                  jintArray dex_priority) {
    return FindMethodUsingString(env, native_ptr, used_string, advanced_match, method_declare_class,
                                 method_name, method_return_type, method_param_types, unique_result,
                                 dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindClassUsingAnnotation(JNIEnv *env, jclass clazz,
                                                                     jlong native_ptr,
                                                                     jstring annotation_class,
                                                                     jstring annotation_using_string,
                                                                     jintArray dex_priority) {
    return FindClassUsingAnnotation(env, native_ptr, annotation_class, annotation_using_string,
                                    dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindFieldUsingAnnotation(JNIEnv *env, jclass clazz,
                                                                     jlong native_ptr,
                                                                     jstring annotation_class,
                                                                     jstring annotation_using_string,
                                                                     jstring field_declare_class,
                                                                     jstring field_name,
                                                                     jstring field_type,
                                                                     jintArray dex_priority) {
    return FindFieldUsingAnnotation(env, native_ptr, annotation_class, annotation_using_string,
                                    field_declare_class, field_name, field_type, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingAnnotation(JNIEnv *env, jclass clazz,
                                                                      jlong native_ptr,
                                                                      jstring annotation_class,
                                                                      jstring annotation_using_string,
                                                                      jstring method_declare_class,
                                                                      jstring method_name,
                                                                      jstring method_return_type,
                                                                      jobjectArray method_param_types,
                                                                      jintArray dex_priority) {
    return FindMethodUsingAnnotation(env, native_ptr, annotation_class, annotation_using_string,
                                     method_declare_class, method_name, method_return_type,
                                     method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethod(JNIEnv *env, jclass clazz,
                                                       jlong native_ptr,
                                                       jstring method_descriptor,
                                                       jstring method_declare_class,
                                                       jstring method_name,
                                                       jstring method_return_type,
                                                       jobjectArray method_param_types,
                                                       jintArray dex_priority) {
    return FindMethod(env, native_ptr, method_descriptor, method_declare_class,
                      method_name, method_return_type, method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindSubClasses(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr,
                                                           jstring parent_class,
                                                           jintArray dex_priority) {
    return FindSubClasses(env, native_ptr, parent_class, dex_priority);
}

DEXKIT_JNI jobjectArray
Java_io_luckypray_dexkit_DexKitBridge_nativeFindMethodUsingOpPrefixSeq(JNIEnv *env, jclass clazz,
                                                                       jlong native_ptr,
                                                                       jintArray op_prefix_seq,
                                                                       jstring method_declare_class,
                                                                       jstring method_name,
                                                                       jstring method_return_type,
                                                                       jobjectArray method_param_types,
                                                                       jintArray dex_priority) {
    return FindMethodUsingOpPrefixSeq(env, native_ptr, op_prefix_seq, method_declare_class,
                                      method_name,
                                      method_return_type, method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
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

DEXKIT_JNI jobject
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
