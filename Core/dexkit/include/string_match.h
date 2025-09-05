// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#pragma once

#include <vector>
#include <cstdint>
#include <optional>

namespace kmp {

static void FindNext(const std::vector<uint8_t> &data, const std::vector<std::optional<uint8_t>> &find, std::vector<int> &next) {
    int i = 0;
    int k = next[0] = -1;
    int len = (int) find.size();
    while (i < len) {
        if (k == -1 || find[k] == std::nullopt || find[i] == find[k]) {
            next[++i] = ++k;
        } else {
            k = next[k];
        }
    }
}

static int FindIndex(const std::vector<uint8_t> &data, const std::vector<std::optional<uint8_t>> &find) {
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
        if (k == -1) {
            next[++i] = ++k;
            continue;
        }
        char c1 = GetIgnoreCaseChar(find[i], ignore_case);
        char c2 = GetIgnoreCaseChar(find[k], ignore_case);
        if (c1 == c2) {
            next[++i] = ++k;
        } else {
            k = next[k];
        }
    }
}

static int FindIndex(const std::string_view &data, const std::string_view &find, bool ignore_case = false) {
    if (find.empty()) return 0;
    std::vector<int> next(find.size() + 5);
    FindNext(data, find, next, ignore_case);
    int i = 0, j = 0;
    int data_len = (int) data.size();
    int find_len = (int) find.size();
    while (i < data_len && j < find_len) {
        if (j == -1) {
            ++i, ++j;
            continue;
        }
        char c1 = GetIgnoreCaseChar(data[i], ignore_case);
        char c2 = GetIgnoreCaseChar(find[j], ignore_case);
        if (c1 == c2) {
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

static bool starts_with(const std::string_view &source, const std::string_view &target, bool ignore_case = false) {
    if (source.size() < target.size()) return false;
    for (int i = 0; i < target.size(); ++i) {
        char c1 = GetIgnoreCaseChar(source[i], ignore_case);
        char c2 = GetIgnoreCaseChar(target[i], ignore_case);
        if (c1 != c2) {
            return false;
        }
    }
    return true;
}

static bool ends_with(const std::string_view &source, const std::string_view &target, bool ignore_case = false) {
    if (source.size() < target.size()) return false;
    for (int i = 0; i < target.size(); ++i) {
        char c1 = GetIgnoreCaseChar(source[source.size() - target.size() + i], ignore_case);
        char c2 = GetIgnoreCaseChar(target[i], ignore_case);
        if (c1 != c2) {
            return false;
        }
    }
    return true;
}

static bool equals(const std::string_view &source, const std::string_view &target, bool ignore_case = false) {
    if (source.size() != target.size()) return false;
    for (int i = 0; i < target.size(); ++i) {
        char c1 = GetIgnoreCaseChar(source[i], ignore_case);
        char c2 = GetIgnoreCaseChar(target[i], ignore_case);
        if (c1 != c2) {
            return false;
        }
    }
    return true;
}

} // namespace kmp