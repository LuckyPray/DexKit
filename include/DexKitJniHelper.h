#include <jni.h>
#include "dex_kit.h"

std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, const jobject &jMap);

jobject CMap2JMap(JNIEnv *env, const std::map<std::string, std::vector<std::string>> &map);

jobjectArray StrVec2JStrArr(JNIEnv *env, const std::vector<std::string> &vector);

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, const jobjectArray &jStrArr);

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, const jintArray &jIntArr);

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, const jintArray &jIntArr);

jlong CreateDexKitInstance(JNIEnv *env, jstring apkPath) {
    auto path = env->GetStringUTFChars(apkPath, nullptr);
    auto dexKit = new dexkit::DexKit(path);
    return reinterpret_cast<jlong>(dexKit);
}

void ReleaseDexKitInstance(JNIEnv *env, jlong dexKit) {
    delete reinterpret_cast<dexkit::DexKit *>(dexKit);
}

jobject BatchFindClassesUsedStrings(JNIEnv *env,
                                    jlong dexKit,
                                    jobject jMap,
                                    jboolean advanced_match,
                                    jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto map = JMap2CMap(env, jMap);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->BatchFindClassesUsedStrings(map, advanced_match, dexPriority);
    return CMap2JMap(env, res);
}

jobject BatchFindMethodsUsedStrings(JNIEnv *env,
                                    jlong dexKit,
                                    jobject jMap,
                                    jboolean advanced_match,
                                    jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto map = JMap2CMap(env, jMap);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->BatchFindMethodsUsedStrings(map, advanced_match, dexPriority);
    return CMap2JMap(env, res);
}

