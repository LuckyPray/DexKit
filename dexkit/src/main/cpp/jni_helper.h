#pragma once

#include <jni.h>

class ScopedLocalFrame {
public:
    ScopedLocalFrame(JNIEnv *env, jint capacity) : env_(env), valid_(env->PushLocalFrame(capacity) == JNI_OK) {}
    ~ScopedLocalFrame() { if (valid_) env_->PopLocalFrame(nullptr); }
    bool ok() const { return valid_; }
    ScopedLocalFrame(const ScopedLocalFrame &) = delete;
    ScopedLocalFrame &operator=(const ScopedLocalFrame &) = delete;

private:
    JNIEnv *env_;
    bool valid_;
};

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