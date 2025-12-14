#pragma once

#include <jni.h>

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv* env, jstring s) : env_(env), string_(s) {
        if (s == nullptr) {
            utf_chars_ = nullptr;
        } else {
            utf_chars_ = env->GetStringUTFChars(s, nullptr);
        }
    }

    ~ScopedUtfChars() {
        if (utf_chars_) {
            env_->ReleaseStringUTFChars(string_, utf_chars_);
        }
    }

    const char* c_str() const {
        return utf_chars_;
    }

    operator const char*() const {
        return utf_chars_;
    }

    ScopedUtfChars(const ScopedUtfChars&) = delete;
    ScopedUtfChars& operator=(const ScopedUtfChars&) = delete;

private:
    JNIEnv* env_;
    jstring string_;
    const char* utf_chars_;
};