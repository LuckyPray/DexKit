// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#include <jni.h>
#include "dexkit.h"
#include "jni_helper.h"

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

using dexkit::Error;

void throwException(JNIEnv *env, const char *msg) {
    static jclass clazz = static_cast<jclass>(env->NewGlobalRef(
            env->FindClass("java/lang/IllegalStateException")));
    env->ThrowNew(clazz, msg);
}

void throwException(JNIEnv *env, Error error) {
    throwException(env, dexkit::GetErrorMessage(error).data());
}

void checkAndSetFlatBufferResult(JNIEnv *env, std::unique_ptr<flatbuffers::FlatBufferBuilder> &ptr, jbyteArray &ret) {
    if (ptr == nullptr) {
        return;
    }
    auto buf_ptr = ptr->GetBufferPointer();
    auto buf_size = ptr->GetSize();
    ret = env->NewByteArray(buf_size);
    env->SetByteArrayRegion(ret, 0, buf_size, (jbyte *) buf_ptr);
    ptr->Release();
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
    if (write(fd, (void *) addr, 8) < 0) {
        return false;
    }
    close(fd);
    return true;
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
    dex_file_field = env->GetFieldID(element, "dexFile", "Ldalvik/system/DexFile;");
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
        std::vector<const void *> dex_images;
        bool has_compact = false;
        if (use_memory_dex_file) {
            for (int j = 1; j < dex_file_length; ++j) {
                const auto *dex_file = dex_files[j];
                if (!CheckPoint((void *) dex_file)
                    || !CheckPoint((void *) dex_file->begin_)) {
                    LOGD("dex_file %d is invalid", j);
                    continue;
                }
                // https://cs.android.com/android/_/android/platform/art/+/4b7aef13e87be3e35de747fb10845057f9ddb712
                // in a14-r29+ size_ is unused
                auto header = reinterpret_cast<const struct dex::Header *>(dex_file->begin_);
                if (dex_file->size_ && dex_file->size_ != header->file_size) {
                    // TODO dex verify
                    LOGD("dex_file %d is invalid", j);
                    continue;
                }
                if (IsCompactDexFile(dex_file->begin_)) {
                    LOGD("skip compact dex");
                    dex_images.clear();
                    has_compact = true;
                    break;
                } else {
                    LOGD("push standard dex file %d, image size: %zu", j, header->file_size);
                    dex_images.emplace_back(dex_file->begin_);
                }
            }
        }
        if (dex_images.empty()) {
            auto file_name_obj = (jstring) env->GetObjectField(java_dex_file, file_name_field);
            if (!file_name_obj) continue;
            auto file_name = ScopedUtfChars(env, file_name_obj);
            if (has_compact) {
                LOGD("contains compact dex, use path load: %s", file_name.c_str());
            } else {
                LOGD("images is empty, use path load: %s", file_name.c_str());
            }
            auto ret = dexkit->AddZipPath(file_name.c_str());
            if (ret != Error::SUCCESS) {
                throwException(env, ret);
                delete dexkit;
                return 0;
            }
        } else {
            std::vector<std::unique_ptr<dexkit::MemMap>> images;
            for (auto image: dex_images) {
                auto header = reinterpret_cast<const struct dex::Header *>(image);
                auto mmap = dexkit::MemMap(header->file_size);
                memcpy((void *) mmap.data(), image, header->file_size);
                images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
            }
            auto ret = dexkit->AddImage(std::move(images));
            if (ret != Error::SUCCESS) {
                throwException(env, ret);
                delete dexkit;
                return 0;
            }
        }
    }
    return (jlong) dexkit;
}
#endif

DEXKIT_JNI jlong
Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByBytesArray(JNIEnv *env,
                                                                    jclass clazz,
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
        memcpy((void *) mmap.data(), dex_byte_ptr, dex_byte_length);
        images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
        env->ReleaseByteArrayElements(dex_byte, dex_byte_ptr, 0);
    }
    auto ret = dexkit->AddImage(std::move(images));
    if (ret != Error::SUCCESS) {
        throwException(env, ret);
        delete dexkit;
        return 0;
    }
    return (jlong) dexkit;
}

