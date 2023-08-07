#pragma once

#include <string_view>
#include <cstring>
#include <cstdint>
#include <functional>
#include "State.h"

namespace acdat {

template<typename V>
class Hit {
public:
    int begin;
    int end;
    V value;

    Hit(int begin, int end, V value) : begin(begin), end(end), value(value) {
    }
};

template<typename V>
class AhoCorasickDoubleArrayTrie {
public:
    std::vector<int> check;
    std::vector<int> base;
    std::vector<int> fail;
    std::vector<std::vector<int>> output;
    std::vector<std::vector<std::pair<V, bool>>> v;
    std::vector<int> l;
    int size = 0;

    std::vector<Hit<V>> ParseText(std::string_view text, bool ignoreCase = false);

    void ParseText(std::string_view text, std::function<void(int, int, V)> &callback, bool ignoreCase = false);

//    void ParseText(std::string_view text, std::function<bool(int, int, V)> callback, bool ignoreCase = false);

    bool Matches(std::string_view text, bool ignoreCase = false);

    Hit<V> FindFirst(std::string_view text, bool ignoreCase = false);

    void Save() {}

    void Load() {}

    std::vector<V> Get(const char *key);

    bool Set(const char *key, std::vector<V> value);

    std::vector<V> Get(int index) {
        std::vector<V> vec;
        for (auto &item: v[index]) {
            vec.push_back(item.first);
        }
        return vec;
    }

    int ExactMatchSearch(const char *key);

    int GetSize() {
        return v.size();
    }


private:


    /**
     * transmit State, supports failure function
     *
     * @param currentState
     * @param character
     * @return
     */
    int GetState(int currentState, uint8_t c);

    void StoreEmits(std::string_view, int position, int currentState, bool ignoreCase, std::vector<Hit<V>> &collectedEmits);

    int ExactMatchSearch(const char *key, int pos, int len, int nodePos);

    int GetMatched(int pos, int len, int result, const char *key, int b1);

protected:
    int Transition(int current, uint8_t c);

    int TransitionWithRoot(int nodePos, uint8_t c);
};

template<typename V>
std::vector<Hit<V>> AhoCorasickDoubleArrayTrie<V>::ParseText(std::string_view text, bool ignoreCase) {
    int position = 1;
    int currentState = 0;
    std::vector<Hit<V>> collectedEmits = std::vector<Hit<V>>();
    int text_len = (int) text.length();
    const char *p = text.data();
    while (text_len-- > 0) {
        currentState = GetState(currentState, *p++, ignoreCase);
        StoreEmits(text, position++, currentState, ignoreCase, collectedEmits);
    }

    return collectedEmits;
}

template<typename V>
void AhoCorasickDoubleArrayTrie<V>::ParseText(std::string_view text, std::function<void(int, int, V)> &callback, bool ignoreCase) {
    int position = 1;
    int currentState = 0;
    int text_len = (int) text.length();
    const char *p = text.data();
    while (text_len-- > 0) {
        currentState = GetState(currentState, *p++);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            for (int &hit: hitArray) {
                auto vec = v[hit];
                for (auto &[item, itemIgnore]: vec) {
                    if (itemIgnore || ignoreCase || item == std::string_view(text.substr(position - l[hit], l[hit]))) {
                        callback(position - l[hit], position, item);
                    }
                }
            }
        }
        ++position;
    }
}

//template<typename V>
//void AhoCorasickDoubleArrayTrie<V>::ParseText(std::string_view text, std::function<bool(int, int, V)> callback, bool ignoreCase) {
//    int position = 1;
//    int currentState = 0;
//    int text_len = (int) text.length();
//    const char *p = text.data();
//    while (text_len-- > 0) {
//        currentState = GetState(currentState, *p++, ignoreCase);
//        auto hitArray = output[currentState];
//        if (!hitArray.empty()) {
//            for (int &hit: hitArray) {
//                auto vec = v[hit];
//                for (auto &[item, itemIgnore]: vec) {
//                    if (itemIgnore || ignoreCase || item == std::string_view(text.substr(position - l[hit], l[hit]))) {
//                        bool proceed = callback(position - l[hit], position, item);
//                        if (!proceed) {
//                            return;
//                        }
//                    }
//                }
//            }
//        }
//        ++position;
//    }
//}

