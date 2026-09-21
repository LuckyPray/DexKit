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
#include "internal/using_strings_prefilter.h"

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
    inverted_string::QueryPlan string_plan;
    if (!should_stop_submission && query->matcher())
        string_plan = PlanRootStringCandidates(query->matcher()->using_strings(), true);
    if (string_plan.Admitted()) slice_size = 0;
    if (slice_size > 0) {
        split_count = (this->reader.ClassDefs().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.ClassDefs().size();
    }
    if (string_plan.route == inverted_string::QueryPlan::Route::Empty) split_count = 0;
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        if (should_stop_submission && executor.ShouldSkipTask()) break;
        query_context.MarkTaskSubmitted();
        futures.emplace_back(SubmitQueryTask(executor,
                [this, query, &in_class_set, &packageTrie, i, slice_size, &query_context, string_plan] {
                    auto task_scope = query_context.TrackTaskExecution();
                    auto result = FindClass(query, in_class_set, packageTrie, i * slice_size,
                                            std::min((i + 1) * slice_size, (uint32_t) this->reader.ClassDefs().size()),
                                            query_context, string_plan);
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
    inverted_string::QueryPlan string_plan;
    if (!should_stop_submission && query->matcher())
        string_plan = PlanRootStringCandidates(query->matcher()->using_strings(), false);
    if (string_plan.Admitted()) slice_size = 0;
    if (slice_size > 0) {
        split_count = (this->reader.MethodIds().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.MethodIds().size();
    }
    if (string_plan.route == inverted_string::QueryPlan::Route::Empty) split_count = 0;
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        if (should_stop_submission && executor.ShouldSkipTask()) break;
        query_context.MarkTaskSubmitted();
        futures.emplace_back(SubmitQueryTask(executor,
                [this, query, &in_class_set, &in_method_set, &packageTrie, i, slice_size, &query_context, string_plan] {
                    auto task_scope = query_context.TrackTaskExecution();
                    auto result = FindMethod(query, in_class_set, in_method_set, packageTrie, i * slice_size,
                                             std::min((i + 1) * slice_size, (uint32_t) this->reader.MethodIds().size()),
                                             query_context, string_plan);
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
        QueryContext &query_context,
        const inverted_string::QueryPlan &string_plan
) {
    auto query_binding = query_context.BindToCurrentThread();
    DEXKIT_CHECK(!string_plan.Admitted() || (start == 0 && end == reader.ClassDefs().size()));
    if (string_plan.route == inverted_string::QueryPlan::Route::Empty) return {};
    auto *prefilter_plan = internal::GetClassUsingStringsPrefilterPlan(query->matcher(), query_context);

    std::vector<uint32_t> find_result;
    auto in_scope = [&](uint32_t i) {
        const auto &class_def = this->reader.ClassDefs()[i];
        if (query->in_classes() && !in_class_set.contains(class_def.class_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[class_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        return true;
    };
    auto try_match_class = [&](uint32_t i) {
        if (!in_scope(i)) return false;
        const auto &class_def = this->reader.ClassDefs()[i];
        if (prefilter_plan && !MayMatchClassUsingStringsPrefilter(class_def.class_idx, *prefilter_plan)) return false;
        if (!IsClassMatched(class_def.class_idx, query->matcher())) return false;
        find_result.emplace_back(class_def.class_idx);
        return true;
    };

    const bool filter_strings = string_plan.route == inverted_string::QueryPlan::Route::Keywords
            && (query->in_classes() || query->search_packages() || query->exclude_packages());
    bool local_strings = filter_strings && string_plan.index_ready && inverted_string_bytes != SIZE_MAX;
    std::vector<uint32_t> filtered_classes;
    if (filter_strings) {
        size_t bytes = 0;
        for (auto i = start; i < end; ++i) {
            if (!in_scope(i)) continue;
            filtered_classes.push_back(i);
            if (local_strings) {
                for (auto method : class_method_ids[reader.ClassDefs()[i].class_idx]) {
                    if (!AccumulateMethodStringBytes(method, bytes)) {
                        local_strings = false;
                        break;
                    }
                }
            }
        }
        if (filtered_classes.empty()) return {};
    }
    inverted_string::Bits candidates;
    const bool inverted = !local_strings && string_plan.Admitted()
            && BuildRootStringCandidates(query->matcher()->using_strings(), true, string_plan, candidates);
    const void *proof_matchers = query->matcher() ? query->matcher()->using_strings() : nullptr;
    const auto *proof = inverted && string_plan.proves_all_strings ? &candidates : nullptr;
    inverted_string::MatchScope scope(this, proof_matchers, true, proof);
    if (filter_strings) {
        for (auto i : filtered_classes) {
            if (!inverted || candidates.Has(reader.ClassDefs()[i].class_idx)) try_match_class(i);
        }
    } else if (inverted) {
        std::vector<uint32_t> definitions;
        candidates.Each([&](uint32_t type) {
            if (type_def_flag[type]) definitions.push_back(type_def_idx[type]);
        });
        // ClassDefs order need not equal type-ID order.
        std::sort(definitions.begin(), definitions.end());
        ScanFindItems<false>(definitions, query_context, try_match_class);
    } else if (query_context.IsEarlyExitEnabled()) {
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
        QueryContext &query_context,
        const inverted_string::QueryPlan &string_plan
) {
    auto query_binding = query_context.BindToCurrentThread();
    DEXKIT_CHECK(!string_plan.Admitted() || (start == 0 && end == reader.MethodIds().size()));
    if (string_plan.route == inverted_string::QueryPlan::Route::Empty) return {};
    auto *prefilter_plan = internal::GetMethodUsingStringsPrefilterPlan(query->matcher(), query_context);

    std::vector<uint32_t> find_result;
    auto in_scope = [&](uint32_t method_idx) {
        const auto &method_def = this->reader.MethodIds()[method_idx];
        if (!this->type_def_flag[method_def.class_idx]) return false;
        if (query->in_classes() && !in_class_set.contains(method_def.class_idx)) return false;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[method_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) return false;
            if (query->search_packages() && !(hit >> 1)) return false;
        }
        if (query->in_methods() && !in_method_set.contains(method_idx)) return false;
        return true;
    };
    auto try_match_method = [&](uint32_t method_idx) {
        if (!in_scope(method_idx)) return false;
        if (prefilter_plan && !MayMatchMethodUsingStringsPrefilter(method_idx, *prefilter_plan)) return false;
        if (!IsMethodMatched(method_idx, query->matcher())) return false;
        find_result.emplace_back(method_idx);
        return true;
    };

    const auto *name_matcher = query->matcher() ? query->matcher()->method_name() : nullptr;
    const bool filter_name = name_matcher && name_matcher->value()
            && name_matcher->match_type() == schema::StringMatchType::Equal;
    const bool has_scope = query->in_classes() || query->in_methods()
            || query->search_packages() || query->exclude_packages();
    const bool filter_strings = string_plan.route == inverted_string::QueryPlan::Route::Keywords
            && (filter_name || has_scope);
    bool local_strings = filter_strings && string_plan.index_ready && inverted_string_bytes != SIZE_MAX;
    std::vector<uint32_t> filtered_methods;
    if (filter_strings) {
        size_t bytes = 0;
        for (auto id = start; id < end; ++id) {
            const auto &method = reader.MethodIds()[id];
            if (has_scope ? !in_scope(id) : !type_def_flag[method.class_idx]) continue;
            if (filter_name && !IsStringMatched(strings[method.name_idx], name_matcher)) continue;
            filtered_methods.push_back(id);
            if (local_strings) local_strings = AccumulateMethodStringBytes(id, bytes);
        }
        if (filtered_methods.empty()) return {};
    }
    inverted_string::Bits candidates;
    const bool inverted = !local_strings && string_plan.Admitted()
            && BuildRootStringCandidates(query->matcher()->using_strings(), false, string_plan, candidates);
    const void *proof_matchers = query->matcher() ? query->matcher()->using_strings() : nullptr;
    const auto *proof = inverted && string_plan.proves_all_strings ? &candidates : nullptr;
    inverted_string::MatchScope scope(this, proof_matchers, false, proof);
    if (filter_strings) {
        for (auto id : filtered_methods) if (!inverted || candidates.Has(id)) try_match_method(id);
    } else if (inverted) {
        // Root results admit only locally defined owners. Cross-reference
        // bindings are populated only for undefined owners; nested matches
        // still resolve them through IsMethodMatched without this scope.
        candidates.Each(try_match_method);
    } else if (query_context.IsEarlyExitEnabled()) {
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
    auto *prefilter_plan = internal::GetClassUsingStringsPrefilterPlan(query->matcher(), query_context);

    if (query->in_classes() && !in_class_set.contains(type_idx)) {
        return {};
    }

    std::vector<uint32_t> find_result;
    if (query->search_packages() || query->exclude_packages()) {
        auto hit = packageTrie.search(this->type_names[type_idx], query->ignore_packages_case());
        if (query->exclude_packages() && (hit & 1)) return {};
        if (query->search_packages() && !(hit >> 1)) return {};
    }

    if (prefilter_plan && !MayMatchClassUsingStringsPrefilter(type_idx, *prefilter_plan)) {
        return {};
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
    auto *prefilter_plan = internal::GetMethodUsingStringsPrefilterPlan(query->matcher(), query_context);

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
        if (prefilter_plan && !MayMatchMethodUsingStringsPrefilter(method_idx, *prefilter_plan)) return false;
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
        ScanFindItems<true>(GetClassFieldIds(type_idx), query_context, try_match_field);
    } else {
        ScanFindItems<false>(GetClassFieldIds(type_idx), query_context, try_match_field);
    }

    std::vector<FieldBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetFieldBean(idx));
    }
    return result;
}

}
