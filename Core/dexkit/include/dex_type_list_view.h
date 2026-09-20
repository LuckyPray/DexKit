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
#include "slicer/dex_format.h"

namespace dexkit {
class DexTypeListView {
    const dex::TypeItem *items_ = nullptr;
    size_t size_ = 0;

public:
    DexTypeListView() = default;
    explicit DexTypeListView(const dex::TypeList *list)
        : items_(list ? list->list : nullptr), size_(list ? list->size : 0) {}
    size_t size() const { return size_; }
    bool empty() const { return size_ == 0; }
    uint32_t operator[](size_t index) const { return items_[index].type_idx; }
};
} // namespace dexkit
