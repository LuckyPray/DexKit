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

    std::string search_package;
    if (query->search_package()) {
        search_package = query->search_package()->string_view();
        std::replace(search_package.begin(), search_package.end(), '.', '/');
        if (search_package[0] != 'L') {
            search_package = "L" + search_package;
        }
    }

    std::map<std::string_view, std::vector<uint32_t>> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        auto class_name = type_names[type_idx];
        if (class_method_ids[type_idx].empty()) {
            continue;
        }
        if (query->search_package() && !class_name.starts_with(search_package)) {
            continue;
        }
        std::set<std::string_view> search_set;
        for (auto method_idx: class_method_ids[type_idx]) {
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
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    auto strings_map = InitBatchFindStringsMap(acTrie, match_type_map);

    if (strings_map.empty()) {
        return {};
    }

    // TODO: check query->in_class

    std::string search_package;
    if (query->search_package()) {
        search_package = query->search_package()->string_view();
        std::replace(search_package.begin(), search_package.end(), '.', '/');
        if (search_package[0] != 'L') {
            search_package = "L" + search_package;
        }
    }

    std::map<std::string_view, std::vector<uint32_t>> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        auto class_name = type_names[type_idx];
        if (class_method_ids[type_idx].empty()) {
            continue;
        }
        if (query->search_package() && !class_name.starts_with(search_package)) {
            continue;
        }
        for (auto method_idx: class_method_ids[type_idx]) {
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