template<typename V>
bool AhoCorasickDoubleArrayTrie<V>::Matches(std::string_view text, bool ignoreCase) {
    int currentState = 0;
    int text_len = (int) text.length();
    const char *p = text.data();
    while (text_len-- > 0) {
        currentState = GetState(currentState, *p++, ignoreCase);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            return true;
        }
    }
    return false;
}

template<typename V>
Hit<V> AhoCorasickDoubleArrayTrie<V>::FindFirst(std::string_view text, bool ignoreCase) {
    int position = 1;
    int currentState = 0;
    int text_len = (int) text.length();
    const char *p = text.data();
    while (text_len-- > 0) {
    currentState = GetState(currentState, *p++, ignoreCase);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            int hit = hitArray[0];
            auto vec = v[hit];
            for (auto &[item, itemIgnore]: vec) {
                if (itemIgnore || ignoreCase || item == std::string_view(text.substr(position - l[hit], l[hit]))) {
                    return Hit<V>(position - l[hit], position, item);
                }
            }
        }
        ++position;
    }
    return nullptr;
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::GetState(int currentState, uint8_t ch) {
    int newCurrentState = TransitionWithRoot(currentState, ch);  // 先按success跳转
    while (newCurrentState == -1) // 跳转失败的话，按failure跳转
    {
        currentState = fail[currentState];
        newCurrentState = TransitionWithRoot(currentState, ch);
    }
    return newCurrentState;
}

template<typename V>
std::vector<V> AhoCorasickDoubleArrayTrie<V>::Get(const char *key) {
    int index = ExactMatchSearch(key);
    if (index >= 0) {
        std::vector<V> vec;
        for (auto &item: v[index]) {
            vec.push_back(item.first);
        }
        return vec;
    }
    return nullptr;
}

template<typename V>
bool AhoCorasickDoubleArrayTrie<V>::Set(const char *key, std::vector<V> value) {
    int index = ExactMatchSearch(key);
    if (index >= 0) {
        v[index] = value;
        return true;
    }
    return false;
}

template<typename V>
void AhoCorasickDoubleArrayTrie<V>::StoreEmits(std::string_view text, int position, int currentState, bool ignoreCase, std::vector<Hit<V>> &collectedEmits) {
    auto hitArray = output[currentState];
    if (!hitArray.empty()) {
        for (int &hit: hitArray) {
            auto vec = v[hit];
            for (auto &[item, itemIgnore]: vec) {
                if (itemIgnore || ignoreCase || item == std::string_view(text.substr(position - l[hit], l[hit]))) {
                    collectedEmits.push_back(Hit<V>(position - l[hit], position, item));
                }
            }
        }
    }
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::Transition(int current, uint8_t c) {
    c = c >= 'A' && c <= 'Z' ? c + 32 : c;
    int b = current;
    int p;

    p = b + c + 1;
    if (b == check[p]) {
        b = base[p];
    } else {
        return -1;
    }

    p = b;
    return p;
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::TransitionWithRoot(int nodePos, uint8_t c) {
    c = c >= 'A' && c <= 'Z' ? c + 32 : c;
    int b = base[nodePos];
    int p;

    p = b + c + 1;
    if (b != check[p]) {
        if (nodePos == 0) {
            return 0;
        }
        return -1;
    }

    return p;
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::ExactMatchSearch(const char *key) {
    return ExactMatchSearch(key, 0, 0, 0);
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::ExactMatchSearch(const char *key, int pos, int len, int nodePos) {
    if (len <= 0) {
        len = (int) strlen(reinterpret_cast<const char *>(key));
    }
    if (nodePos <= 0) {
        nodePos = 0;
    }

    int result = -1;

    return GetMatched(pos, len, result, key, base[nodePos]);
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::GetMatched(int pos, int len, int result, const char *key, int b1) {
    int b = b1;
    int p;

    for (int i = pos; i < len; i++) {
        p = b + (int) (key[i]) + 1;
        if (b == check[p]) {
            b = base[p];
        } else {
            return result;
        }
    }

    p = b; // transition through '\0' to check if it's the end of a word
    int n = base[p];
    if (b == check[p]) // yes, it is.
    {
        result = -n - 1;
    }
    return result;
}

}