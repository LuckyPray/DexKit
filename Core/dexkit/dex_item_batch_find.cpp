#include "dex_item.h"

namespace dexkit {

std::vector<BatchFindClassItemBean>
DexItem::BatchFindClassUsingStrings(
        const schema::BatchFindClassUsingStrings *query,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        std::map<std::string_view, std::set<std::string_view>> &keywords_map,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
        std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie
) {

    std::map<std::string_view, std::vector<uint32_t>> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (class_method_ids[type_idx].empty()) continue;
        if (query->in_classes() && in_class_set.contains(type_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[type_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }

        std::set<std::string_view> search_set;
        for (auto method_idx: class_method_ids[type_idx]) {
            for (auto string_idx: method_using_string_ids[method_idx]) {
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
        trie::PackageTrie &packageTrie
) {

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

            std::set<std::string_view> search_set;
            for (auto string_idx: method_using_string_ids[method_idx]) {
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