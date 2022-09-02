#pragma once

#include <stdlib.h>
#include <map>
#include <set>
#include <iostream>
#include "jmap.h"
#include "jni.h"
#include "pch.h"

using namespace std;

map<string, set<string>> j2c(jobject instance, JNIEnv* env) {
	map<string, set<string>> map = {};
	// 找需要的类
	jclass class_map = env->FindClass("java/util/HashMap");
	jclass class_set = env->FindClass("java/util/HashSet");
	// 找需要的方法id
	jmethodID method_id_get = env->GetMethodID(class_map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
	jmethodID method_id_keySet = env->GetMethodID(class_map, "keySet", "()Ljava/util/Set;");
	jmethodID method_id_toArray_set = env->GetMethodID(class_set, "toArray", "()[Ljava/lang/Object;");
	// 开始取值
	jobject keySet = env->CallObjectMethod(instance, method_id_keySet);// Map.keySet();
	jobjectArray keyArray = (jobjectArray)env->CallObjectMethod(keySet, method_id_toArray_set);// Set.toArray();
	jsize size_key = env->GetArrayLength(keyArray);// Array.length()
	for (int pos = 0; pos < size_key; pos++) {
		jstring key = (jstring)env->GetObjectArrayElement(keyArray, pos);// 取出key
		jobject value = env->CallObjectMethod(instance, method_id_get, key);// Map.get(key);
		jobjectArray valueArray = (jobjectArray)env->CallObjectMethod(value, method_id_toArray_set);// Set.toArray();
		jsize size_value = env->GetArrayLength(valueArray);
		const char* keyL = env->GetStringUTFChars(key, nullptr);
		set<string> values = {};
		for (size_t i = 0; i < size_value; i++)
		{
			jstring valueStr = (jstring)env->GetObjectArrayElement(valueArray , i);// 取出value
			const char* valueL = env->GetStringUTFChars(valueStr, nullptr);
			values.insert(valueL);
		}
		map[keyL] = values;
	}
	return map;
}

jobject c2j(map<string, set<string>> map, JNIEnv* env) {
	// 找需要的类
	jclass class_map = env->FindClass("java/util/HashMap");
	jclass class_set = env->FindClass("java/util/HashSet");
	// 找需要的方法id
	jmethodID method_id_put = env->GetMethodID(class_map, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	jmethodID method_id_add = env->GetMethodID(class_set, "add", "(Ljava/lang/Object;)Z");
	jmethodID method_id_new_set = env->GetMethodID(class_set, "<init>", "()V");
	jmethodID method_id_new_map = env->GetMethodID(class_map, "<init>", "()V");
	// 初始化map
	jobject valueMap = env->NewObject(class_map, method_id_new_map);
	for (auto iter = map.begin(); iter != map.end(); ++iter) {
		// 取值
		string keyS = iter->first;
		const char* keyL = keyS.c_str();
		jstring key = env->NewStringUTF(keyL);
		set<string> set = iter->second;
		// 初始化set
		jobject valueSet = env->NewObject(class_set, method_id_new_set);
		for (auto iterS = set.begin(); iterS != set.end(); ++iterS) {
			// 取值
			string valueS = iterS->c_str();
			const char* valueL = valueS.c_str();
			jstring value = env->NewStringUTF(valueL);
			// 赋值
			env->CallBooleanMethod(valueSet, method_id_add, value);
		}
		env->CallObjectMethod(valueMap, method_id_put, key, valueSet);
	}
	return valueMap;
}