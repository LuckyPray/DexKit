#include <jni.h>
#include "dexkit.h"

#define TAG "DexKit"
#define DEXKIT_JNI JNIEXPORT JNICALL

#ifdef NDEXKIT_LOG
#define LOGI(...)
#define LOGD(...)
#define LOGE(...)
#define LOGF(...)
#define LOGW(...)
#else
#ifdef __ANDROID__
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#else

#include <cstdio>

#define LOGI(__FORMAT__, ...) fprintf(stdout, "I/" TAG ": " __FORMAT__ "\n", ##__VA_ARGS__); fflush(stdout)
#define LOGD(__FORMAT__, ...) fprintf(stdout, "D/" TAG ": " __FORMAT__ "\n", ##__VA_ARGS__); fflush(stdout)
#define LOGE(__FORMAT__, ...) fprintf(stdout, "E/" TAG ": " __FORMAT__ "\n", ##__VA_ARGS__); fflush(stdout)
#define LOGF(__FORMAT__, ...) fprintf(stdout, "F/" TAG ": " __FORMAT__ "\n", ##__VA_ARGS__); fflush(stdout)
#define LOGW(__FORMAT__, ...) fprintf(stdout, "W/" TAG ": " __FORMAT__ "\n", ##__VA_ARGS__); fflush(stdout)
#endif
#endif

template<typename T>
const T *From(const void *buf) {
    return ::flatbuffers::GetRoot<T>(buf);
}

extern "C" {

#ifdef __ANDROID__
// android memory processing
#include <sys/eventfd.h>

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
    auto fd = eventfd(0, 0);
    if (fd < 0) {
        LOGE("eventfd failed: %s", strerror(errno));
        return false;
    }
    bool valid = true;
    if (write(fd, (void *) addr, 8) < 0) {
        valid = false;
    }
    close(fd);
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
Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByClassLoader(JNIEnv *env, jclass clazz,
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
            dexkit->AddZipPath(file_name);
        } else {
            std::vector<std::unique_ptr<dexkit::MemMap>> images;
            for (auto &image: dex_images) {
                auto mmap = dexkit::MemMap(image->size_);
                memcpy(mmap.addr(), image->begin_, image->size_);
                images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
            }
            dexkit->AddImage(std::move(images));
        }
    }
    dexkit->BuildCrossRef();
    return (jlong) dexkit;
}
#endif

DEXKIT_JNI jlong
Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByBytesArray___3_3B(JNIEnv *env, jclass clazz,
                                                                           jobjectArray dex_bytes_array) {
    if (!dex_bytes_array) {
        return 0;
    }
    auto dexkit = new dexkit::DexKit();
    std::vector<std::unique_ptr<dexkit::MemMap>> images;
    for (int32_t i = 0, len = env->GetArrayLength(dex_bytes_array); i < len; ++i) {
        auto dex_byte = (jbyteArray) env->GetObjectArrayElement(dex_bytes_array, i);
        if (!dex_byte) continue;
        auto dex_byte_length = env->GetArrayLength(dex_byte);
        auto *dex_byte_ptr = env->GetByteArrayElements(dex_byte, nullptr);
        if (!dex_byte_ptr) continue;
        auto mmap = dexkit::MemMap(dex_byte_length);
        memcpy(mmap.addr(), dex_byte_ptr, dex_byte_length);
        images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
        env->ReleaseByteArrayElements(dex_byte, dex_byte_ptr, 0);
    }
    dexkit->AddImage(std::move(images));
    dexkit->BuildCrossRef();
    return (jlong) dexkit;
}