jobjectArray FindMethodBeInvoked(JNIEnv *env,
                                 const jlong dexKit,
                                 const jstring &method_descriptor,
                                 const jstring &method_declare_class,
                                 const jstring &method_name,
                                 const jstring &method_return_type,
                                 const jobjectArray &method_param_types,
                                 const jstring &caller_method_declare_class,
                                 const jstring &caller_method_name,
                                 const jstring &caller_method_return_type,
                                 const jobjectArray &caller_method_param_types,
                                 const jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto callerMethodClass = env->GetStringUTFChars(caller_method_declare_class, nullptr);
    auto callerMethodName = env->GetStringUTFChars(caller_method_name, nullptr);
    auto callerMethodReturnType = env->GetStringUTFChars(caller_method_return_type, nullptr);
    auto callerParamTypes = dexkit::null_param;
    if (caller_method_param_types != NULL) {
        callerParamTypes = JStrArr2StrVec(env, caller_method_param_types);
    }
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->FindMethodBeInvoked(methodDescriptor,
                                              methodDeclareClass,
                                              methodName,
                                              methodReturnType,
                                              ParamTypes,
                                              callerMethodClass,
                                              callerMethodName,
                                              callerMethodReturnType,
                                              callerParamTypes,
                                              dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(caller_method_declare_class, callerMethodClass);
    env->ReleaseStringUTFChars(caller_method_name, callerMethodName);
    env->ReleaseStringUTFChars(caller_method_return_type, callerMethodReturnType);
    return StrVec2JStrArr(env, res);
}

jobject FindMethodInvoking(JNIEnv *env,
                           const jlong dexKit,
                           const jstring &method_descriptor,
                           const jstring &method_declare_class,
                           const jstring &method_name,
                           const jstring &method_return_type,
                           const jobjectArray &method_param_types,
                           const jstring &be_called_method_declare_class,
                           const jstring &be_called_method_name,
                           const jstring &be_called_method_return_type,
                           const jobjectArray &be_called_method_param_types,
                           const jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto beCalledMethodDeclareClass = env->GetStringUTFChars(be_called_method_declare_class,
                                                             nullptr);
    auto beCalledMethodDeclareName = env->GetStringUTFChars(be_called_method_name, nullptr);
    auto beCalledMethodReturnType = env->GetStringUTFChars(be_called_method_return_type, nullptr);
    auto beCalledMethodParamTypes = dexkit::null_param;
    if (be_called_method_param_types != NULL) {
        beCalledMethodParamTypes = JStrArr2StrVec(env, be_called_method_param_types);
    }
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindMethodInvoking(methodDescriptor,
                                             methodDeclareClass,
                                             methodName,
                                             methodReturnType,
                                             ParamTypes,
                                             beCalledMethodDeclareClass,
                                             beCalledMethodDeclareName,
                                             beCalledMethodReturnType,
                                             beCalledMethodParamTypes,
                                             dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(be_called_method_declare_class, beCalledMethodDeclareClass);
    env->ReleaseStringUTFChars(be_called_method_name, beCalledMethodDeclareName);
    env->ReleaseStringUTFChars(be_called_method_return_type, beCalledMethodReturnType);
    return CMap2JMap(env, res);
}

jobjectArray FindFieldBeUsed(JNIEnv *env,
                             jlong dexKit,
                             jstring &field_descriptor,
                             jstring &field_declare_class,
                             jstring &field_name,
                             jstring &field_type,
                             jint be_used_flags,
                             jstring &caller_method_declare_class,
                             jstring &caller_method_name,
                             jstring &caller_method_return_type,
                             jobjectArray &caller_method_param_types,
                             jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto fieldDescriptor = env->GetStringUTFChars(field_descriptor, nullptr);
    auto fieldDeclareClass = env->GetStringUTFChars(field_declare_class, nullptr);
    auto fieldName = env->GetStringUTFChars(field_name, nullptr);
    auto fieldType = env->GetStringUTFChars(field_type, nullptr);
    if (be_used_flags == 0) {
        be_used_flags = dexkit::fGetting | dexkit::fSetting;
    }
    auto callerMethodClass = env->GetStringUTFChars(caller_method_declare_class, nullptr);
    auto callerMethodName = env->GetStringUTFChars(caller_method_name, nullptr);
    auto callerMethodReturnType = env->GetStringUTFChars(caller_method_return_type, nullptr);
    auto callerParamTypes = dexkit::null_param;
    if (caller_method_param_types != NULL) {
        callerParamTypes = JStrArr2StrVec(env, caller_method_param_types);
    }
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindFieldBeUsed(fieldDescriptor,
                                          fieldDeclareClass,
                                          fieldName,
                                          fieldType,
                                          be_used_flags,
                                          callerMethodClass,
                                          callerMethodName,
                                          callerMethodReturnType,
                                          callerParamTypes,
                                          dexPriority);
    env->ReleaseStringUTFChars(field_descriptor, fieldDescriptor);
    env->ReleaseStringUTFChars(field_declare_class, fieldDeclareClass);
    env->ReleaseStringUTFChars(field_name, fieldName);
    env->ReleaseStringUTFChars(field_type, fieldType);
    env->ReleaseStringUTFChars(caller_method_declare_class, callerMethodClass);
    env->ReleaseStringUTFChars(caller_method_name, callerMethodName);
    env->ReleaseStringUTFChars(caller_method_return_type, callerMethodReturnType);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsedString(JNIEnv *env,
                                  jlong dexKit,
                                  jstring &used_utf8_string,
                                  jboolean advanced_match,
                                  jstring &method_declare_class,
                                  jstring &method_name,
                                  jstring &method_return_type,
                                  jobjectArray &method_param_types,
                                  jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto usedUtf8String = env->GetStringUTFChars(used_utf8_string, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindMethodUsedString(usedUtf8String,
                                               advanced_match,
                                               methodDeclareClass,
                                               methodName,
                                               methodReturnType,
                                               ParamTypes,
                                               dexPriority);
    env->ReleaseStringUTFChars(used_utf8_string, usedUtf8String);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethod(JNIEnv *env,
                        jlong dexKit,
                        jstring method_declare_class,
                        jstring method_name,
                        jstring method_return_type,
                        jobjectArray &method_param_types,
                        jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindMethod(methodDeclareClass,
                                     methodName,
                                     methodReturnType,
                                     ParamTypes,
                                     dexPriority);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindSubClasses(JNIEnv *env,
                            jlong dexKit,
                            jstring &parent_class,
                            jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto parentClass = env->GetStringUTFChars(parent_class, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindSubClasses(parentClass, dexPriority);
    env->ReleaseStringUTFChars(parent_class, parentClass);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodOpPrefixSeq(JNIEnv *env,
                                   jlong dexKit,
                                   jintArray &op_prefix_seq,
                                   jstring &method_declare_class,
                                   jstring &method_name,
                                   jstring &method_return_type,
                                   jobjectArray &method_param_types,
                                   jintArray &dex_priority) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);
    auto opPrefixSeq = JIntArr2u8Vec(env, op_prefix_seq);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKitPtr->FindMethodOpPrefixSeq(opPrefixSeq,
                                                methodDeclareClass,
                                                methodName,
                                                methodReturnType,
                                                ParamTypes,
                                                dexPriority);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    return StrVec2JStrArr(env, res);
}


std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, const jobject &jMap) {
    jclass cMap = env->GetObjectClass(jMap);
    jmethodID mKeySet = env->GetMethodID(cMap, "keySet", "()Ljava/util/Set;");
    jobject keySet = env->CallObjectMethod(jMap, mKeySet);

    jclass cSet = env->GetObjectClass(keySet);
    jmethodID mIterator = env->GetMethodID(cSet, "iterator", "()Ljava/util/Iterator;");
    jobject keySetIterator = env->CallObjectMethod(keySet, mIterator);

    jclass cIterator = env->GetObjectClass(keySetIterator);
    jmethodID mHasNext = env->GetMethodID(cIterator, "hasNext", "()Z");
    jmethodID mNext = env->GetMethodID(cIterator, "next", "()Ljava/lang/Object;");
    jmethodID mGet = env->GetMethodID(cMap, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

    std::map<std::string, std::set<std::string>> result;
    while (env->CallBooleanMethod(keySetIterator, mHasNext)) {
        auto key = (jstring) env->CallObjectMethod(keySetIterator, mNext);
        const char *keyStr = env->GetStringUTFChars(key, nullptr);
        jobject valueSet = env->CallObjectMethod(jMap, mGet, key);
        jobject setIterator = env->CallObjectMethod(valueSet, mIterator);
        jclass cSetIterator = env->GetObjectClass(setIterator);
        jmethodID mSetHasNext = env->GetMethodID(cSetIterator, "hasNext", "()Z");
        jmethodID mSetNext = env->GetMethodID(cSetIterator, "next", "()Ljava/lang/Object;");
        std::set<std::string> set;
        while (env->CallBooleanMethod(setIterator, mSetHasNext)) {
            auto value = (jstring) env->CallObjectMethod(setIterator, mSetNext);
            const char *valueStr = env->GetStringUTFChars(value, nullptr);
            set.insert(valueStr);
            env->ReleaseStringUTFChars(value, valueStr);
        }
        result.insert(std::make_pair(keyStr, set));
        env->ReleaseStringUTFChars(key, keyStr);
    }
    return result;
}

jobject CMap2JMap(JNIEnv *env, const std::map<std::string, std::vector<std::string>> &map) {
    jclass cMap = env->FindClass("java/util/HashMap");
    jmethodID mMapInit = env->GetMethodID(cMap, "<init>", "()V");
    jobject jMap = env->NewObject(cMap, mMapInit);
    jmethodID mPut = env->GetMethodID(cMap, "put",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    for (auto &item: map) {
        auto key = env->NewStringUTF(item.first.c_str());
        auto value = StrVec2JStrArr(env, item.second);
        env->CallObjectMethod(jMap, mPut, key, value);
    }
    return jMap;
}

jobjectArray StrVec2JStrArr(JNIEnv *env, const std::vector<std::string> &vector) {
    jclass cString = env->FindClass("java/lang/String");
    auto result = env->NewObjectArray((int) vector.size(), cString, nullptr);
    for (int i = 0; i < vector.size(); ++i) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(vector[i].c_str()));
    }
    return result;
}

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, const jobjectArray &jStrArr) {
    std::vector<std::string> result;
    for (int i = 0; i < env->GetArrayLength(jStrArr); ++i) {
        auto jStr = (jstring) env->GetObjectArrayElement(jStrArr, i);
        const char *cStr = env->GetStringUTFChars(jStr, nullptr);
        result.emplace_back(cStr);
        env->ReleaseStringUTFChars(jStr, cStr);
    }
    return result;
}

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, const jintArray &jIntArr) {
    std::vector<size_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, const jintArray &jIntArr) {
    std::vector<uint8_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}
