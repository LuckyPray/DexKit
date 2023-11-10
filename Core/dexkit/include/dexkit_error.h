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

namespace dexkit {

enum class Error : uint16_t {
#define DEXKIT_ERROR(name, msg) name,
#include "dexkit_error_list.h"
    DEXKIT_ERROR_LIST(DEXKIT_ERROR)
#undef DEXKIT_ERROR_LIST
#undef DEXKIT_ERROR
};

constexpr std::string_view error_messages[] = {
#define DEXKIT_ERROR(name, msg) msg,
#include "dexkit_error_list.h"
        DEXKIT_ERROR_LIST(DEXKIT_ERROR)
#undef DEXKIT_ERROR_LIST
#undef DEXKIT_ERROR
};

constexpr size_t error_messages_size = sizeof(error_messages) / sizeof(std::string_view);

inline std::string_view GetErrorMessage(Error e) {
    if (static_cast<uint16_t>(e) >= error_messages_size) return "";
    return error_messages[static_cast<size_t>(e)];
}

} // namespace dexkit