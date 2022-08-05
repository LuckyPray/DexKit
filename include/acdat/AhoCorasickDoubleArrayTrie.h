#pragma once

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
    std::vector<V> v;
    std::vector<int> l;
    int size = 0;

    std::vector<Hit<V>> parseText(const char *text);

    void parseText(const char *text, std::function<void(int, int, V)> &callback);

    void parseText(const char *text, std::function<bool(int, int, V)> callback);

    bool matches(const char *text);

    Hit<V> findFirst(const char *text);

    void save() {}

    void load() {}

    V get(const char *key);

    bool set(const char *key, V value);

    V get(int index) {
        return v[index];
    }

    int exactMatchSearch(const char *key);

    int getSize() {
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
    int getState(int currentState, unsigned char c);

    void storeEmits(int position, int currentState, std::vector<Hit<V>> &collectedEmits);

    int exactMatchSearch(const char *key, int pos, int len, int nodePos);

    int getMatched(int pos, int len, int result, const char *key, int b1);

protected:
    int transition(int current, unsigned char c);

    int transitionWithRoot(int nodePos, unsigned char c);
};

template<typename V>
std::vector<Hit<V>> AhoCorasickDoubleArrayTrie<V>::parseText(const char *text) {
    int position = 1;
    int currentState = 0;
    std::vector<Hit<V>> collectedEmits = std::vector<Hit<V>>();
    while (*text != '\0') {
        currentState = getState(currentState, *text);
        storeEmits(position, currentState, collectedEmits);
        ++position;
        ++text;
    }

    return collectedEmits;
}

template<typename V>
void AhoCorasickDoubleArrayTrie<V>::parseText(const char *text, std::function<void(int, int, V)> &callback) {
    int position = 1;
    int currentState = 0;
    while (*text != '\0') {
        currentState = getState(currentState, *text++);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            for (int &hit: hitArray) {
                callback(position - l[hit], position, v[hit]);
            }
        }
        ++position;
    }
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::getState(int currentState, unsigned char c) {
    int newCurrentState = transitionWithRoot(currentState, c);  // 先按success跳转
    while (newCurrentState == -1) // 跳转失败的话，按failure跳转
    {
        currentState = fail[currentState];
        newCurrentState = transitionWithRoot(currentState, c);
    }
    return newCurrentState;
}

template<typename V>
void AhoCorasickDoubleArrayTrie<V>::parseText(const char *text, std::function<bool(int, int, V)> callback) {
    int position = 1;
    int currentState = 0;
    while (*text != '\0') {
        currentState = getState(currentState, *text++);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            for (int &hit: hitArray) {
                bool proceed = callback.hit(position - l[hit], position, v[hit]);
                if (!proceed) {
                    return;
                }
            }
        }
        ++position;
    }
}

template<typename V>
bool AhoCorasickDoubleArrayTrie<V>::matches(const char *text) {
    int currentState = 0;
    while (*text != '\0') {
        currentState = getState(currentState, *text++);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            return true;
        }
    }
    return false;
}

template<typename V>
Hit<V> AhoCorasickDoubleArrayTrie<V>::findFirst(const char *text) {
    int position = 1;
    int currentState = 0;
    while (*text != '\0') {
        currentState = getState(currentState, *text++);
        auto hitArray = output[currentState];
        if (!hitArray.empty()) {
            int hitIndex = hitArray[0];
            return Hit<V>(position - l[hitIndex], position, v[hitIndex]);
        }
        ++position;
    }
    return nullptr;
}

template<typename V>
V AhoCorasickDoubleArrayTrie<V>::get(const char *key) {
    int index = exactMatchSearch(key);
    if (index >= 0) {
        return v[index];
    }
    return nullptr;
}

template<typename V>
bool AhoCorasickDoubleArrayTrie<V>::set(const char *key, V value) {
    int index = exactMatchSearch(key);
    if (index >= 0) {
        v[index] = value;
        return true;
    }
    return false;
}

template<typename V>
void AhoCorasickDoubleArrayTrie<V>::storeEmits(int position, int currentState, std::vector<Hit<V>> &collectedEmits) {
    auto hitArray = output[currentState];
    if (!hitArray.empty()) {
        for (int &hit: hitArray) {
            collectedEmits.push_back(Hit<V>(position - l[hit], position, v[hit]));
        }
    }
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::transition(int current, unsigned char c) {
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
int AhoCorasickDoubleArrayTrie<V>::transitionWithRoot(int nodePos, unsigned char c) {
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
int AhoCorasickDoubleArrayTrie<V>::exactMatchSearch(const char *key) {
    return exactMatchSearch(key, 0, 0, 0);
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::exactMatchSearch(const char *key, int pos, int len, int nodePos) {
    if (len <= 0) {
        len = (int) strlen(reinterpret_cast<const char *>(key));
    }
    if (nodePos <= 0) {
        nodePos = 0;
    }

    int result = -1;

    return getMatched(pos, len, result, key, base[nodePos]);
}

template<typename V>
int AhoCorasickDoubleArrayTrie<V>::getMatched(int pos, int len, int result, const char *key, int b1) {
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