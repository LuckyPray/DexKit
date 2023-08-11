#pragma once

#include <vector>
#include <cstdint>

namespace kmp {

static void FindNext(const std::vector<uint8_t> &data, const std::vector<uint8_t> &find, std::vector<int> &next) {
    int i = 0;
    int k = next[0] = -1;
    int len = (int) find.size();
    while (i < len) {
        if (k == -1 || find[i] == find[k]) {
            next[++i] = ++k;
        } else {
            k = next[k];
        }
    }
}

static int FindIndex(const std::vector<uint8_t> &data, const std::vector<uint8_t> &find) {
    std::vector<int> next(find.size() + 5);
    FindNext(data, find, next);
    int i = 0, j = 0;
    int data_len = (int) data.size();
    int find_len = (int) find.size();
    while (i < data_len && j < find_len) {
        if (j == -1 || data[i] == find[j]) {
            ++i, ++j;
        } else {
            j = next[j];
        }
    }
    if (j == find_len) {
        return i - j;
    }
    return -1;
}

inline char GetIgnoreCaseChar(char c, bool ignore_case = false) {
    if (ignore_case) {
        if (c >= 'A' && c <= 'Z') {
            return char(c + 32);
        }
    }
    return c;
}

static void FindNext(const std::string_view &data, const std::string_view &find, std::vector<int> &next, bool ignore_case = false) {
    int i = 0;
    int k = next[0] = -1;
    int len = (int) find.size();
    while (i < len) {
        char c1 = GetIgnoreCaseChar(find[i], ignore_case);
        char c2 = GetIgnoreCaseChar(find[k], ignore_case);
        if (k == -1 || c1 == c2) {
            next[++i] = ++k;
        } else {
            k = next[k];
        }
    }
}

static int FindIndex(const std::string_view &data, const std::string_view &find, bool ignore_case = false) {
    std::vector<int> next(find.size() + 5);
    FindNext(data, find, next);
    int i = 0, j = 0;
    int data_len = (int) data.size();
    int find_len = (int) find.size();
    while (i < data_len && j < find_len) {
        char c1 = GetIgnoreCaseChar(data[i], ignore_case);
        char c2 = GetIgnoreCaseChar(find[j], ignore_case);
        if (j == -1 || c1 == c2) {
            ++i, ++j;
        } else {
            j = next[j];
        }
    }
    if (j == find_len) {
        return i - j;
    }
    return -1;
}

} // namespace kmp