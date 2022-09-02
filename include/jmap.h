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
	cout << "Find Class: HashMap" << endl;
	jclass class_map = env->FindClass("java/util/HashMap");
	cout << "Find Class: HashSet" << endl;
	jclass class_set = env->FindClass("java/util/HashSet");
	// 找需要的方法id
	cout << "Find Method: Map->get" << endl;
	jmethodID method_id_get = env->GetMethodID(class_map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
	cout << "Find Method: Map->keySet" << endl;
	jmethodID method_id_keySet = env->GetMethodID(class_map, "keySet", "()Ljava/util/Set;");
	cout << "Find Method: Set->toArray" << endl;
	jmethodID method_id_toArray_set = env->GetMethodID(class_set, "toArray", "()[Ljava/lang/Object;");
	// 开始取值
	cout << "Get Value: Map->keySet" << endl;
	jobject keySet = env->CallObjectMethod(instance, method_id_keySet);// Map.keySet();
	cout << "Get Value: Set->toArray" << endl;
	jobjectArray keyArray = (jobjectArray)env->CallObjectMethod(keySet, method_id_toArray_set);// Set.toArray();
	cout << "Get Value: Set->length" << endl;
	jsize size_key = env->GetArrayLength(keyArray);// Array.length()
	for (int pos = 0; pos < size_key; pos++) {
		jstring key = (jstring)env->GetObjectArrayElement(keyArray, pos);// 取出key
		jobject value = env->CallObjectMethod(instance, method_id_get, key);// Map.get(key);
		jobjectArray valueArray = (jobjectArray)env->CallObjectMethod(value, method_id_toArray_set);// Set.toArray();
		jsize size_value = env->GetArrayLength(valueArray);
		const char* keyL = env->GetStringUTFChars(key, nullptr);
		cout << "Get Key Value: Array->Value@" << pos << ":" << keyL << endl;
		set<string> values = {};
		for (size_t i = 0; i < size_value; i++)
		{
			jstring valueStr = (jstring)env->GetObjectArrayElement(valueArray , i);// 取出value
			const char* valueL = env->GetStringUTFChars(valueStr, nullptr);
			cout << "Get Value Value: Array->Value@" << i << ":" << valueL << endl;
			values.insert(valueL);
		}
		map[keyL] = values;
	}
	cout << "j2c Success" << endl;
	return map;
}

jobject c2j(map<string, set<string>> map, JNIEnv* env) {
	// 找需要的类
	// 找需要的类
	cout << "Find Class: HashMap" << endl;
	jclass class_map = env->FindClass("java/util/HashMap");
	cout << "Find Class: HashSet" << endl;
	jclass class_set = env->FindClass("java/util/HashSet");
	// 找需要的方法id
	cout << "Find Method: HashMap->put" << endl;
	jmethodID method_id_put = env->GetMethodID(class_map, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	cout << "Find Method: HashSet->add" << endl;
	jmethodID method_id_add = env->GetMethodID(class_set, "add", "(Ljava/lang/Object;)Z");
	cout << "Find Method: HashSet->new" << endl;
	jmethodID method_id_new_set = env->GetMethodID(class_set, "<init>", "()V");
	cout << "Find Method: HashMap->new" << endl;
	jmethodID method_id_new_map = env->GetMethodID(class_map, "<init>", "()V");
	// 初始化map
	cout << "Init Object: HashMap" << endl;
	jobject valueMap = env->NewObject(class_map, method_id_new_map);
	for (auto iter = map.begin(); iter != map.end(); ++iter) {
		// 取值
		string keyS = iter->first;
		const char* keyL = keyS.c_str();
		jstring key = env->NewStringUTF(keyL);
		set<string> set = iter->second;
		// 初始化set
		cout << "Init Object: HashSet" << endl;
		jobject valueSet = env->NewObject(class_set, method_id_new_set);
		for (auto iterS = set.begin(); iterS != set.end(); ++iterS) {
			// 取值
			string valueS = iterS->c_str();
			const char* valueL = valueS.c_str();
			jstring value = env->NewStringUTF(valueL);
			cout << "Call Method: HashSet->add" << endl;
			// 赋值
			env->CallBooleanMethod(valueSet, method_id_add, value);
		}
		cout << "Call Method: HashMap->put" << endl;
		env->CallObjectMethod(valueMap, method_id_put, key, valueSet);
	}
	return valueMap;
}