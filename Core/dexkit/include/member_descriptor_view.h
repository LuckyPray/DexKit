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
#include <optional>
#include <string_view>

namespace dexkit::internal {

// Views borrow the input of a single lookup. They never own/cache a member.
struct MethodDescriptorView {
    std::string_view declaring_type;
    std::string_view name;
    std::string_view parameters;
    std::string_view return_type;
    size_t parameter_count;
};

struct FieldDescriptorView {
    std::string_view declaring_type;
    std::string_view name;
    std::string_view type;
};

inline bool ConsumeDescriptorType(std::string_view &remaining, bool allow_void) {
    if (remaining.empty()) return false;
    size_t position = 0;
    while (position < remaining.size() && remaining[position] == '[') ++position;
    if (position == remaining.size()) return false;
    const char type = remaining[position++];
    if (type == 'L') {
        const auto end = remaining.find(';', position);
        if (end == std::string_view::npos || end == position) return false;
        position = end + 1;
    } else if (std::string_view("ZBSCIJFD").find(type) == std::string_view::npos
               && !(type == 'V' && allow_void && position == 1)) {
        return false;
    }
    remaining.remove_prefix(position);
    return true;
}

inline std::optional<MethodDescriptorView> ParseMethodDescriptorView(std::string_view text) {
    const auto arrow = text.find("->");
    if (arrow == std::string_view::npos) return std::nullopt;
    const auto open = text.find('(', arrow + 2);
    if (open == std::string_view::npos) return std::nullopt;
    const auto close = text.find(')', open + 1);
    if (close == std::string_view::npos) return std::nullopt;
    MethodDescriptorView result{text.substr(0, arrow), text.substr(arrow + 2, open - arrow - 2),
        text.substr(open + 1, close - open - 1), text.substr(close + 1), 0};
    auto parameters = result.parameters;
    while (!parameters.empty()) {
        if (!ConsumeDescriptorType(parameters, false)) return std::nullopt;
        ++result.parameter_count;
    }
    auto return_type = result.return_type;
    if (!ConsumeDescriptorType(return_type, true) || !return_type.empty()) return std::nullopt;
    return result;
}

inline std::optional<FieldDescriptorView> ParseFieldDescriptorView(std::string_view text) {
    const auto arrow = text.find("->");
    if (arrow == std::string_view::npos) return std::nullopt;
    const auto colon = text.find(':', arrow + 2);
    if (colon == std::string_view::npos) return std::nullopt;
    FieldDescriptorView result{text.substr(0, arrow), text.substr(arrow + 2, colon - arrow - 2),
        text.substr(colon + 1)};
    auto type = result.type;
    if (!ConsumeDescriptorType(type, false) || !type.empty()) return std::nullopt;
    return result;
}

} // namespace dexkit::internal
