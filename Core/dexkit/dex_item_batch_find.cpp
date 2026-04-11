// DexKit - An high-performance runtime parsing library for deximplemented in C++
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

template<typename MatchFunc>
bool MatchUsingStringGroup(
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *matchers,
        const std::vector<std::string_view> &using_strings,
        MatchFunc &&match_func
) {
    if (matchers == nullptr) {
        return true;
    }
    for (int i = 0; i < matchers->size(); ++i) {
        auto string_matcher = matchers->Get(i);
        bool matched = false;
        for (auto str: using_strings) {
            if (match_func(str, string_matcher)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            return false;
        }
    }
    return true;
}

} // namespace

std::vector<BatchFindClassItemBean>
DexItem::BatchFindClassUsingStrings(
        const schema::BatchFindClassUsingStrings *query,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        std::map<std::string_view, std::set<std::string_view>> &keywords_map,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
        std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    std::map<std::string_view, std::vector<uint32_t>> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (class_method_ids[type_idx].empty()) continue;
        if (query->in_classes() && in_class_set.contains(type_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[type_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }

        if (keywords_map.empty()) {
            std::vector<std::string_view> using_strings;
            for (auto method_idx: class_method_ids[type_idx]) {
                auto &method_using_strings = method_using_string_ids[method_idx];
                using_strings.reserve(using_strings.size() + method_using_strings.size());
                for (auto string_idx: method_using_strings) {
                    using_strings.emplace_back(this->strings[string_idx]);
                }
            }
            for (int i = 0; i < query->matchers()->size(); ++i) {
                auto matcher = query->matchers()->Get(i);
                if (MatchUsingStringGroup(matcher->using_strings(), using_strings, [this](std::string_view str, const schema::StringMatcher *string_matcher) {
                    return this->IsStringMatched(str, string_matcher);
                })) {
                    find_result[matcher->union_key()->string_view()].emplace_back(type_idx);
                }
            }
            continue;
        }

        auto using_empty_string_count = 0;
        std::set<std::string_view> search_set;
        for (auto method_idx: class_method_ids[type_idx]) {
            for (auto string_idx: method_using_string_ids[method_idx]) {
                if (string_idx == this->empty_string_id) ++using_empty_string_count;
                auto str = this->strings[string_idx];
                auto hits = acTrie.ParseText(str);
                for (auto &hit: hits) {
                    auto match_type = match_type_map[hit.value];
                    bool match;
                    switch (match_type) {
                        case schema::StringMatchType::Contains: match = true; break;
                        case schema::StringMatchType::StartWith: match = (hit.begin == 0); break;
                        case schema::StringMatchType::EndWith: match = (hit.end == str.size()); break;
                        case schema::StringMatchType::Equal: match = (hit.begin == 0 && hit.end == str.size()); break;
                        case schema::StringMatchType::SimilarRegex: abort();
                    }
                    if (match) {
                        search_set.emplace(hit.value);
                    }
                }
            }
        }
        if (match_type_map.contains("")) {
            DEXKIT_CHECK(match_type_map[""] == schema::StringMatchType::Equal);
            if (using_empty_string_count) {
                search_set.emplace("");
            }
        }
        if (search_set.empty()) continue;

        for (auto &[key, matched_set]: keywords_map) {
            std::vector<std::string_view> vec;
            std::set_intersection(search_set.begin(), search_set.end(),
                                  matched_set.begin(), matched_set.end(),
                                  std::inserter(vec, vec.begin()));
            if (vec.size() == matched_set.size()) {
                find_result[key].emplace_back(type_idx);
            }
        }
    }

    std::vector<BatchFindClassItemBean> result;
    for (auto &[key, values]: find_result) {
        std::vector<ClassBean> classes;
        for (auto id: values) {
            classes.emplace_back(this->GetClassBean(id));
        }
        BatchFindClassItemBean itemBean;
        itemBean.union_key = key;
        itemBean.classes = classes;
        result.emplace_back(itemBean);
    }

    return result;
}

std::vector<BatchFindMethodItemBean>
DexItem::BatchFindMethodUsingStrings(
        const schema::BatchFindMethodUsingStrings *query,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        std::map<std::string_view, std::set<std::string_view>> &keywords_map,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
        std::set<uint32_t> &in_class_set,
        std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        QueryContext &query_context
) {
    auto query_binding = query_context.BindToCurrentThread();

    std::map<std::string_view, std::vector<uint32_t>> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (class_method_ids[type_idx].empty()) continue;
        if (query->in_classes() && in_class_set.contains(type_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[type_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }

        for (auto method_idx: class_method_ids[type_idx]) {
            if (query->in_methods() && in_method_set.contains(method_idx)) continue;
            auto code = this->method_codes[method_idx];
            if (code == nullptr) continue;

            if (keywords_map.empty()) {
                std::vector<std::string_view> using_strings;
                auto &using_string_ids = method_using_string_ids[method_idx];
                using_strings.reserve(using_string_ids.size());
                for (auto string_idx: using_string_ids) {
                    using_strings.emplace_back(this->strings[string_idx]);
                }
                for (int i = 0; i < query->matchers()->size(); ++i) {
                    auto matcher = query->matchers()->Get(i);
                    if (MatchUsingStringGroup(matcher->using_strings(), using_strings, [this](std::string_view str, const schema::StringMatcher *string_matcher) {
                        return this->IsStringMatched(str, string_matcher);
                    })) {
                        find_result[matcher->union_key()->string_view()].emplace_back(method_idx);
                    }
                }
                continue;
            }

            auto using_empty_string_count = 0;
            std::set<std::string_view> search_set;
            for (auto string_idx: method_using_string_ids[method_idx]) {
                if (string_idx == this->empty_string_id) ++using_empty_string_count;
                auto str = this->strings[string_idx];
                auto hits = acTrie.ParseText(str);
                for (auto &hit: hits) {
                    auto match_type = match_type_map[hit.value];
                    bool match;
                    switch (match_type) {
                        case schema::StringMatchType::Contains: match = true; break;
                        case schema::StringMatchType::StartWith: match = (hit.begin == 0); break;
                        case schema::StringMatchType::EndWith: match = (hit.end == str.size()); break;
                        case schema::StringMatchType::Equal: match = (hit.begin == 0 && hit.end == str.size()); break;
                        case schema::StringMatchType::SimilarRegex: abort();
                    }
                    if (match) {
                        search_set.emplace(hit.value);
                    }
                }
            }
            if (match_type_map.contains("")) {
                DEXKIT_CHECK(match_type_map[""] == schema::StringMatchType::Equal);
                if (using_empty_string_count) {
                    search_set.emplace("");
                }
            }
            if (search_set.empty()) continue;

            for (auto &[key, matched_set]: keywords_map) {
                std::vector<std::string_view> vec;
                std::set_intersection(search_set.begin(), search_set.end(),
                                      matched_set.begin(), matched_set.end(),
                                      std::inserter(vec, vec.begin()));
                if (vec.size() == matched_set.size()) {
                    find_result[key].emplace_back(method_idx);
                }
            }
        }
    }

    std::vector<BatchFindMethodItemBean> result;
    for (auto &[key, values]: find_result) {
        std::vector<MethodBean> methods;
        for (auto id: values) {
            methods.emplace_back(this->GetMethodBean(id));
        }
        BatchFindMethodItemBean itemBean;
        itemBean.union_key = key;
        itemBean.methods = methods;
        result.emplace_back(itemBean);
    }
    return result;
}

}
