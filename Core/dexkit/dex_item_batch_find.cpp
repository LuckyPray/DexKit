#include "dex_item.h"

namespace dexkit {

phmap::flat_hash_map<uint32_t, std::vector<std::string_view>>
DexItem::InitBatchFindStringsMap(
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    phmap::flat_hash_map<uint32_t, std::vector<std::string_view>> strings_map;

    for (auto i = 0; i < this->strings.size(); ++i) {
        std::string_view string = this->strings[i];
        auto hits = acTrie.ParseText(string);
        for (auto &hit: hits) {
            auto match_type = match_type_map[hit.value];
            bool match = false;
            switch (match_type) {
                case schema::StringMatchType::Contains:
                    match = true;
                    break;
                case schema::StringMatchType::StartWith:
                    if (hit.begin == 0) {
                        match = true;
                    }
                    break;
                case schema::StringMatchType::EndWith:
                    if (hit.end == string.size()) {
                        match = true;
                    }
                    break;
                case schema::StringMatchType::Equal:
                    if (hit.begin == 0 && hit.end == string.size()) {
                        match = true;
                    }
                    break;
                default:
                    break;
            }
            if (match) {
                if (strings_map.contains(i)) {
                    strings_map[i].emplace_back(hit.value);
                } else {
                    strings_map[i] = {hit.value};
                }
            }
        }
    }
    return strings_map;
}

std::vector<BatchFindClassItemBean>
DexItem::BatchFindClassUsingStrings(
        const schema::BatchFindClassUsingStrings *query,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        std::map<std::string_view, std::set<std::string_view>> &keywords_map,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    auto strings_map = InitBatchFindStringsMap(acTrie, match_type_map);

    if (strings_map.empty()) {
        return {};
    }

    // TODO: check query->in_class

    std::optional<std::string_view> find_package;
    schema::StringMatchType package_match_type;
    auto find_package_name = query->find_package_name();
    if (find_package_name) {
        // TODO: matcher
        find_package = find_package_name->value()->string_view();
        package_match_type = find_package_name->type();
    }

    std::map<std::string_view, std::vector<dex::u4>> find_result;
    for (int class_idx = 0; class_idx < this->type_names.size(); ++class_idx) {
        auto class_name = type_names[class_idx];
        if (class_method_ids[class_idx].empty()) {
            continue;
        }
        std::set<std::string_view> search_set;
        for (auto method_idx: class_method_ids[class_idx]) {
            auto code = this->method_codes[method_idx];
            if (code == nullptr) {
                continue;
            }
            auto p = code->insns;
            auto end_p = p + code->insns_size;
            while (p < end_p) {
                auto op = *p & 0xff;
                auto ptr = p;
                auto width = GetBytecodeWidth(ptr++);
                switch (op) {
                    case 0x1a: {
                        auto string_idx = ReadShort(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string: strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    case 0x1b: {
                        auto string_idx = ReadInt(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string: strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
                p += width;
            }
        }
        if (search_set.empty()) continue;
        for (auto &[key, matched_set]: keywords_map) {
            std::vector<std::string_view> vec;
            std::set_intersection(search_set.begin(), search_set.end(),
                                  matched_set.begin(), matched_set.end(),
                                  std::inserter(vec, vec.begin()));
            if (vec.size() == matched_set.size()) {
                find_result[key].emplace_back(class_idx);
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
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    auto strings_map = InitBatchFindStringsMap(acTrie, match_type_map);

    if (strings_map.empty()) {
        return {};
    }

    // TODO: check query->in_class

    std::optional<std::string_view> find_package;
    schema::StringMatchType package_match_type;
    auto find_package_name = query->find_package_name();
    if (find_package_name) {
        // TODO: matcher
        find_package = find_package_name->value()->string_view();
        package_match_type = find_package_name->type();
    }

    std::map<std::string_view, std::vector<dex::u4>> find_result;
    for (int class_idx = 0; class_idx < this->type_names.size(); ++class_idx) {
        auto class_name = type_names[class_idx];
        if (class_method_ids[class_idx].empty()) {
            continue;
        }
        for (auto method_idx: class_method_ids[class_idx]) {
            std::set<std::string_view> search_set;
            auto code = this->method_codes[method_idx];
            if (code == nullptr) {
                continue;
            }
            auto p = code->insns;
            auto end_p = p + code->insns_size;
            while (p < end_p) {
                auto op = *p & 0xff;
                auto ptr = p;
                auto width = GetBytecodeWidth(ptr++);
                switch (op) {
                    case 0x1a: {
                        auto string_idx = ReadShort(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string: strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    case 0x1b: {
                        auto string_idx = ReadInt(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string: strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
                p += width;
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