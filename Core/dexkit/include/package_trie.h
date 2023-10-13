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
#include <string_view>

namespace trie {

const int NODE_SIZE = UINT8_MAX + 1;

struct PackageTrie {
    std::vector<std::vector<int>> nodes;
    std::vector<uint8_t> flags;

    PackageTrie() {
        nodes.emplace_back(NODE_SIZE, -1);
        flags.emplace_back(0);
    }

    void insert(const std::string_view word, bool is_white, bool ignore_case = false) {
        int currentNode = 0;

        for (uint8_t c : word) {
            if (ignore_case) {
                c = c >= 'A' && c <= 'Z' ? (c + 32) : c;
            }
            if (nodes[currentNode][c] == -1) {
                nodes[currentNode][c] = (int) nodes.size();
                nodes.emplace_back(NODE_SIZE, -1);
                flags.emplace_back(0);
            }
            currentNode = nodes[currentNode][c];
        }

        flags[currentNode] = flags[currentNode] | (1 << is_white);
    }

    uint8_t search(const std::string_view word, bool ignore_case = false) {
        int currentNode = 0;
        std::vector<std::string_view> result;

        uint8_t flag = 0;

        for (uint8_t c : word) {
            if (ignore_case) {
                c = c >= 'A' && c <= 'Z' ? (c + 32) : c;
            }
            if (nodes[currentNode][c] == -1) {
                return flag;
            }
            currentNode = nodes[currentNode][c];

            if (flags[currentNode]) {
                if (flags[currentNode] & 1) {
                    flag |= 1;
                }
                if (flags[currentNode] & 2) {
                    flag |= 2;
                }
            }
        }

        return flag;
    }
};

}