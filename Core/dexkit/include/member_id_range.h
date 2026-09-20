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

#include "common.h"
#include <cstddef>
#include <cstdint>
#include <iterator>

namespace dexkit {

// A contiguous interval in a raw DEX ID table, without an allocated ID array.
class MemberIdRange {
public:
    struct Iterator {
        using iterator_category = std::forward_iterator_tag;
        using value_type = uint32_t;
        using difference_type = std::ptrdiff_t;
        using pointer = void;
        using reference = uint32_t;

        uint32_t value = 0;
        uint32_t operator*() const { return value; }
        Iterator &operator++() { ++value; return *this; }
        Iterator operator++(int) { auto previous = *this; ++*this; return previous; }
        bool operator==(const Iterator &) const = default;
    };

    MemberIdRange() = default;
    MemberIdRange(uint32_t begin, uint32_t end) : begin_(begin), end_(end) {
        DEXKIT_CHECK(begin <= end);
    }

    size_t size() const { return size_t(end_) - begin_; }
    bool empty() const { return begin_ == end_; }
    uint32_t operator[](size_t index) const {
        DEXKIT_CHECK(index < size());
        return begin_ + static_cast<uint32_t>(index);
    }
    Iterator begin() const { return {begin_}; }
    Iterator end() const { return {end_}; }

private:
    uint32_t begin_ = 0;
    uint32_t end_ = 0;
};

} // namespace dexkit
