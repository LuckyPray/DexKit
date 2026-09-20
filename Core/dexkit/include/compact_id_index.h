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
#include "index_types.h"
#include "field_use.h"
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <limits>
#include <span>
#include <vector>

namespace dexkit {

// Immutable after publication. Build every row in increasing row-ID order,
// including empty rows. Adjacent boundaries delimit rows and preserve duplicates.
template<class Id>
class CompactIdIndex {
public:
    using value_type = Id;
    void resize(size_t rows) {
        if (rows == std::numeric_limits<size_t>::max()) std::abort();
        offsets_.resize(rows + 1);
    }

    // Known-size rows can append into one allocation without zero-initializing
    // a second payload. Check the wide total before narrowing or allocating.
    void ReserveValues(uint64_t values) {
        DEXKIT_CHECK(ids_.empty());
        if (values > std::numeric_limits<CacheOffset>::max()
                || values > std::numeric_limits<size_t>::max() / sizeof(Id)) std::abort();
        ids_.reserve(static_cast<size_t>(values));
    }

    bool empty() const { return offsets_.size() <= 1; }

    // Sparse builders may stage a source offset in each ending boundary.
    // Visit rows in order and read the source before EndRow replaces it.
    void StageRowSource(uint32_t row, CacheOffset source) {
        DEXKIT_CHECK(ids_.empty() && !empty() && row < offsets_.size() - 1);
        offsets_[size_t(row) + 1] = source;
    }

    CacheOffset RowSource(uint32_t row) const {
        DEXKIT_CHECK(!empty() && row < offsets_.size() - 1);
        return offsets_[size_t(row) + 1];
    }

    std::vector<Id> *BeginRow(uint32_t row) {
        DEXKIT_CHECK(!empty() && row < offsets_.size() - 1);
        offsets_[row] = static_cast<CacheOffset>(ids_.size());
        return &ids_;
    }

    void EndRow(uint32_t row) {
        DEXKIT_CHECK(!empty() && row < offsets_.size() - 1);
        const auto end = static_cast<CacheOffset>(ids_.size());
        DEXKIT_CHECK(offsets_[row] <= end);
        offsets_[size_t(row) + 1] = end;
    }

    // No offsets may be consumed before this check. The vector retains the
    // full size even if a provisional 32-bit boundary wrapped while building.
    void FinishBuild() const {
        (void) CheckedIndexCast<CacheOffset>(ids_.size());
    }

    size_t ValueCount() const { return ids_.size(); }

    std::span<const Id> operator[](size_t row) const {
        DEXKIT_CHECK(!empty() && row < offsets_.size() - 1);
        const auto begin = offsets_[row], end = offsets_[row + 1];
        DEXKIT_CHECK(begin <= end && end <= ids_.size());
        return std::span<const Id>(ids_).subspan(begin, end - begin);
    }

private:
    std::vector<CacheOffset> offsets_;
    std::vector<Id> ids_;
};

// Base members and code-derived rows share the checked append/freeze representation.
using CompactClassMethodIndex = CompactIdIndex<LocalMethodId>;
using CompactStringIndex = CompactIdIndex<uint32_t>;
using CompactInvocationIndex = CompactIdIndex<InvokeOperandId>;
using CompactFieldUseIndex = CompactIdIndex<FieldUse>;
using CompactOpcodeIndex = CompactIdIndex<uint8_t>;

} // namespace dexkit
