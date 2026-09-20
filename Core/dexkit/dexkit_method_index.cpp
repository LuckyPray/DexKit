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

#include "dexkit.h"

#include "ThreadPool.h"

namespace dexkit {

// Plan disjoint output ranges before any worker writes a payload. Local edges
// precede imports, and imports keep the original source/work-list ordering.
void DexKit::PrepareCompactMethodIndex(CompactMethodIndex DexItem::*member, bool fields) {
    const auto visit_imports = [&](auto &&visit) {
        for (auto &owner : dex_items) {
            auto &source = owner.get()->*member;
            if (fields) {
                for (const auto &work : owner->pending_aggregate_field_work_items)
                    visit(source, work.source_field_idx, dex_items[work.target_dex_id].get()->*member,
                          work.target_field_idx);
            } else {
                for (const auto &work : owner->pending_aggregate_method_work_items)
                    visit(source, work.source_method_idx, dex_items[work.target_dex_id].get()->*member,
                          work.target_method_idx);
            }
        }
    };
    for (auto &owner : dex_items) (owner.get()->*member).BeginLayout();
    visit_imports([](auto &source, uint32_t row, auto &target, uint32_t destination) {
        const auto count = source.LocalCount(row);
        if (count == 0) return;
        source.EmptyRow(row);
        target.AddRowCount(destination, count);
    });
    for (auto &owner : dex_items) {
        auto &index = owner.get()->*member;
        const auto &bindings = fields ? owner->field_cross_info : owner->method_cross_info;
        index.Allocate();
        for (uint32_t row = 0; row < index.size(); ++row) {
            if (!bindings[row]) index.ReserveLocal(row);
            else DEXKIT_CHECK(index.RowBegin(row) == index.RowEnd(row));
        }
    }
    visit_imports([](auto &source, uint32_t row, auto &target, uint32_t destination) {
        const auto count = source.LocalCount(row);
        if (count == 0) return;
        auto &next = target.Cursor(destination);
        DEXKIT_CHECK(uint64_t(next) + count <= target.RowEnd(destination));
        source.SetImportCursor(row, next);
        next += count;
    });
    for (auto &owner : dex_items) {
        auto &index = owner.get()->*member;
        const auto &bindings = fields ? owner->field_cross_info : owner->method_cross_info;
        for (uint32_t row = 0; row < index.size(); ++row) {
            if (!bindings[row]) {
                DEXKIT_CHECK(index.Cursor(row) == index.RowEnd(row));
                index.Cursor(row) = index.RowBegin(row);
            }
        }
    }
}

void DexKit::BuildCompactCallers(uint32_t thread_num) {
    PrepareCompactMethodIndex(&DexItem::method_caller_ids, false);
    const auto fill_source = [this](DexItem *source) {
        auto &index = source->method_caller_ids;
        for (const auto &definition : source->reader.ClassDefs()) {
            for (auto caller : source->class_method_ids[definition.class_idx]) {
                for (auto invoked : source->method_invoking_ids[caller]) {
                    auto &cursor = index.Cursor(invoked);
                    const auto &binding = source->method_cross_info[invoked];
                    auto &target = binding ? dex_items[binding.value().first]->method_caller_ids : index;
                    target.Write(cursor++, static_cast<uint16_t>(source->dex_id), caller);
                }
            }
        }
    };
    if (thread_num > 1 && dex_items.size() > 1) {
        ThreadPool pool(std::min(static_cast<size_t>(thread_num), dex_items.size()));
        for (auto &owner : dex_items) pool.enqueue(fill_source, owner.get());
    } else {
        for (auto &owner : dex_items) fill_source(owner.get());
    }
    for (auto &owner : dex_items) {
        owner->method_caller_ids.ReleaseBuild();
        decltype(owner->pending_aggregate_method_work_items)().swap(owner->pending_aggregate_method_work_items);
    }
}

void DexKit::BuildCompactFields(uint32_t thread_num) {
    PrepareCompactMethodIndex(&DexItem::field_get_method_ids, true);
    PrepareCompactMethodIndex(&DexItem::field_put_method_ids, true);
    const auto fill_source = [this](DexItem *source) {
        for (const auto &definition : source->reader.ClassDefs()) {
            for (auto method : source->class_method_ids[definition.class_idx]) {
                for (auto field_use : source->method_using_field_ids[method]) {
                    const auto [field, is_getter] = DecodeFieldUse(field_use);
                    const auto &binding = source->field_cross_info[field];
                    auto *target = binding ? dex_items[binding.value().first].get() : source;
                    auto &index = is_getter ? source->field_get_method_ids : source->field_put_method_ids;
                    auto &output = is_getter ? target->field_get_method_ids : target->field_put_method_ids;
                    output.Write(index.Cursor(field)++, static_cast<uint16_t>(source->dex_id), method);
                }
            }
        }
    };
    if (thread_num > 1 && dex_items.size() > 1) {
        ThreadPool pool(std::min(static_cast<size_t>(thread_num), dex_items.size()));
        for (auto &owner : dex_items) pool.enqueue(fill_source, owner.get());
    } else {
        for (auto &owner : dex_items) fill_source(owner.get());
    }
    for (auto &owner : dex_items) {
        owner->field_get_method_ids.ReleaseBuild();
        owner->field_put_method_ids.ReleaseBuild();
        decltype(owner->pending_aggregate_field_work_items)().swap(owner->pending_aggregate_field_work_items);
    }
    // FinishBuildCrossRefAggregates publishes both immutable reverse tables.
}

} // namespace dexkit
