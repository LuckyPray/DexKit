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

#include "dex_item.h"

namespace dexkit {

namespace {

template<bool kEarlyExit, typename MatchFn>
void ScanFindRange(
        uint32_t start,
        uint32_t end,
        QueryContext &query_context,
        MatchFn &&match_fn
) {
    for (auto i = start; i < end; ++i) {
        if constexpr (kEarlyExit) {
            if (query_context.ShouldEarlyExit()) break;
        }
        if (!match_fn(i)) continue;
        if constexpr (kEarlyExit) {
            (void) query_context.RequestEarlyExit();
            break;
        }
    }
}

template<bool kEarlyExit, typename Range, typename MatchFn>
void ScanFindItems(const Range &items, QueryContext &query_context, MatchFn &&match_fn) {
    for (auto item : items) {
        if constexpr (kEarlyExit) {
            if (query_context.ShouldEarlyExit()) break;
        }
        if (!match_fn(item)) continue;
        if constexpr (kEarlyExit) {
            (void) query_context.RequestEarlyExit();
            break;
        }
    }
}

} // namespace

std::vector<std::future<std::vector<ClassBean>>>
DexItem::FindClass(
        const schema::FindClass *query,
        const std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        IQueryExecutor &executor,
        uint32_t slice_size,
        QueryContext &query_context
) {
    std::vector<std::future<std::vector<ClassBean>>> futures;
    uint32_t split_count;
    auto should_stop_submission = query_context.IsEarlyExitEnabled();
    if (slice_size > 0) {
        split_count = (this->reader.ClassDefs().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.ClassDefs().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        if (should_stop_submission && executor.ShouldSkipTask()) break;
        query_context.MarkTaskSubmitted();
        futures.emplace_back(SubmitQueryTask(executor,
                [this, query, &in_class_set, &packageTrie, i, slice_size, &query_context] {
                    auto task_scope = query_context.TrackTaskExecution();
                    auto result = FindClass(query, in_class_set, packageTrie, i * slice_size,
                                            std::min((i + 1) * slice_size, (uint32_t) this->reader.ClassDefs().size()),
                                            query_context);
                    query_context.MarkTaskCompleted();
                    return result;
                }
        ));
    }
    return futures;
}

std::vector<std::future<std::vector<MethodBean>>>
DexItem::FindMethod(
        const schema::FindMethod *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        IQueryExecutor &executor,
        uint32_t slice_size,
        QueryContext &query_context
) {
    std::vector<std::future<std::vector<MethodBean>>> futures;
    uint32_t split_count;
    auto should_stop_submission = query_context.IsEarlyExitEnabled();
    if (slice_size > 0) {
        split_count = (this->reader.MethodIds().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.MethodIds().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        if (should_stop_submission && executor.ShouldSkipTask()) break;
        query_context.MarkTaskSubmitted();
        futures.emplace_back(SubmitQueryTask(executor,
                [this, query, &in_class_set, &in_method_set, &packageTrie, i, slice_size, &query_context] {
                    auto task_scope = query_context.TrackTaskExecution();
                    auto result = FindMethod(query, in_class_set, in_method_set, packageTrie, i * slice_size,
                                             std::min((i + 1) * slice_size, (uint32_t) this->reader.MethodIds().size()),
                                             query_context);
                    query_context.MarkTaskCompleted();
                    return result;
                }
        ));
    }
    return futures;
}

std::vector<std::future<std::vector<FieldBean>>>
DexItem::FindField(
        const schema::FindField *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_field_set,
        trie::PackageTrie &packageTrie,
        IQueryExecutor &executor,
        uint32_t slice_size,
        QueryContext &query_context
) {
    std::vector<std::future<std::vector<FieldBean>>> futures;
    uint32_t split_count;
    auto should_stop_submission = query_context.IsEarlyExitEnabled();
    if (slice_size > 0) {
        split_count = (this->reader.FieldIds().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.FieldIds().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        if (should_stop_submission && executor.ShouldSkipTask()) break;
        query_context.MarkTaskSubmitted();
        futures.emplace_back(SubmitQueryTask(executor,
                [this, query, &in_class_set, &in_field_set, &packageTrie, i, slice_size, &query_context] {
                    auto task_scope = query_context.TrackTaskExecution();
                    auto result = FindField(query, in_class_set, in_field_set, packageTrie, i * slice_size,
                                            std::min((i + 1) * slice_size, (uint32_t) this->reader.FieldIds().size()),
                                            query_context);
                    query_context.MarkTaskCompleted();
                    return result;
                }
        ));
    }
    return futures;
}

std::vector<ClassBean>
DexItem::FindClass(
        const schema::FindClass *query,
        const std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    std::vector<uint32_t> find_result;
    auto try_match_class = [&](uint32_t i) {
        auto &class_def = this->reader.ClassDefs()[i];
        if (query->in_classes() && !in_class_set.contains(class_def.class_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[class_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (!IsClassMatched(class_def.class_idx, query->matcher())) return false;
        find_result.emplace_back(class_def.class_idx);
        return true;
    };

    if (query_context.IsEarlyExitEnabled()) {
        ScanFindRange<true>(start, end, query_context, try_match_class);
    } else {
        ScanFindRange<false>(start, end, query_context, try_match_class);
    }

    std::vector<ClassBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetClassBean(idx));
    }
    return result;
}

std::vector<MethodBean>
DexItem::FindMethod(
        const schema::FindMethod *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    std::vector<uint32_t> find_result;
    auto try_match_method = [&](uint32_t method_idx) {
        auto &method_def = this->reader.MethodIds()[method_idx];
        if (!this->type_def_flag[method_def.class_idx]) return false;
        if (query->in_classes() && !in_class_set.contains(method_def.class_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[method_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (query->in_methods() && !in_method_set.contains(method_idx)) return false;
        if (!IsMethodMatched(method_idx, query->matcher())) return false;
        find_result.emplace_back(method_idx);
        return true;
    };

    if (query_context.IsEarlyExitEnabled()) {
        ScanFindRange<true>(start, end, query_context, try_match_method);
    } else {
        ScanFindRange<false>(start, end, query_context, try_match_method);
    }

    std::vector<MethodBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetMethodBean(idx));
    }
    return result;
}

std::vector<FieldBean>
DexItem::FindField(
        const schema::FindField *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_field_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    std::vector<uint32_t> find_result;
    auto try_match_field = [&](uint32_t field_idx) {
        auto &field_def = this->reader.FieldIds()[field_idx];
        if (!this->type_def_flag[field_def.class_idx]) return false;
        if (query->in_classes() && !in_class_set.contains(field_def.class_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[field_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (query->in_fields() && !in_field_set.contains(field_idx)) return false;
        if (!IsFieldMatched(field_idx, query->matcher())) return false;
        find_result.emplace_back(field_idx);
        return true;
    };

    if (query_context.IsEarlyExitEnabled()) {
        ScanFindRange<true>(start, end, query_context, try_match_field);
    } else {
        ScanFindRange<false>(start, end, query_context, try_match_field);
    }

    std::vector<FieldBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetFieldBean(idx));
    }
    return result;
}

std::vector<ClassBean>
DexItem::FindClass(
        const schema::FindClass *query,
        const std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        uint32_t type_idx,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    if (query->in_classes() && !in_class_set.contains(type_idx)) {
        return {};
    }

    std::vector<uint32_t> find_result;
    if (query->search_packages() || query->exclude_packages()) {
        auto hit = packageTrie.search(this->type_names[type_idx], query->ignore_packages_case());
        if (query->exclude_packages() && (hit & 1)) return {};
        if (query->search_packages() && !(hit >> 1)) return {};
    }

    if (IsClassMatched(type_idx, query->matcher())) {
        find_result.emplace_back(type_idx);
        if (query_context.IsEarlyExitEnabled()) {
            (void) query_context.RequestEarlyExit();
        }
    }

    std::vector<ClassBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetClassBean(idx));
    }
    return result;
}

std::vector<MethodBean>
DexItem::FindMethod(
        const schema::FindMethod *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        uint32_t type_idx,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    if (query->in_classes() && !in_class_set.contains(type_idx)) {
        return {};
    }

    std::vector<uint32_t> find_result;
    auto try_match_method = [&](uint32_t method_idx) {
        auto &method_def = this->reader.MethodIds()[method_idx];
        if (query->in_methods() && !in_method_set.contains(method_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[method_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (!IsMethodMatched(method_idx, query->matcher())) return false;
        find_result.emplace_back(method_idx);
        return true;
    };
    if (query_context.IsEarlyExitEnabled()) {
        ScanFindItems<true>(this->class_method_ids[type_idx], query_context, try_match_method);
    } else {
        ScanFindItems<false>(this->class_method_ids[type_idx], query_context, try_match_method);
    }

    std::vector<MethodBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetMethodBean(idx));
    }
    return result;
}

std::vector<FieldBean>
DexItem::FindField(
        const schema::FindField *query,
        const std::set<uint32_t> &in_class_set,
        const std::set<uint32_t> &in_field_set,
        trie::PackageTrie &packageTrie,
        uint32_t type_idx,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    if (query->in_classes() && !in_class_set.contains(type_idx)) {
        return {};
    }

    std::vector<uint32_t> find_result;
    auto try_match_field = [&](uint32_t field_idx) {
        auto &field_def = this->reader.FieldIds()[field_idx];
        if (query->in_fields() && !in_field_set.contains(field_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[field_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (!IsFieldMatched(field_idx, query->matcher())) return false;
        find_result.emplace_back(field_idx);
        return true;
    };
    if (query_context.IsEarlyExitEnabled()) {
        ScanFindItems<true>(this->class_field_ids[type_idx], query_context, try_match_field);
    } else {
        ScanFindItems<false>(this->class_field_ids[type_idx], query_context, try_match_field);
    }

    std::vector<FieldBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetFieldBean(idx));
    }
    return result;
}

}
