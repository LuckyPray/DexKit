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

static void FindNext(const std::string_view &data, const std::string_view &find, std::vector<int> &next) {
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

static int FindIndex(const std::string_view &data, const std::string_view &find) {
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

} // namespace kmp