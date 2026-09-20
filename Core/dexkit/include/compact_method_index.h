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
#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <limits>
#include <memory>
#include <numeric>
#include <span>
#include <type_traits>
#include <vector>

namespace dexkit {

// Built under the existing bridge warmup barrier and immutable after publication.
// The build array holds local counts first, then destination write cursors.
class CompactMethodIndex {
public:
    using Entry = MethodReference;

    void BeginCounts(size_t rows) {
        DEXKIT_CHECK(build_.empty() && offsets_.empty());
        if (rows >= std::numeric_limits<size_t>::max() / sizeof(CacheOffset)) std::abort();
        build_.resize(rows);
    }

    void Count(uint32_t method) {
        DEXKIT_CHECK(method < build_.size());
        DEXKIT_CHECK(build_[method] < std::numeric_limits<CacheOffset>::max());
        ++build_[method];
    }

    CacheOffset LocalCount(uint32_t method) const { return build_[method]; }

    // Validate local totals before cross-DEX binding consumes the row counts.
    // Individual row counters are debug-checked; an oversized total is rejected
    // here before a wrapped counter can become a layout or write bound.
    void FinishCounts(uint64_t local_edges) {
        (void)CheckedIndexCast<CacheOffset>(local_edges);
        edges_ = local_edges;
    }

    void BeginLayout() {
        DEXKIT_CHECK(std::accumulate(build_.begin(), build_.end(), uint64_t{0}) == edges_);
        offsets_.resize(build_.size() + 1);
        std::copy(build_.begin(), build_.end(), offsets_.begin() + 1);
    }

    void EmptyRow(uint32_t method) {
        DEXKIT_CHECK(method < build_.size());
        // Alias rows only export their local edges; imports go to definitions.
        DEXKIT_CHECK(offsets_[method + 1] == build_[method]);
        DEXKIT_CHECK(edges_ >= build_[method]);
        edges_ -= build_[method];
        offsets_[method + 1] = 0;
    }
    void AddRowCount(uint32_t method, CacheOffset count) {
        DEXKIT_CHECK(method < build_.size());
        DEXKIT_CHECK(count <= std::numeric_limits<uint64_t>::max() - edges_);
        edges_ += count;
        offsets_[method + 1] += count;
    }

    void Allocate() {
        // Check the independent wide total before using row counts as bounds.
        // Per-source totals fit u32, but cross-DEX imports can exceed that.
        if (edges_ > std::numeric_limits<CacheOffset>::max()
                || edges_ > std::numeric_limits<size_t>::max() / sizeof(Entry)) std::abort();
        for (size_t i = 1; i < offsets_.size(); ++i)
            offsets_[i] += offsets_[i - 1];
        DEXKIT_CHECK(offsets_.back() == edges_);
        // Entry is trivial: default initialization leaves the exact payload
        // untouched. Every element is filled before the publication barrier.
        if (edges_) values_.reset(new Entry[static_cast<size_t>(edges_)]);
#if !defined(NDEBUG)
        expected_ends_.resize(build_.size());
#endif
    }

    CacheOffset RowBegin(uint32_t method) const { return offsets_[method]; }
    CacheOffset RowEnd(uint32_t method) const { return offsets_[method + 1]; }
    CacheOffset &Cursor(uint32_t method) { return build_[method]; }

    void ReserveLocal(uint32_t row) {
        const auto begin = RowBegin(row);
        const auto count = LocalCount(row);
        DEXKIT_CHECK(uint64_t(begin) + count <= RowEnd(row));
        build_[row] = begin + count;
#if !defined(NDEBUG)
        expected_ends_[row] = build_[row];
#endif
    }

    void SetImportCursor(uint32_t row, CacheOffset begin) {
#if !defined(NDEBUG)
        DEXKIT_CHECK(uint64_t(begin) + LocalCount(row) <= std::numeric_limits<CacheOffset>::max());
        expected_ends_[row] = begin + LocalCount(row);
#endif
        build_[row] = begin;
    }

    void Write(size_t position, uint16_t dex, uint32_t method) {
        DEXKIT_CHECK(position < edges_);
        values_[position] = Entry{dex, method};
    }

    void ReleaseBuild() {
#if !defined(NDEBUG)
        DEXKIT_CHECK(build_ == expected_ends_);
        std::vector<CacheOffset>().swap(expected_ends_);
#endif
        std::vector<CacheOffset>().swap(build_);
    }
    bool empty() const { return offsets_.size() <= 1; }
    size_t size() const { return offsets_.empty() ? 0 : offsets_.size() - 1; }

    std::span<const Entry> operator[](size_t method) const {
        DEXKIT_CHECK(method < size());
        const auto begin = offsets_[method], end = offsets_[method + 1];
        DEXKIT_CHECK(begin <= end && end <= edges_);
        if (begin == end) return {};
        return {values_.get() + begin, end - begin};
    }

private:
    std::vector<CacheOffset> build_;
#if !defined(NDEBUG)
    std::vector<CacheOffset> expected_ends_;
#endif
    std::vector<CacheOffset> offsets_;
    std::unique_ptr<Entry[]> values_;
    // Wide during layout so overflowing provisional row counts cannot hide
    // an oversized allocation. Once Allocate succeeds, this fits CacheOffset.
    uint64_t edges_ = 0;
};

} // namespace dexkit