DEXKIT_JNI jlong
Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKit(JNIEnv *env, jclass clazz,
                                                        jstring apk_path
) {
    if (!apk_path) {
        return 0;
    }
    const char *cStr = env->GetStringUTFChars(apk_path, nullptr);
    LOGI("apkPath -> %s", cStr);
    std::string filePathStr(cStr);
    auto dexkit = new dexkit::DexKit(filePathStr);
    dexkit->BuildCrossRef();
    env->ReleaseStringUTFChars(apk_path, cStr);
    return (jlong) dexkit;
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeSetThreadNum__JI(JNIEnv *env, jclass clazz,
                                                              jlong native_ptr, jint thread_num
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    dexkit->SetThreadNum(thread_num);
}

DEXKIT_JNI jint
Java_org_luckypray_dexkit_DexKitBridge_nativeGetDexNum__J(JNIEnv *env, jclass clazz,
                                                          jlong native_ptr
) {
    if (!native_ptr) {
        return 0;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    return dexkit->GetDexNum();
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeRelease__J(JNIEnv *env, jclass clazz,
                                                        jlong native_ptr
) {
    if (!native_ptr) {
        return;
    }
    delete reinterpret_cast<dexkit::DexKit *>(native_ptr);
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeExportDexFile__JLjava_lang_String_2(JNIEnv *env, jclass clazz,
                                                                                 jlong native_ptr, jstring out_dir
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    const char *outDir = env->GetStringUTFChars(out_dir, nullptr);
    std::string outDirStr(outDir);
    dexkit->ExportDexFile(outDirStr);
    env->ReleaseStringUTFChars(out_dir, outDir);
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindClassUsingStrings__J_3B(JNIEnv *env, jclass clazz,
                                                                              jlong native_ptr,
                                                                              jbyteArray arr
) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    jbyte *bytes = env->GetByteArrayElements(arr, nullptr);
    auto query = From<dexkit::schema::BatchFindClassUsingStrings>(bytes);
    auto result = dexkit->BatchFindClassUsingStrings(query);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindMethodUsingStrings__J_3B(JNIEnv *env, jclass clazz,
                                                                               jlong native_ptr,
                                                                               jbyteArray arr
) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    jbyte *bytes = env->GetByteArrayElements(arr, nullptr);
    auto query = From<dexkit::schema::BatchFindMethodUsingStrings>(bytes);
    auto result = dexkit->BatchFindMethodUsingStrings(query);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindClass__J_3B(JNIEnv *env, jclass clazz,
                                                             jlong native_ptr,
                                                             jbyteArray arr
) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    jbyte *bytes = env->GetByteArrayElements(arr, nullptr);
    auto query = From<dexkit::schema::FindClass>(bytes);
    auto result = dexkit->FindClass(query);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindMethod__J_3B(JNIEnv *env, jclass clazz,
                                                              jlong native_ptr,
                                                              jbyteArray arr
) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    jbyte *bytes = env->GetByteArrayElements(arr, nullptr);
    auto query = From<dexkit::schema::FindMethod>(bytes);
    auto result = dexkit->FindMethod(query);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindField__J_3B(JNIEnv *env, jclass clazz,
                                                             jlong native_ptr,
                                                             jbyteArray arr
) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    jbyte *bytes = env->GetByteArrayElements(arr, nullptr);
    auto query = From<dexkit::schema::FindField>(bytes);
    auto result = dexkit->FindField(query);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassByIds(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr,
                                                           jlongArray encode_id_array) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto ids_len = env->GetArrayLength(encode_id_array);
    auto *ids_ptr = env->GetLongArrayElements(encode_id_array, nullptr);
    std::vector<int64_t> ids_vec(ids_len);
    memcpy(ids_vec.data(), ids_ptr, ids_len * sizeof(int64_t));
    env->ReleaseLongArrayElements(encode_id_array, ids_ptr, 0);
    auto result = dexkit->GetClassByIds(ids_vec);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodByIds(JNIEnv *env, jclass clazz,
                                                            jlong native_ptr,
                                                            jlongArray encode_id_array) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto ids_len = env->GetArrayLength(encode_id_array);
    auto *ids_ptr = env->GetLongArrayElements(encode_id_array, nullptr);
    std::vector<int64_t> ids_vec(ids_len);
    memcpy(ids_vec.data(), ids_ptr, ids_len * sizeof(int64_t));
    env->ReleaseLongArrayElements(encode_id_array, ids_ptr, 0);
    auto result = dexkit->GetMethodByIds(ids_vec);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldByIds(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr,
                                                           jlongArray encode_id_array) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto ids_len = env->GetArrayLength(encode_id_array);
    auto *ids_ptr = env->GetLongArrayElements(encode_id_array, nullptr);
    std::vector<int64_t> ids_vec(ids_len);
    memcpy(ids_vec.data(), ids_ptr, ids_len * sizeof(int64_t));
    env->ReleaseLongArrayElements(encode_id_array, ids_ptr, 0);
    auto result = dexkit->GetFieldByIds(ids_vec);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassAnnotations(JNIEnv *env, jclass clazz,
                                                                 jlong native_ptr,
                                                                 jlong encode_class_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetClassAnnotations(encode_class_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldAnnotations(JNIEnv *env, jclass clazz,
                                                                 jlong native_ptr,
                                                                 jlong encode_field_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetFieldAnnotations(encode_field_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodAnnotations(JNIEnv *env, jclass clazz,
                                                                  jlong native_ptr,
                                                                  jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetMethodAnnotations(encode_method_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jobjectArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterNames(JNIEnv *env, jclass clazz,
                                                               jlong native_ptr,
                                                               jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetParameterNames(encode_method_id);
    if (!result.has_value()) {
        return nullptr;
    }
    jobjectArray ret = env->NewObjectArray(result->size(), env->FindClass("java/lang/String"),nullptr);
    for (int i = 0; i < result->size(); ++i) {
        auto value = result->at(i);
        if (!value.has_value()) {
            env->SetObjectArrayElement(ret, i, nullptr);
        } else {
            env->SetObjectArrayElement(ret, i, env->NewStringUTF(value->data()));
        }
    }
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterAnnotations(JNIEnv *env, jclass clazz,
                                                                     jlong native_ptr,
                                                                     jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetParameterAnnotations(encode_method_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (const jbyte *) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jintArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodOpCodes(JNIEnv *env, jclass clazz,
                                                              jlong native_ptr,
                                                              jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetMethodOpCodes(encode_method_id);
    auto int_vector = std::vector<int>(result.begin(), result.end());
    jintArray ret = env->NewIntArray(int_vector.size());
    env->SetIntArrayRegion(ret, 0, int_vector.size(), (const jint*) int_vector.data());
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetCallMethods(JNIEnv *env, jclass clazz,
                                                            jlong native_ptr,
                                                            jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetCallMethods(encode_method_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size,(const jbyte*) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetInvokeMethods(JNIEnv *env, jclass clazz,
                                                              jlong native_ptr,
                                                              jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetInvokeMethods(encode_method_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);;
    env->SetByteArrayRegion(ret, 0, buf_size, (const jbyte*) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFieldGetMethods(JNIEnv *env, jclass clazz,
                                                             jlong native_ptr,
                                                             jlong encode_field_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->FieldGetMethods(encode_field_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);;
    env->SetByteArrayRegion(ret, 0, buf_size, (const jbyte*) buf_ptr);
    result->Release();
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFieldPutMethods(JNIEnv *env, jclass clazz,
                                                             jlong native_ptr,
                                                             jlong encode_field_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->FieldPutMethods(encode_field_id);
    auto buf_ptr = result->GetBufferPointer();
    auto buf_size = result->GetSize();
    jbyteArray ret = env->NewByteArray(buf_size);;
    env->SetByteArrayRegion(ret, 0, buf_size, (const jbyte*) buf_ptr);
    result->Release();
    return ret;
}

}