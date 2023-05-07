#include <jni.h>
#include "dex_kit.h"

std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, const jobject jMap);

jobject CMap2JMap(JNIEnv *env, const std::map<std::string, std::vector<std::string>> &map);

jobjectArray StrVec2JStrArr(JNIEnv *env, const std::vector<std::string> &vector);

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, const jobjectArray jStrArr);

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, const jintArray jIntArr);

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, const jintArray jIntArr);

jintArray U8Vec2JIntArr(JNIEnv *env, const std::vector<uint8_t> &vector);

jobject EmptyJMap(JNIEnv *env);

void SetThreadNum(JNIEnv *env, jlong dexKitPtr, jint threadNum) {
    if (!dexKitPtr) {
        return;
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    dexKit->SetThreadNum(threadNum);
}

jint GetDexNum(JNIEnv *env, jlong dexKitPtr) {
    if (!dexKitPtr) {
        return 0;
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    return dexKit->GetDexNum();
}

void ReleaseDexKitInstance(JNIEnv *env, jlong dexKit) {
    if (!dexKit) {
        return;
    }
    delete reinterpret_cast<dexkit::DexKit *>(dexKit);
}

void ExportDexFile(JNIEnv *env, jlong dexKitPtr, jstring jOutDir) {
    if (!dexKitPtr) {
        return;
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    const char *outDir = env->GetStringUTFChars(jOutDir, nullptr);
    std::string outDirStr(outDir);
    dexKit->ExportDexFile(outDirStr);
    env->ReleaseStringUTFChars(jOutDir, outDir);
}

jobject BatchFindClassesUsingStrings(JNIEnv *env,
                                     jlong dexKitPtr,
                                     jobject jMap,
                                     jint match_type,
                                     jstring find_package,
                                     jintArray dex_priority) {
    if (!dexKitPtr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto map = JMap2CMap(env, jMap);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->BatchFindClassesUsingStrings(map,
                                                    match_type,
                                                    findPackage,
                                                    dexPriority);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return CMap2JMap(env, res);
}

jobject BatchFindMethodsUsingStrings(JNIEnv *env,
                                     jlong dexKitPtr,
                                     jobject jMap,
                                     jint match_type,
                                     jstring find_package,
                                     jintArray dex_priority) {
    if (!dexKitPtr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto map = JMap2CMap(env, jMap);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->BatchFindMethodsUsingStrings(map,
                                                    match_type,
                                                    findPackage,
                                                    dexPriority);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return CMap2JMap(env, res);
}

jobject FindMethodCaller(JNIEnv *env,
                         jlong dexKitPtr,
                         jstring method_descriptor,
                         jstring method_declare_class,
                         jstring method_name,
                         jstring method_return_type,
                         jobjectArray method_param_types,
                         jstring caller_method_descriptor,
                         jstring caller_method_declare_class,
                         jstring caller_method_name,
                         jstring caller_method_return_type,
                         jobjectArray caller_method_param_types,
                         jboolean unique_result,
                         jstring source_file,
                         jstring find_package,
                         jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto callerMethodDescriptor = env->GetStringUTFChars(caller_method_descriptor, nullptr);
    auto callerMethodClass = env->GetStringUTFChars(caller_method_declare_class, nullptr);
    auto callerMethodName = env->GetStringUTFChars(caller_method_name, nullptr);
    auto callerMethodReturnType = env->GetStringUTFChars(caller_method_return_type, nullptr);
    auto callerParamTypes = dexkit::null_param;
    if (caller_method_param_types != NULL) {
        callerParamTypes = JStrArr2StrVec(env, caller_method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodCaller(methodDescriptor,
                                        methodDeclareClass,
                                        methodName,
                                        methodReturnType,
                                        ParamTypes,
                                        callerMethodDescriptor,
                                        callerMethodClass,
                                        callerMethodName,
                                        callerMethodReturnType,
                                        callerParamTypes,
                                        unique_result,
                                        sourceFile,
                                        findPackage,
                                        dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(caller_method_descriptor, callerMethodDescriptor);
    env->ReleaseStringUTFChars(caller_method_declare_class, callerMethodClass);
    env->ReleaseStringUTFChars(caller_method_name, callerMethodName);
    env->ReleaseStringUTFChars(caller_method_return_type, callerMethodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return CMap2JMap(env, res);
}

jobject FindMethodInvoking(JNIEnv *env,
                           jlong dexKitPtr,
                           jstring method_descriptor,
                           jstring method_declare_class,
                           jstring method_name,
                           jstring method_return_type,
                           jobjectArray method_param_types,
                           jstring be_called_method_descriptor,
                           jstring be_called_method_declare_class,
                           jstring be_called_method_name,
                           jstring be_called_method_return_type,
                           jobjectArray be_called_method_param_types,
                           jboolean unique_result,
                           jstring source_file,
                           jstring find_package,
                           jintArray dex_priority) {
    if (!dexKitPtr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto beCalledMethodDescriptor = env->GetStringUTFChars(be_called_method_descriptor, nullptr);
    auto beCalledMethodDeclareClass = env->GetStringUTFChars(be_called_method_declare_class,
                                                             nullptr);
    auto beCalledMethodDeclareName = env->GetStringUTFChars(be_called_method_name, nullptr);
    auto beCalledMethodReturnType = env->GetStringUTFChars(be_called_method_return_type, nullptr);
    auto beCalledMethodParamTypes = dexkit::null_param;
    if (be_called_method_param_types != NULL) {
        beCalledMethodParamTypes = JStrArr2StrVec(env, be_called_method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodInvoking(methodDescriptor,
                                          methodDeclareClass,
                                          methodName,
                                          methodReturnType,
                                          ParamTypes,
                                          beCalledMethodDescriptor,
                                          beCalledMethodDeclareClass,
                                          beCalledMethodDeclareName,
                                          beCalledMethodReturnType,
                                          beCalledMethodParamTypes,
                                          unique_result,
                                          sourceFile,
                                          findPackage,
                                          dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(be_called_method_descriptor, beCalledMethodDescriptor);
    env->ReleaseStringUTFChars(be_called_method_declare_class, beCalledMethodDeclareClass);
    env->ReleaseStringUTFChars(be_called_method_name, beCalledMethodDeclareName);
    env->ReleaseStringUTFChars(be_called_method_return_type, beCalledMethodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return CMap2JMap(env, res);
}

jobject FindMethodUsingField(JNIEnv *env,
                             jlong dexKitPtr,
                             jstring field_descriptor,
                             jstring field_declare_class,
                             jstring field_name,
                             jstring field_type,
                             jint used_flags,
                             jstring caller_method_descriptor,
                             jstring caller_method_declare_class,
                             jstring caller_method_name,
                             jstring caller_method_return_type,
                             jobjectArray caller_method_param_types,
                             jboolean unique_result,
                             jstring source_file,
                             jstring find_package,
                             jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto fieldDescriptor = env->GetStringUTFChars(field_descriptor, nullptr);
    auto fieldDeclareClass = env->GetStringUTFChars(field_declare_class, nullptr);
    auto fieldName = env->GetStringUTFChars(field_name, nullptr);
    auto fieldType = env->GetStringUTFChars(field_type, nullptr);
    if (used_flags == 0) {
        used_flags = dexkit::fGetting | dexkit::fSetting;
    }
    auto callerMethodDescriptor = env->GetStringUTFChars(caller_method_descriptor, nullptr);
    auto callerMethodClass = env->GetStringUTFChars(caller_method_declare_class, nullptr);
    auto callerMethodName = env->GetStringUTFChars(caller_method_name, nullptr);
    auto callerMethodReturnType = env->GetStringUTFChars(caller_method_return_type, nullptr);
    auto callerParamTypes = dexkit::null_param;
    if (caller_method_param_types != NULL) {
        callerParamTypes = JStrArr2StrVec(env, caller_method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodUsingField(fieldDescriptor,
                                            fieldDeclareClass,
                                            fieldName,
                                            fieldType,
                                            used_flags,
                                            callerMethodDescriptor,
                                            callerMethodClass,
                                            callerMethodName,
                                            callerMethodReturnType,
                                            callerParamTypes,
                                            unique_result,
                                            sourceFile,
                                            findPackage,
                                            dexPriority);
    env->ReleaseStringUTFChars(field_descriptor, fieldDescriptor);
    env->ReleaseStringUTFChars(field_declare_class, fieldDeclareClass);
    env->ReleaseStringUTFChars(field_name, fieldName);
    env->ReleaseStringUTFChars(field_type, fieldType);
    env->ReleaseStringUTFChars(caller_method_descriptor, callerMethodDescriptor);
    env->ReleaseStringUTFChars(caller_method_declare_class, callerMethodClass);
    env->ReleaseStringUTFChars(caller_method_name, callerMethodName);
    env->ReleaseStringUTFChars(caller_method_return_type, callerMethodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return CMap2JMap(env, res);
}

jobjectArray FindMethodUsingString(JNIEnv *env,
                                   jlong dexKitPtr,
                                   jstring using_string,
                                   jint match_type,
                                   jstring method_declare_class,
                                   jstring method_name,
                                   jstring method_return_type,
                                   jobjectArray method_param_types,
                                   jboolean unique_result,
                                   jstring source_file,
                                   jstring find_package,
                                   jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto usedUtf8String = env->GetStringUTFChars(using_string, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodUsingString(usedUtf8String,
                                             match_type,
                                             methodDeclareClass,
                                             methodName,
                                             methodReturnType,
                                             ParamTypes,
                                             unique_result,
                                             sourceFile,
                                             findPackage,
                                             dexPriority);
    env->ReleaseStringUTFChars(using_string, usedUtf8String);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsingNumber(JNIEnv *env,
                                   jlong dexKitPtr,
                                   jlong using_number,
                                   jstring method_declare_class,
                                   jstring method_name,
                                   jstring method_return_type,
                                   jobjectArray method_param_types,
                                   jboolean unique_result,
                                   jstring source_file,
                                   jstring find_package) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    auto res = dexKit->FindMethodUsingNumber(using_number,
                                             methodDeclareClass,
                                             methodName,
                                             methodReturnType,
                                             ParamTypes,
                                             unique_result,
                                             sourceFile,
                                             findPackage);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindClassUsingAnnotation(JNIEnv *env,
                                      jlong dexKitPtr,
                                      jstring annotation_class,
                                      jstring annotation_using_string,
                                      jint match_type,
                                      jstring source_file,
                                      jstring find_package,
                                      jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto annotationClass = env->GetStringUTFChars(annotation_class, nullptr);
    auto annotationUsingString = env->GetStringUTFChars(annotation_using_string, nullptr);
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindClassUsingAnnotation(annotationClass,
                                                annotationUsingString,
                                                match_type,
                                                sourceFile,
                                                findPackage,
                                                dexPriority);
    env->ReleaseStringUTFChars(annotation_class, annotationClass);
    env->ReleaseStringUTFChars(annotation_using_string, annotationUsingString);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindFieldUsingAnnotation(JNIEnv *env,
                                      jlong dexKitPtr,
                                      jstring annotation_class,
                                      jstring annotation_using_string,
                                      jint match_type,
                                      jstring field_declare_class,
                                      jstring field_name,
                                      jstring field_type,
                                      jstring source_file,
                                      jstring find_package,
                                      jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto annotationClass = env->GetStringUTFChars(annotation_class, nullptr);
    auto annotationUsingString = env->GetStringUTFChars(annotation_using_string, nullptr);
    auto fieldDeclareClass = env->GetStringUTFChars(field_declare_class, nullptr);
    auto fieldName = env->GetStringUTFChars(field_name, nullptr);
    auto fieldType = env->GetStringUTFChars(field_type, nullptr);
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindFieldUsingAnnotation(annotationClass,
                                                annotationUsingString,
                                                match_type,
                                                fieldDeclareClass,
                                                fieldName,
                                                fieldType,
                                                sourceFile,
                                                findPackage,
                                                dexPriority);
    env->ReleaseStringUTFChars(annotation_class, annotationClass);
    env->ReleaseStringUTFChars(annotation_using_string, annotationUsingString);
    env->ReleaseStringUTFChars(field_declare_class, fieldDeclareClass);
    env->ReleaseStringUTFChars(field_name, fieldName);
    env->ReleaseStringUTFChars(field_type, fieldType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsingAnnotation(JNIEnv *env,
                                       jlong dexKitPtr,
                                       jstring annotation_class,
                                       jstring annotation_using_string,
                                       jint match_type,
                                       jstring method_declare_class,
                                       jstring method_name,
                                       jstring method_return_type,
                                       jobjectArray method_param_types,
                                       jstring source_file,
                                       jstring find_package,
                                       jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto annotationClass = env->GetStringUTFChars(annotation_class, nullptr);
    auto annotationUsingString = env->GetStringUTFChars(annotation_using_string, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodUsingAnnotation(annotationClass,
                                                 annotationUsingString,
                                                 match_type,
                                                 methodDeclareClass,
                                                 methodName,
                                                 methodReturnType,
                                                 ParamTypes,
                                                 sourceFile,
                                                 findPackage,
                                                 dexPriority);
    env->ReleaseStringUTFChars(annotation_class, annotationClass);
    env->ReleaseStringUTFChars(annotation_using_string, annotationUsingString);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethod(JNIEnv *env,
                        jlong dexKitPtr,
                        jstring method_descriptor,
                        jstring method_declare_class,
                        jstring method_name,
                        jstring method_return_type,
                        jobjectArray method_param_types,
                        jstring source_file,
                        jstring find_package,
                        jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethod(methodDescriptor,
                                  methodDeclareClass,
                                  methodName,
                                  methodReturnType,
                                  ParamTypes,
                                  sourceFile,
                                  findPackage,
                                  dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindClass(JNIEnv *env,
                       jlong native_ptr,
                       jstring source_file,
                       jstring find_path,
                       jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(native_ptr);
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPath = env->GetStringUTFChars(find_path, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindClass(sourceFile, findPath, dexPriority);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_path, findPath);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindSubClasses(JNIEnv *env,
                            jlong dexKitPtr,
                            jstring parent_class,
                            jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto parentClass = env->GetStringUTFChars(parent_class, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindSubClasses(parentClass, dexPriority);
    env->ReleaseStringUTFChars(parent_class, parentClass);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsingOpPrefixSeq(JNIEnv *env,
                                        jlong dexKitPtr,
                                        jintArray op_prefix_seq,
                                        jstring method_declare_class,
                                        jstring method_name,
                                        jstring method_return_type,
                                        jobjectArray method_param_types,
                                        jstring source_file,
                                        jstring find_package,
                                        jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto opPrefixSeq = JIntArr2u8Vec(env, op_prefix_seq);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto ParamTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        ParamTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodUsingOpCodeSeq(opPrefixSeq,
                                                methodDeclareClass,
                                                methodName,
                                                methodReturnType,
                                                ParamTypes,
                                                sourceFile,
                                                findPackage,
                                                dexPriority);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobjectArray FindMethodUsingOpCodeSeq(JNIEnv *env,
                                      jlong dexKitPtr,
                                      jintArray op_code_seq,
                                      jstring method_declare_class,
                                      jstring method_name,
                                      jstring method_return_type,
                                      jobjectArray method_param_types,
                                      jstring source_file,
                                      jstring find_package,
                                      jintArray dex_priority) {
    if (!dexKitPtr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto opCodeSeq = JIntArr2u8Vec(env, op_code_seq);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto paramTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        paramTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->FindMethodUsingOpCodeSeq(opCodeSeq,
                                                methodDeclareClass,
                                                methodName,
                                                methodReturnType,
                                                paramTypes,
                                                sourceFile,
                                                findPackage,
                                                dexPriority);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);
    return StrVec2JStrArr(env, res);
}

jobject GetMethodOpCodeSeq(JNIEnv *env,
                           jlong dexKitPtr,
                           jstring method_descriptor,
                           jstring method_declare_class,
                           jstring method_name,
                           jstring method_return_type,
                           jobjectArray method_param_types,
                           jstring source_file,
                           jstring find_package,
                           jintArray dex_priority) {
    if (!dexKitPtr) {
        return EmptyJMap(env);
    }
    auto dexKit = reinterpret_cast<dexkit::DexKit *>(dexKitPtr);
    auto methodDescriptor = env->GetStringUTFChars(method_descriptor, nullptr);
    auto methodDeclareClass = env->GetStringUTFChars(method_declare_class, nullptr);
    auto methodName = env->GetStringUTFChars(method_name, nullptr);
    auto methodReturnType = env->GetStringUTFChars(method_return_type, nullptr);
    auto paramTypes = dexkit::null_param;
    if (method_param_types != NULL) {
        paramTypes = JStrArr2StrVec(env, method_param_types);
    }
    auto sourceFile = env->GetStringUTFChars(source_file, nullptr);
    auto findPackage = env->GetStringUTFChars(find_package, nullptr);
    std::vector<size_t> dexPriority;
    if (dex_priority != NULL) {
        dexPriority = JIntArr2IntVec(env, dex_priority);
    }
    auto res = dexKit->GetMethodOpCodeSeq(methodDescriptor,
                                          methodDeclareClass,
                                          methodName,
                                          methodReturnType,
                                          paramTypes,
                                          sourceFile,
                                          findPackage,
                                          dexPriority);
    env->ReleaseStringUTFChars(method_descriptor, methodDescriptor);
    env->ReleaseStringUTFChars(method_declare_class, methodDeclareClass);
    env->ReleaseStringUTFChars(method_name, methodName);
    env->ReleaseStringUTFChars(method_return_type, methodReturnType);
    env->ReleaseStringUTFChars(source_file, sourceFile);
    env->ReleaseStringUTFChars(find_package, findPackage);

    jclass cMap = env->FindClass("java/util/HashMap");
    jmethodID mMapInit = env->GetMethodID(cMap, "<init>", "()V");
    jobject jMap = env->NewObject(cMap, mMapInit);
    jmethodID mPut = env->GetMethodID(cMap, "put",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    for (auto &item: res) {
        auto key = env->NewStringUTF(item.first.c_str());
        auto value = U8Vec2JIntArr(env, item.second);
        env->CallObjectMethod(jMap, mPut, key, value);
    }
    return jMap;
}


std::map<std::string, std::set<std::string>> JMap2CMap(JNIEnv *env, jobject jMap) {
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

        jclass cValueSet = env->GetObjectClass(valueSet);
        jmethodID mValueIterator = env->GetMethodID(cValueSet, "iterator",
                                                    "()Ljava/util/Iterator;");
        jobject setIterator = env->CallObjectMethod(valueSet, mValueIterator);

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

std::vector<std::string> JStrArr2StrVec(JNIEnv *env, const jobjectArray jStrArr) {
    std::vector<std::string> result;
    for (int i = 0; i < env->GetArrayLength(jStrArr); ++i) {
        auto jStr = (jstring) env->GetObjectArrayElement(jStrArr, i);
        const char *cStr = env->GetStringUTFChars(jStr, nullptr);
        result.emplace_back(cStr);
        env->ReleaseStringUTFChars(jStr, cStr);
    }
    return result;
}

std::vector<size_t> JIntArr2IntVec(JNIEnv *env, const jintArray jIntArr) {
    std::vector<size_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}

std::vector<uint8_t> JIntArr2u8Vec(JNIEnv *env, const jintArray jIntArr) {
    std::vector<uint8_t> result;
    jint *ptr = env->GetIntArrayElements(jIntArr, nullptr);
    for (int i = 0; i < env->GetArrayLength(jIntArr); ++i) {
        result.emplace_back(ptr[i]);
    }
    env->ReleaseIntArrayElements(jIntArr, ptr, 0);
    return result;
}

jintArray U8Vec2JIntArr(JNIEnv *env, const std::vector<uint8_t> &vector) {
    auto result = env->NewIntArray((int) vector.size());
    jint *ptr = env->GetIntArrayElements(result, nullptr);
    for (int i = 0; i < vector.size(); ++i) {
        ptr[i] = vector[i];
    }
    env->ReleaseIntArrayElements(result, ptr, 0);
    return result;
}

jobject EmptyJMap(JNIEnv *env) {
    jclass cMap = env->FindClass("java/util/HashMap");
    jmethodID mMapInit = env->GetMethodID(cMap, "<init>", "()V");
    return env->NewObject(cMap, mMapInit);
}