DEXKIT_JNI jlong
Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKit(JNIEnv *env, jclass clazz,
                                                        jstring apk_path
) {
    if (!apk_path) {
        return 0;
    }
    auto cpath = ScopedUtfChars(env, apk_path);
    LOGI("apkPath -> %s", cpath.c_str());
    auto dexkit = new dexkit::DexKit();
    auto ret = dexkit->AddZipPath(cpath.c_str());
    if (ret != Error::SUCCESS) {
        throwException(env, ret);
        delete dexkit;
        return 0;
    }
    return (jlong) dexkit;
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeSetThreadNum(JNIEnv *env, jclass clazz,
                                                          jlong native_ptr, jint thread_num
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    dexkit->SetThreadNum(thread_num);
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeInitFullCache(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto ret = dexkit->InitFullCache();
    if (ret != Error::SUCCESS) {
        throwException(env, ret);
    }
}

DEXKIT_JNI jint
Java_org_luckypray_dexkit_DexKitBridge_nativeGetDexNum(JNIEnv *env, jclass clazz,
                                                       jlong native_ptr
) {
    if (!native_ptr) {
        return 0;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    return dexkit->GetDexNum();
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeRelease(JNIEnv *env, jclass clazz,
                                                     jlong native_ptr
) {
    if (!native_ptr) {
        return;
    }
    delete reinterpret_cast<dexkit::DexKit *>(native_ptr);
}

DEXKIT_JNI void
Java_org_luckypray_dexkit_DexKitBridge_nativeExportDexFile(JNIEnv *env,
                                                           jclass clazz,
                                                           jlong native_ptr,
                                                           jstring out_dir
) {
    if (!native_ptr) {
        return;
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto cdir = ScopedUtfChars(env, out_dir);
    auto ret = dexkit->ExportDexFile(cdir.c_str());
    if (ret != Error::SUCCESS) {
        throwException(env, ret);
    }
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindClassUsingStrings(JNIEnv *env,
                                                                        jclass clazz,
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindMethodUsingStrings(JNIEnv *env,
                                                                         jclass clazz,
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindClass(JNIEnv *env, jclass clazz,
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindMethod(JNIEnv *env, jclass clazz,
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeFindField(JNIEnv *env, jclass clazz,
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    env->ReleaseByteArrayElements(arr, bytes, 0);
    return ret;
}


DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassData(JNIEnv *env, jclass clazz,
                                                          jlong native_ptr,
                                                          jstring dex_descriptor) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto cdesc = ScopedUtfChars(env, dex_descriptor);
    auto result = dexkit->GetClassData(cdesc.c_str());
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodData(JNIEnv *env, jclass clazz,
                                                           jlong native_ptr,
                                                           jstring dex_descriptor) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto cdesc = ScopedUtfChars(env, dex_descriptor);
    auto result = dexkit->GetMethodData(cdesc.c_str());
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldData(JNIEnv *env, jclass clazz,
                                                          jlong native_ptr,
                                                          jstring dex_descriptor) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto cdesc = ScopedUtfChars(env, dex_descriptor);
    auto result = dexkit->GetFieldData(cdesc.c_str());
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jobjectArray ret = env->NewObjectArray(result->size(), env->FindClass("java/lang/String"),
                                           nullptr);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    env->SetIntArrayRegion(ret, 0, int_vector.size(), (const jint *) int_vector.data());
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    return ret;
}

DEXKIT_JNI jobjectArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingStrings(JNIEnv *env, jclass clazz,
                                                                   jlong native_ptr,
                                                                   jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetUsingStrings(encode_method_id);
    jobjectArray ret = env->NewObjectArray(result.size(), env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < result.size(); ++i) {
        env->SetObjectArrayElement(ret, i, env->NewStringUTF(result[i].data()));
    }
    return ret;
}

DEXKIT_JNI jbyteArray
Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingFields(JNIEnv *env, jclass clazz,
                                                                   jlong native_ptr,
                                                                   jlong encode_method_id) {
    if (!native_ptr) {
        return {};
    }
    auto dexkit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto result = dexkit->GetUsingFields(encode_method_id);
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
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
    jbyteArray ret = nullptr;
    checkAndSetFlatBufferResult(env, result, ret);
    return ret;
}

jboolean UnboxBoolean(JNIEnv *env, jobject booleanObj) {
    static jmethodID booleanValueMID = [env]() -> auto {
        jclass booleanClass = env->FindClass("java/lang/Boolean");
        auto mid = env->GetMethodID(booleanClass, "booleanValue", "()Z");
        env->DeleteLocalRef(booleanClass);
        return mid;
    }();
    return env->CallBooleanMethod(booleanObj, booleanValueMID);
}

DEXKIT_JNI jobject
Java_org_luckypray_dexkit_util_NativeReflect_getReflectedField(JNIEnv* env, jclass clazz,
                                                               jclass declaringClass,
                                                               jstring name,
                                                               jstring jniSig,
                                                               jobject booleanObj) {
    static jclass noSuchFieldErrorCls = [env]() -> auto {
        auto cls = env->FindClass("java/lang/NoSuchFieldError");
        auto ret = (jclass) env->NewGlobalRef(cls);
        env->DeleteLocalRef(cls);
        return ret;
    }();

    auto cname = ScopedUtfChars(env, name);
    auto csig = ScopedUtfChars(env, jniSig);
    std::optional<jboolean> isStatic;
    jfieldID fid = nullptr;

    if (booleanObj != nullptr) {
        isStatic = UnboxBoolean(env, booleanObj);
        LOGD("UnboxBoolean isStatic: %d, cname: %s, csig: %s\n", *isStatic, cname.c_str(), csig.c_str());
        fid = *isStatic
              ? env->GetStaticFieldID(declaringClass, cname, csig)
              : env->GetFieldID(declaringClass, cname, csig);
        if (fid == nullptr) {
            return nullptr;
        }
    }
    if (!isStatic.has_value()) {
        fid = env->GetFieldID(declaringClass, cname, csig);
        if (fid != nullptr) {
            isStatic = false;
        } else {
            jthrowable exception = env->ExceptionOccurred();
            if (!env->IsInstanceOf(exception, noSuchFieldErrorCls)) {
                env->DeleteLocalRef(exception);
                return nullptr;
            }
            env->ExceptionClear();
        }
    }
    if (!isStatic.has_value()) {
        fid = env->GetStaticFieldID(declaringClass, cname, csig);
        if (fid != nullptr) {
            isStatic = true;
        } else {
            jthrowable exception = env->ExceptionOccurred();
            if (!env->IsInstanceOf(exception, noSuchFieldErrorCls)) {
                env->DeleteLocalRef(exception);
                return nullptr;
            }
            env->ExceptionClear();
        }
    }
    if (fid == nullptr) {
        return nullptr;
    }
    return env->ToReflectedField(declaringClass, fid, *isStatic);
}

DEXKIT_JNI jobject
Java_org_luckypray_dexkit_util_NativeReflect_getReflectedMethod(JNIEnv* env, jclass clazz,
                                                                jclass declaringClass,
                                                                jstring name,
                                                                jstring jniSig,
                                                                jobject booleanObj) {
    static jclass noSuchMethodErrorCls = [env]() -> auto {
        auto cls = env->FindClass("java/lang/NoSuchMethodError");
        auto ret = (jclass) env->NewGlobalRef(cls);
        env->DeleteLocalRef(cls);
        return ret;
    }();

    auto cname = ScopedUtfChars(env, name);
    auto csig = ScopedUtfChars(env, jniSig);
    std::optional<jboolean> isStatic;
    jmethodID mid = nullptr;
    if (booleanObj != nullptr) {
        isStatic = UnboxBoolean(env, booleanObj);
        mid = *isStatic
              ? env->GetStaticMethodID(declaringClass, cname, csig)
              : env->GetMethodID(declaringClass, cname, csig);
        if (mid == nullptr) {
            env->ExceptionClear();
        }
    }
    if (!isStatic.has_value()) {
        mid = env->GetMethodID(declaringClass, cname, csig);
        if (mid != nullptr) {
            isStatic = false;
        } else {
            jthrowable exception = env->ExceptionOccurred();
            if (!env->IsInstanceOf(exception, noSuchMethodErrorCls)) {
                env->DeleteLocalRef(exception);
                return nullptr;
            }
            env->ExceptionClear();
        }
    }
    if (!isStatic.has_value()) {
        mid = env->GetStaticMethodID(declaringClass, cname, csig);
        if (mid != nullptr) {
            isStatic = true;
        } else {
            jthrowable exception = env->ExceptionOccurred();
            if (!env->IsInstanceOf(exception, noSuchMethodErrorCls)) {
                env->DeleteLocalRef(exception);
                return nullptr;
            }
            env->ExceptionClear();
        }
    }
    if (mid == nullptr) {
        return nullptr;
    }
    return env->ToReflectedMethod(declaringClass, mid, *isStatic);
}

}