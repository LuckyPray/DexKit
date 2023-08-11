#include "dex_item.h"

namespace dexkit {

std::vector<ClassBean>
DexItem::FindClass(const schema::FindClass *query, std::set<uint32_t> &in_class_set) {

    std::string search_package;
    if (query->search_package()) {
        search_package = query->search_package()->string_view();
        std::replace(search_package.begin(), search_package.end(), '.', '/');
        if (search_package[0] != 'L') {
            search_package = "L" + search_package;
        }
    }

    std::vector<uint32_t> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (query->in_classes() && !in_class_set.contains(type_idx)) {
            continue;
        }
        if (class_method_ids[type_idx].empty()) {
            continue;
        }
        auto class_name = type_names[type_idx];
        if (query->search_package() && !class_name.starts_with(search_package)) {
            continue;
        }
        if (IsClassMatched(type_idx, query->matcher())) {
            find_result.emplace_back(type_idx);
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
DexItem::FindMethod(const schema::FindMethod *query, std::set<uint32_t> &in_class_set, std::set<uint32_t> &in_method_set) {

    std::string search_package;
    if (query->search_package()) {
        search_package = query->search_package()->string_view();
        std::replace(search_package.begin(), search_package.end(), '.', '/');
        if (search_package[0] != 'L') {
            search_package = "L" + search_package;
        }
    }

    std::vector<uint32_t> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (query->in_classes() && !in_class_set.contains(type_idx)) {
            continue;
        }
        if (class_method_ids[type_idx].empty()) {
            continue;
        }
        auto class_name = type_names[type_idx];
        if (query->search_package() && !class_name.starts_with(search_package)) {
            continue;
        }
        for (auto method_idx: class_method_ids[type_idx]) {
            if (query->in_methods() && !in_method_set.contains(type_idx)) {
                continue;
            }
            if (IsMethodMatched(method_idx, query->matcher())) {
                find_result.emplace_back(type_idx);
            }
        }
    }


    std::vector<MethodBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetMethodBean(idx));
    }
    return result;
}

std::vector<FieldBean>
DexItem::FindField(const schema::FindField *query, std::set<uint32_t> &in_class_set, std::set<uint32_t> &in_field_set) {

    std::string search_package;
    if (query->search_package()) {
        search_package = query->search_package()->string_view();
        std::replace(search_package.begin(), search_package.end(), '.', '/');
        if (search_package[0] != 'L') {
            search_package = "L" + search_package;
        }
    }

    std::vector<uint32_t> find_result;
    for (int type_idx = 0; type_idx < this->type_names.size(); ++type_idx) {
        if (query->in_classes() && !in_class_set.contains(type_idx)) {
            continue;
        }
        if (class_method_ids[type_idx].empty()) {
            continue;
        }
        auto class_name = type_names[type_idx];
        if (query->search_package() && !class_name.starts_with(search_package)) {
            continue;
        }
        for (auto field_idx: class_field_ids[type_idx]) {
            if (query->in_fields() && !in_field_set.contains(field_idx)) {
                continue;
            }
            if (IsFieldMatched(field_idx, query->matcher())) {
                find_result.emplace_back(type_idx);
            }
        }
    }

    std::vector<FieldBean> result;
    result.reserve(find_result.size());
    for (auto idx: find_result) {
        result.emplace_back(GetFieldBean(idx));
    }
    return result;
}

}