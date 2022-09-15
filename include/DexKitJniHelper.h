#include <jni.h>
#include "dex_kit.h"

std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, jobject &jMap);

jobject CMap2JMap(JNIEnv *env, const std::map<std::string, std::vector<std::string>> &map);

jobjectArray StrVec2JStrArr(JNIEnv *env, const std::vector<std::string> &vector);

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, jobjectArray &jStrArr);

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, jintArray &jIntArr);

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, jintArray &jIntArr);

jlong CreateDexKitInstance(JNIEnv *env, jstring apkPath) {
    auto path = env->GetStringUTFChars(apkPath, nullptr);
    auto dexKit = new dexkit::DexKit(path);
    return reinterpret_cast<jlong>(dexKit);
}

void ReleaseDexKitInstance(JNIEnv *env, jlong dexKit) {
    delete reinterpret_cast<dexkit::DexKit *>(dexKit);
}

jobject LocationClasses(JNIEnv *env,
                        jlong dexKit,
                        jobject jMap,
                        jboolean advanced_match) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto map = JMap2CMap(env, jMap);
    auto res = dexKitPtr->LocationClasses(map, advanced_match);
    return CMap2JMap(env, res);
}

jobject LocationMethods(JNIEnv *env,
                        jlong dexKit,
                        jobject jMap,
                        jboolean advanced_match) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto map = JMap2CMap(env, jMap);
    auto res = dexKitPtr->LocationMethods(map, advanced_match);
    return CMap2JMap(env, res);
}

jobjectArray FindMethodInvoked(JNIEnv *env,
                               jlong dexKit,
                               jstring method_descriptor,
                               jstring class_decl_name,
                               jstring method_name,
                               jstring result_class_decl,
                               jobjectArray &param_class_decls,
                               jintArray &dex_priority,
                               jboolean match_any_param_if_param_vector_empty) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto classDeclName = env->GetStringUTFChars(class_decl_name, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto resultClassDecl = env->GetStringUTFChars(result_class_decl, nullptr);
    auto paramClassDecls = JStrArr2StrVec(env, param_class_decls);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->FindMethodInvoked(methodDescriptor,
                                            classDeclName,
                                            methodName,
                                            resultClassDecl,
                                            paramClassDecls,
                                            dexPriority,
                                            match_any_param_if_param_vector_empty);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(class_decl_name, classDeclName);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(result_class_decl, resultClassDecl);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsedString(JNIEnv *env,
                                  jlong dexKit,
                                  jstring &jstr,
                                  jstring &class_decl_name,
                                  jstring &method_name,
                                  jstring &result_class_decl,
                                  jobjectArray &param_class_decls,
                                  jintArray &dex_priority,
                                  jboolean match_any_param_if_param_vector_empty,
                                  jboolean advanced_match) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto str = env->GetStringUTFChars(jstr, nullptr);
    auto classDeclName = env->GetStringUTFChars(class_decl_name, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto resultClassDecl = env->GetStringUTFChars(result_class_decl, nullptr);
    auto paramClassDecls = JStrArr2StrVec(env, param_class_decls);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->FindMethodUsedString(str,
                                               classDeclName,
                                               methodName,
                                               resultClassDecl,
                                               paramClassDecls,
                                               dexPriority,
                                               match_any_param_if_param_vector_empty,
                                               advanced_match);
    env->ReleaseStringUTFChars(jstr, str);
    env->ReleaseStringUTFChars(class_decl_name, classDeclName);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(result_class_decl, resultClassDecl);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethod(JNIEnv *env,
                        jlong dexKit,
                        jstring class_decl_name,
                        jstring method_name,
                        jstring result_class_decl,
                        jobjectArray &param_class_decls,
                        jintArray &dex_priority,
                        jboolean match_any_param_if_param_vector_empty) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto classDeclName = env->GetStringUTFChars(class_decl_name, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto resultClassDecl = env->GetStringUTFChars(result_class_decl, nullptr);
    auto paramClassDecls = JStrArr2StrVec(env, param_class_decls);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto res = dexKitPtr->FindMethod(classDeclName,
                                     methodName,
                                     resultClassDecl,
                                     paramClassDecls,
                                     dexPriority,
                                     match_any_param_if_param_vector_empty);
    env->ReleaseStringUTFChars(class_decl_name, classDeclName);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(result_class_decl, resultClassDecl);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindSubClasses(JNIEnv *env,
                            jlong dexKit,
                            jstring class_decl_name) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto classDeclName = env->GetStringUTFChars(class_decl_name, nullptr);
    auto res = dexKitPtr->FindSubClasses(classDeclName);
    env->ReleaseStringUTFChars(class_decl_name, classDeclName);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodOpPrefixSeq(JNIEnv *env,
                                   jlong dexKit,
                                   jintArray &op_prefix_seq,
                                   jstring &class_decl_name,
                                   jstring &method_name,
                                   jstring &result_class_decl,
                                   jobjectArray &param_class_decls,
                                   jintArray &dex_priority,
                                   jboolean match_any_param_if_param_vector_empty) {
    auto dexKitPtr = reinterpret_cast<dexkit::DexKit *>(dexKit);;
    auto classDeclName = env->GetStringUTFChars(class_decl_name, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto resultClassDecl = env->GetStringUTFChars(result_class_decl, nullptr);
    auto paramClassDecls = JStrArr2StrVec(env, param_class_decls);
    auto dexPriority = JIntArr2IntVec(env, dex_priority);
    auto opPrefixSeq = JIntArr2u8Vec(env, op_prefix_seq);
    auto res = dexKitPtr->FindMethodOpPrefixSeq(opPrefixSeq,
                                                classDeclName,
                                                methodName,
                                                resultClassDecl,
                                                paramClassDecls,
                                                dexPriority,
                                                match_any_param_if_param_vector_empty);
    env->ReleaseStringUTFChars(class_decl_name, classDeclName);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(result_class_decl, resultClassDecl);
    return StrVec2JStrArr(env, res);
}


std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, jobject &jMap) {
    jclass cMap = env->GetObjectClass(jMap);
    jmethodID mKeySet = env->GetMethodID(cMap, "keySet", "()Ljava/util/Set;");
    jobject keySet = env->CallObjectMethod(jMap, mKeySet);

    jclass cSet = env->GetObjectClass(keySet);
    jmethodID mIterator = env->GetMethodID(cSet, "iterator", "()Ljava/util/Iterator;");
    jobject keySetIterator = env->CallObjectMethod(keySet, mIterator);

    jclass cIterator = env->GetObjectClass(keySetIterator);
    jmethodID mHasNext = env->GetMethodID(cIterator, "hasNext", "()Z");
    jmethodID mNext = env->GetMethodID(cIterator, "next", "()Ljava/lang/Object;");
    jmethodID mGet = env->GetMethodID(cMap, "get","(Ljava/lang/Object;)Ljava/lang/Object;");

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
    jmethodID mPut = env->GetMethodID(cMap, "put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
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

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, jobjectArray &jStrArr) {
    std::vector<std::string> result;
    for (int i = 0; i < env->GetArrayLength(jStrArr); ++i) {
        auto jStr = (jstring) env->GetObjectArrayElement(jStrArr, i);
        const char *cStr = env->GetStringUTFChars(jStr, nullptr);
        result.emplace_back(cStr);
        env->ReleaseStringUTFChars(jStr, cStr);
    }
    return result;
}

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, jintArray &jIntArr) {
    std::vector<size_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, jintArray &jIntArr) {
    std::vector<uint8_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}
