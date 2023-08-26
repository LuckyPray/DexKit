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
    for (auto &class_def: this->reader.ClassDefs()) {
        if (query->in_classes() && !in_class_set.contains(class_def.class_idx)) continue;
        if (query->search_package() && !type_names[class_def.class_idx].starts_with(search_package)) continue;

        if (IsClassMatched(class_def.class_idx, query->matcher())) {
            find_result.emplace_back(class_def.class_idx);
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
    auto index = 0;
    for (auto &method_def: this->reader.MethodIds()) {
        auto method_idx = index++;
        if (query->in_classes() && !in_class_set.contains(method_def.class_idx)) continue;
        if (query->search_package() && !type_names[method_def.class_idx].starts_with(search_package)) continue;
        if (query->in_methods() && !in_method_set.contains(method_idx)) continue;

        if (IsMethodMatched(method_idx, query->matcher())) {
            find_result.emplace_back(method_idx);
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
    auto index = 0;
    for (auto &field_def: this->reader.FieldIds()) {
        auto field_idx = index++;
        auto field_class_type_idx = field_def.class_idx;
        if (query->in_classes() && !in_class_set.contains(field_class_type_idx)) continue;
        if (query->search_package() && !type_names[field_class_type_idx].starts_with(search_package)) continue;
        if (query->in_fields() && !in_field_set.contains(field_idx)) continue;

        if (IsFieldMatched(field_idx, query->matcher())) {
            find_result.emplace_back(field_idx);
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