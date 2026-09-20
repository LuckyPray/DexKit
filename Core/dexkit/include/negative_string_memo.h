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
#include <memory>
#include <new>
#include "query_context.h"

namespace dexkit {

// Owned by one batch-query / DEX job. A bit means ParseText completed with no
// hits for this exact trie. Positive and unvisited values both take the normal
// path; no incomplete result, query identity or cross-DEX ID is shared.
class NegativeStringMemo {
    struct Lease {
        QueryContext &query;
        size_t bytes = 0;
        Lease(QueryContext &query, size_t requested) : query(query) {
            if (query.TryReserveStringMemo(requested)) bytes = requested;
        }
        void Reset() { query.ReleaseStringMemo(bytes); bytes = 0; }
        ~Lease() { Reset(); }
    };
public:
    NegativeStringMemo(QueryContext &query, size_t strings)
        : lease_(query, (strings / 64 + (strings % 64 != 0)) * sizeof(uint64_t)) {
        // This optimization is optional: allocation failure falls back to parsing.
        if (lease_.bytes) {
            bits_.reset(new (std::nothrow) uint64_t[lease_.bytes / sizeof(uint64_t)]());
            if (!bits_) lease_.Reset();
        }
    }
    NegativeStringMemo(const NegativeStringMemo &) = delete;
    NegativeStringMemo &operator=(const NegativeStringMemo &) = delete;

    bool Contains(uint32_t string) const {
        return bits_ && (bits_[string / 64] & (uint64_t{1} << (string % 64)));
    }

    void RecordEmpty(uint32_t string) {
        if (bits_) bits_[string / 64] |= uint64_t{1} << (string % 64);
    }

private:
    Lease lease_;
    std::unique_ptr<uint64_t[]> bits_;
};

} // namespace dexkit
