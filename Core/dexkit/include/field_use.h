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

#include <cstdint>
#include <utility>

namespace dexkit {

// Only local field operands from iget/iput/sget/sput are packed. Cross-DEX
// definitions and public IDs keep their existing widths.
using FieldUse = uint32_t;

constexpr FieldUse EncodeFieldUse(uint16_t field_id, bool is_getter) {
    return (uint32_t(field_id) << 1) | uint32_t(is_getter);
}

constexpr std::pair<uint32_t, bool> DecodeFieldUse(FieldUse use) {
    return {use >> 1, (use & 1) != 0};
}

} // namespace dexkit
