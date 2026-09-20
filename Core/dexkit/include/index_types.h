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

#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <limits>
#include <type_traits>
#include <utility>

namespace dexkit {

using CacheOffset = uint32_t;
using InvokeOperandId = uint16_t;
using ClassDefIndex = uint16_t;

// Method definitions/metadata can have wider IDs than invoke operands.
using LocalMethodId = uint32_t;

struct MethodReference {
    uint16_t first;
    LocalMethodId second;
    bool operator==(const MethodReference &) const = default;
};
static_assert(std::is_trivial_v<MethodReference>);
static_assert(sizeof(MethodReference) == 8);

// Only storage is narrowed. Arithmetic and public IDs retain their full width.
template<class To, class From>
constexpr To CheckedIndexCast(From value) {
    static_assert(std::is_integral_v<To> && std::is_unsigned_v<To> && std::is_integral_v<From>);
    static_assert(sizeof(To) <= sizeof(uintmax_t) && sizeof(From) <= sizeof(uintmax_t));
    // The Android prefab standard library does not provide std::in_range.
    if constexpr (std::is_signed_v<From>) {
        if (value < 0) std::abort();
    }
    if (static_cast<uintmax_t>(value) > static_cast<uintmax_t>(std::numeric_limits<To>::max())) std::abort();
    return static_cast<To>(value);
}

} // namespace dexkit
