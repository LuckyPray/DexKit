#include "dex_item.h"

namespace dexkit {

std::vector<std::future<std::vector<ClassBean>>>
DexItem::FindClass(
        const schema::FindClass *query,
        std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        ThreadPool &pool,
        uint32_t slice_size
) {
    std::vector<std::future<std::vector<ClassBean>>> futures;
    uint32_t split_count;
    if (slice_size > 0) {
        split_count = (this->reader.ClassDefs().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.ClassDefs().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        futures.emplace_back(pool.enqueue(
                [this, query, &in_class_set, &packageTrie, i, slice_size] {
                    return FindClass(query, in_class_set, packageTrie, i * slice_size, std::min((i + 1) * slice_size, (uint32_t) this->reader.ClassDefs().size()));
                }
        ));
    }
    return futures;
}

std::vector<std::future<std::vector<MethodBean>>>
DexItem::FindMethod(
        const schema::FindMethod *query,
        std::set<uint32_t> &in_class_set,
        std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        ThreadPool &pool,
        uint32_t slice_size
) {
    std::vector<std::future<std::vector<MethodBean>>> futures;
    uint32_t split_count;
    if (slice_size > 0) {
        split_count = (this->reader.MethodIds().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.MethodIds().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        futures.emplace_back(pool.enqueue(
                [this, query, &in_class_set, &in_method_set, &packageTrie, i, slice_size] {
                    return FindMethod(query, in_class_set, in_method_set, packageTrie, i * slice_size, std::min((i + 1) * slice_size, (uint32_t) this->reader.MethodIds().size()));
                }
        ));
    }
    return futures;
}

std::vector<std::future<std::vector<FieldBean>>>
DexItem::FindField(
        const schema::FindField *query,
        std::set<uint32_t> &in_class_set,
        std::set<uint32_t> &in_field_set,
        trie::PackageTrie &packageTrie,
        ThreadPool &pool,
        uint32_t slice_size
) {
    std::vector<std::future<std::vector<FieldBean>>> futures;
    uint32_t split_count;
    if (slice_size > 0) {
        split_count = (this->reader.FieldIds().size() + slice_size - 1) / slice_size;
    } else {
        split_count = 1;
        slice_size = this->reader.FieldIds().size();
    }
    futures.reserve(split_count);
    for (auto i = 0; i < split_count; ++i) {
        futures.emplace_back(pool.enqueue(
                [this, query, &in_class_set, &in_field_set, &packageTrie, i, slice_size] {
                    return FindField(query, in_class_set, in_field_set, packageTrie, i * slice_size, std::min((i + 1) * slice_size, (uint32_t) this->reader.FieldIds().size()));
                }
        ));
    }
    return futures;
}

std::vector<ClassBean>
DexItem::FindClass(
        const schema::FindClass *query,
        std::set<uint32_t> &in_class_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end
) {

    std::vector<uint32_t> find_result;
    for (auto i = start; i < end; ++i) {
        auto &class_def = this->reader.ClassDefs()[i];
        if (query->in_classes() && !in_class_set.contains(class_def.class_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[class_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }

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
DexItem::FindMethod(
        const schema::FindMethod *query,
        std::set<uint32_t> &in_class_set,
        std::set<uint32_t> &in_method_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end
) {

    std::vector<uint32_t> find_result;
    for (auto method_idx = start; method_idx < end; ++method_idx) {
        auto &method_def = this->reader.MethodIds()[method_idx];
        if (query->in_classes() && !in_class_set.contains(method_def.class_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[method_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }
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
DexItem::FindField(
        const schema::FindField *query,
        std::set<uint32_t> &in_class_set,
        std::set<uint32_t> &in_field_set,
        trie::PackageTrie &packageTrie,
        uint32_t start,
        uint32_t end
) {

    std::vector<uint32_t> find_result;
    auto index = 0;
    for (auto field_idx = start; field_idx < end; ++field_idx) {
        auto &field_def = this->reader.FieldIds()[field_idx];
        if (query->in_classes() && !in_class_set.contains(field_def.class_idx)) continue;
        if (query->search_packages() || query->exclude_packages()) {
            auto hit = packageTrie.search(this->type_names[field_def.class_idx], query->ignore_packages_case());
            if (query->exclude_packages() && (hit & 1)) continue;
            if (query->search_packages() && !(hit >> 1)) continue;
        }
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