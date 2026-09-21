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

#include <algorithm>
#include <bit>
#include <cstdint>
#include <limits>
#include <optional>
#include <vector>

namespace dexkit::inverted_string {

inline constexpr size_t kBitmapBudget = 16 * 1024 * 1024;
inline size_t WordCount(size_t entities) { return entities / 64 + (entities % 64 != 0); }

// Chosen on the submitting thread, before changing task granularity. In
// particular, Legacy must not turn into an inverse scan after publication by
// another query. A range selected from a published index never waits/builds.
struct QueryPlan {
    enum class Route { Legacy, Keywords, Range, Empty };
    Route route = Route::Legacy;
    size_t begin = 0, end = 0;
    bool index_ready = false;
    // A range chosen from one of several conditions is only a necessary seed.
    bool proves_all_strings = false;
    bool Admitted() const { return route != Route::Legacy; }
};

// Per-DEX requested bitmap bytes, not allocator capacity or a process limit.
// Check both construction and consumption: a method batch retains its groups
// while adding a method union and a differently sized type union.
inline std::optional<size_t> BitmapPlanBytes(size_t entities, size_t types, size_t keywords, size_t groups) {
    const auto words = WordCount(entities), type_words = WordCount(types);
    if (!words || !keywords || !groups || words > kBitmapBudget / sizeof(uint64_t)
            || type_words > kBitmapBudget / sizeof(uint64_t)) return std::nullopt;
    const auto bytes = words * sizeof(uint64_t), type_bytes = type_words * sizeof(uint64_t);
    const auto slots = kBitmapBudget / bytes;
    if (keywords > slots || groups > slots - keywords || slots - keywords - groups < 2) return std::nullopt;
    const auto consumer_slots = (kBitmapBudget - type_bytes) / bytes;
    if (groups >= consumer_slots) return std::nullopt;
    return std::max((keywords + groups + 2) * bytes, (groups + 1) * bytes + type_bytes);
}

class Bits {
public:
    Bits() = default;
    explicit Bits(size_t count) : words(WordCount(count)) {}
    void Set(size_t id) { words[id / 64] |= uint64_t{1} << (id % 64); }
    bool Has(size_t id) const { return words[id / 64] & (uint64_t{1} << (id % 64)); }
    void Or(const Bits &other) {
        for (size_t i = 0; i < words.size(); ++i) words[i] |= other.words[i];
    }
    void And(const Bits &other) {
        for (size_t i = 0; i < words.size(); ++i) words[i] &= other.words[i];
    }
    template<class Visit> void Each(Visit &&visit) const {
        for (size_t i = 0; i < words.size(); ++i) {
            auto word = words[i];
            while (word) {
                visit(static_cast<uint32_t>(i * 64 + std::countr_zero(word)));
                word &= word - 1;
            }
        }
    }
    std::vector<uint64_t> words;
};

class RankBits : public Bits {
public:
    RankBits() = default;
    explicit RankBits(size_t count) : Bits(count), prefix(words.size() + 1) {}
    void Finish() {
        for (size_t i = 0; i < words.size(); ++i) prefix[i + 1] = prefix[i] + std::popcount(words[i]);
    }
    uint32_t Rank(size_t id) const {
        const size_t word = id / 64, bit = id % 64;
        return prefix[word] + (bit ? std::popcount(words[word] & ((uint64_t{1} << bit) - 1)) : 0);
    }
private:
    std::vector<uint32_t> prefix;
};

// Built from a ready immutable forward index; no code bytes are decoded here.
// Each pass visits methods in ID order, deduplicating repeated const-string
// instructions. Counts are reused as exact row cursors in the second pass.
class Index {
public:
    template<class Forward> bool Build(size_t strings, size_t methods, const Forward &forward) {
        if (strings > UINT32_MAX || methods > UINT32_MAX) return false;
        std::vector<uint32_t> counts(strings), seen(strings, UINT32_MAX);
        uint64_t edges = 0;
        uint32_t used = 0, singles = 0;
        for (uint32_t method = 0; method < methods; ++method) {
            for (auto id : forward[method]) {
                if (seen[id] == method) continue;
                seen[id] = method;
                if (++edges > UINT32_MAX) return false;
                ++counts[id];
            }
        }
        for (auto count : counts) { used += count != 0; singles += count == 1; }
        narrow = methods <= uint64_t{UINT16_MAX} + 1;
        presence = RankBits(strings);
        multiple = RankBits(used);
        offsets.resize(static_cast<size_t>(used) - singles + 1);
        if (narrow) { one16.resize(singles); many16.resize(edges - singles); }
        else { one32.resize(singles); many32.resize(edges - singles); }
        uint32_t row = 0, one = 0, multi = 0, value = 0;
        for (uint32_t id = 0; id < strings; ++id) {
            const auto count = counts[id];
            if (!count) continue;
            presence.Set(id);
            if (count == 1) counts[id] = one++;
            else {
                multiple.Set(row);
                offsets[multi++] = value;
                counts[id] = value;
                value += count;
            }
            ++row;
        }
        offsets[multi] = value;
        presence.Finish();
        multiple.Finish();
        std::fill(seen.begin(), seen.end(), UINT32_MAX);
        for (uint32_t method = 0; method < methods; ++method) {
            for (auto id : forward[method]) {
                if (seen[id] == method) continue;
                seen[id] = method;
                const auto position = counts[id]++;
                const bool many = multiple.Has(presence.Rank(id));
                if (narrow) (many ? many16 : one16)[position] = static_cast<uint16_t>(method);
                else (many ? many32 : one32)[position] = method;
            }
        }
        ready = true;
        return true;
    }
    template<class Visit> void VisitRange(size_t begin, size_t end, Visit &&visit) const {
        const auto a = presence.Rank(begin), b = presence.Rank(end);
        const auto ma = multiple.Rank(a), mb = multiple.Rank(b);
        if (narrow) {
            for (size_t i = a - ma; i < b - mb; ++i) visit(one16[i]);
            for (size_t i = offsets[ma]; i < offsets[mb]; ++i) visit(many16[i]);
        } else {
            for (size_t i = a - ma; i < b - mb; ++i) visit(one32[i]);
            for (size_t i = offsets[ma]; i < offsets[mb]; ++i) visit(many32[i]);
        }
    }
    // Counts distinct (method, string) edges, not the union of method IDs.
    // Two matching strings in the same method therefore count twice.
    size_t CountRange(size_t begin, size_t end) const {
        const auto a = presence.Rank(begin), b = presence.Rank(end);
        const auto ma = multiple.Rank(a), mb = multiple.Rank(b);
        return size_t(b - a) - (mb - ma) + size_t(offsets[mb]) - offsets[ma];
    }
    template<class Visit> void EachString(Visit &&visit) const { presence.Each(visit); }
    bool Ready() const { return ready; }
private:
    bool ready = false, narrow = false;
    RankBits presence, multiple;
    std::vector<uint32_t> offsets;
    std::vector<uint16_t> one16, many16;
    std::vector<uint32_t> one32, many32;
};

// Only the exact root using_strings predicate is cached. Recursive matching on
// another DEX, entity kind, or matcher vector always uses the existing matcher.
struct MatchScope {
    inline static thread_local const MatchScope *current = nullptr;
    const MatchScope *previous;
    const void *dex;
    const void *matchers;
    bool classes;
    const Bits *hits;
    MatchScope(const void *owner, const void *source, bool class_query, const Bits *bits)
        : previous(current), dex(owner), matchers(source), classes(class_query), hits(bits) { current = this; }
    ~MatchScope() { current = previous; }
    MatchScope(const MatchScope &) = delete;
    MatchScope &operator=(const MatchScope &) = delete;
};

} // namespace dexkit::inverted_string
