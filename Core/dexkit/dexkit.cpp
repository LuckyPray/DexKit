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

#include "include/dexkit.h"

#include "ThreadPool.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"

#define WRITE_FILE_BLOCK_SIZE 1024 * 1024

namespace dexkit {

bool comp(std::unique_ptr<DexItem> &a, std::unique_ptr<DexItem> &b) {
    return a->GetDexId() < b->GetDexId();
}

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    if (unzip_thread_num > 0) {
        _thread_num = unzip_thread_num;
    }
    std::lock_guard lock(_mutex);
    AddZipPath(apk_path, unzip_thread_num);
    std::sort(dex_items.begin(), dex_items.end(), comp);
}

void DexKit::SetThreadNum(int num) {
    _thread_num = num;
}

Error DexKit::InitFullCache() {
    InitDexCache(UINT32_MAX);
    return Error::SUCCESS;
}

Error DexKit::AddDex(uint8_t *data, size_t size) {
    std::lock_guard lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, data, size, this));
    std::sort(dex_items.begin(), dex_items.end(), comp);
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::unique_ptr<MemMap> dex_image) {
    std::lock_guard lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, std::move(dex_image), this));
    std::sort(dex_items.begin(), dex_items.end(), comp);
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::vector<std::unique_ptr<MemMap>> dex_images) {
    std::lock_guard lock(_mutex);
    auto old_size = dex_items.size();
    auto new_size = old_size + dex_images.size();
    dex_items.resize(new_size);
    {
        ThreadPool pool(_thread_num);
        auto index = old_size;
        for (auto &dex_image: dex_images) {
            pool.enqueue([this, &dex_image, index]() {
                dex_items[index] = std::make_unique<DexItem>(index, std::move(dex_image), this);
            });
            index++;
        }
    }
    std::sort(dex_items.begin(), dex_items.end(), comp);
    return Error::SUCCESS;
}

Error DexKit::AddZipPath(std::string_view apk_path, int unzip_thread_num) {
    auto map = MemMap(apk_path);
    if (!map.ok()) {
        return Error::FILE_NOT_FOUND;
    }
    auto zip_file = ZipFile::Open(map);
    if (!zip_file) return Error::OPEN_ZIP_FILE_FAILED;
    std::vector<std::pair<int, ZipLocalFile *>> dex_pairs;
    for (int idx = 1;; ++idx) {
        auto entry_name = "classes" + (idx == 1 ? std::string() : std::to_string(idx)) + ".dex";
        auto entry = zip_file->Find(entry_name);
        if (!entry) {
            break;
        }
        dex_pairs.emplace_back(idx, entry);
    }
    int old_size = (int) dex_items.size();
    int new_size = (int) (old_size + dex_pairs.size());
    dex_items.resize(new_size);
    {
        ThreadPool pool(unzip_thread_num == 0 ? _thread_num : unzip_thread_num);
        for (auto &dex_pair: dex_pairs) {
            pool.enqueue([this, &dex_pair, old_size]() {
                auto dex_image = dex_pair.second->uncompress();
                auto ptr = std::make_unique<MemMap>(std::move(dex_image));
                if (!ptr->ok()) {
                    return;
                }
                int idx = old_size + dex_pair.first - 1;
                dex_items[idx] = std::make_unique<DexItem>(idx, std::move(ptr), this);
            });
        }
    }
    dex_cnt += (uint32_t) dex_pairs.size();
    return Error::SUCCESS;
}

Error DexKit::ExportDexFile(std::string_view path) {
    for (auto &dex_item: dex_items) {
        auto image = dex_item->GetImage();
        std::string file_name(path);
        if (file_name.back() != '/') {
            file_name += '/';
        }
        file_name += "classes_" + std::to_string(image->len()) + ".dex";
        // write file
        FILE *fp = fopen(file_name.c_str(), "wb");
        if (fp == nullptr) {
            return Error::OPEN_FILE_FAILED;
        }
        int len = (int) image->len();
        int offset = 0;
        while (offset < len) {
            int size = std::min(WRITE_FILE_BLOCK_SIZE, len - offset);
            size_t write_size = fwrite(image->addr() + offset, 1, size, fp);
            if (write_size != size) {
                fclose(fp);
                return Error::WRITE_FILE_INCOMPLETE;
            }
            offset += size;
            fflush(fp);
        }
        fclose(fp);
    }
    return Error::SUCCESS;
}

int DexKit::GetDexNum() const {
    return (int) dex_items.size();
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindClass(const schema::FindClass *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    InitDexCache(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    std::vector<ClassBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher()) {
        auto class_name = query->matcher()->class_name();
        if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
            auto [dex, type_idx] = GetClassDeclaredPair(class_name->value()->string_view());
            if (dex) {
                fast_search_dex = dex;
                auto res = dex->FindClass(query, packageTrie, type_idx);
                result.insert(result.end(), res.begin(), res.end());
            }
        }
    }

    if (fast_search_dex == nullptr) {
        bool find_fist_flag = false;
        ThreadPool pool(_thread_num);
        std::vector<std::future<std::vector<ClassBean>>> futures;
        for (auto &dex_item: dex_items) {
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            if (dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindClass(query, class_set, packageTrie, pool, BATCH_SIZE / 2, find_fist_flag);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }

        for (auto &f: futures) {
            auto vec = f.get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (query->find_first()) {
                pool.skip_unexec_tasks();
                break;
            }
        }
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::ClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateClassMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindMethod(const schema::FindMethod *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_method_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_methods()) {
        for (auto encode_idx: *query->in_methods()) {
            dex_method_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    InitDexCache(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    std::vector<MethodBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher()) {
        auto declaring_class = query->matcher()->declaring_class();
        if (declaring_class) {
            auto class_name = declaring_class->class_name();
            if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
                auto [dex, type_idx] = GetClassDeclaredPair(class_name->value()->string_view());
                if (dex) {
                    fast_search_dex = dex;
                    auto res = dex->FindMethod(query, packageTrie, type_idx);
                    result.insert(result.end(), res.begin(), res.end());
                }
            }
        }
    }

    if (fast_search_dex == nullptr) {
        bool find_fist_flag = false;
        ThreadPool pool(_thread_num);
        std::vector<std::future<std::vector<MethodBean>>> futures;
        for (auto &dex_item: dex_items) {
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            auto &method_set = dex_method_map[dex_item->GetDexId()];
            if (dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindMethod(query, class_set, method_set, packageTrie, pool, BATCH_SIZE, find_fist_flag);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }

        for (auto &f: futures) {
            auto vec = f.get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (query->find_first()) {
                pool.skip_unexec_tasks();
                break;
            }
        }
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    std::set<std::string_view> declared_set;
    for (auto &bean: result) {
        if (declared_set.contains(bean.dex_descriptor)) {
            continue;
        }
        declared_set.emplace(bean.dex_descriptor);
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FindField(const schema::FindField *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_field_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_fields()) {
        for (auto encode_idx: *query->in_fields()) {
            dex_field_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    auto analyze_ret = Analyze(query->matcher(), 1);
    InitDexCache(analyze_ret.need_flags);

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    std::vector<FieldBean> result;

    // fast search declared class
    DexItem *fast_search_dex = nullptr;
    if (query->matcher()) {
        auto declaring_class = query->matcher()->declaring_class();
        if (declaring_class) {
            auto class_name = declaring_class->class_name();
            if (class_name && class_name->match_type() == schema::StringMatchType::Equal && !class_name->ignore_case()) {
                auto [dex, type_idx] = GetClassDeclaredPair(class_name->value()->string_view());
                if (dex) {
                    fast_search_dex = dex;
                    auto res = dex->FindField(query, packageTrie, type_idx);
                    result.insert(result.end(), res.begin(), res.end());
                }
            }
        }
    }

    if (fast_search_dex == nullptr) {
        bool find_fist_flag = false;
        ThreadPool pool(_thread_num);
        std::vector<std::future<std::vector<FieldBean>>> futures;
        for (auto &dex_item: dex_items) {
            auto &class_set = dex_class_map[dex_item->GetDexId()];
            auto &field_set = dex_field_map[dex_item->GetDexId()];
            if (dex_item->CheckAllTypeNamesDeclared(analyze_ret.declare_class)) {
                auto res = dex_item->FindField(query, class_set, field_set, packageTrie, pool, BATCH_SIZE, find_fist_flag);
                for (auto &f: res) {
                    futures.emplace_back(std::move(f));
                }
            }
        }

        for (auto &f: futures) {
            auto vec = f.get();
            if (vec.empty()) continue;
            result.insert(result.end(), vec.begin(), vec.end());
            if (query->find_first()) {
                pool.skip_unexec_tasks();
                break;
            }
        }
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::FieldMeta>> offsets;
    std::set<std::string_view> declared_set;
    for (auto &bean: result) {
        if (declared_set.contains(bean.dex_descriptor)) {
            continue;
        }
        declared_set.emplace(bean.dex_descriptor);
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::BatchFindClassUsingStrings(const schema::BatchFindClassUsingStrings *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    // build keywords trie
    std::vector<std::pair<std::string_view, bool>> keywords;
    phmap::flat_hash_map<std::string_view, schema::StringMatchType> match_type_map;
    auto keywords_map = BuildBatchFindKeywordsMap(query->matchers(), keywords, match_type_map);
    acdat::AhoCorasickDoubleArrayTrie<std::string_view> acTrie;
    acdat::Builder<std::string_view>().Build(keywords, &acTrie);

    std::map<std::string_view, std::vector<ClassBean>> find_result_map;
    // init find_result_map keys
    for (int i = 0; i < query->matchers()->size(); ++i) {
        auto matchers = query->matchers();
        for (int j = 0; j < matchers->size(); ++j) {
            find_result_map[matchers->Get(j)->union_key()->string_view()] = {};
        }
    }

    InitDexCache(kUsingString);

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<BatchFindClassItemBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_map = dex_class_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_map, &packageTrie]() {
            return dex_item->BatchFindClassUsingStrings(query, acTrie, keywords_map, match_type_map, class_map, packageTrie);
        }));
    }

    // fetch and merge result
    for (auto &f: futures) {
        auto items = f.get();
        for (auto &item: items) {
            auto &beans = find_result_map[item.union_key];
            beans.insert(beans.end(), item.classes.begin(), item.classes.end());
        }
    }

    std::vector<BatchFindClassItemBean> result;
    for (auto &[key, value]: find_result_map) {
        BatchFindClassItemBean bean;
        bean.union_key = key;
        bean.classes = value;
        result.emplace_back(bean);
    }

    auto fbb = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::BatchClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateBatchClassMeta(*fbb);
        fbb->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateBatchClassMetaArrayHolder(*fbb, fbb->CreateVector(offsets));
    fbb->Finish(array_holder);
    return fbb;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    std::map<uint32_t, std::set<uint32_t>> dex_method_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }
    if (query->in_methods()) {
        for (auto encode_idx: *query->in_methods()) {
            dex_method_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    // build keywords trie
    std::vector<std::pair<std::string_view, bool>> keywords;
    phmap::flat_hash_map<std::string_view, schema::StringMatchType> match_type_map;
    auto keywords_map = BuildBatchFindKeywordsMap(query->matchers(), keywords, match_type_map);
    acdat::AhoCorasickDoubleArrayTrie<std::string_view> acTrie;
    acdat::Builder<std::string_view>().Build(keywords, &acTrie);

    std::map<std::string_view, std::vector<MethodBean>> find_result_map;
    // init find_result_map keys
    for (int i = 0; i < query->matchers()->size(); ++i) {
        auto matchers = query->matchers();
        for (int j = 0; j < matchers->size(); ++j) {
            find_result_map[matchers->Get(j)->union_key()->string_view()] = {};
        }
    }

    InitDexCache(kUsingString);

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<BatchFindMethodItemBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &method_set = dex_method_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_set, &method_set, &packageTrie]() {
            return dex_item->BatchFindMethodUsingStrings(query, acTrie, keywords_map, match_type_map, class_set, method_set, packageTrie);
        }));
    }

    // fetch and merge result
    for (auto &f: futures) {
        auto items = f.get();
        for (auto &item: items) {
            auto &beans = find_result_map[item.union_key];
            beans.insert(beans.end(), item.methods.begin(), item.methods.end());
        }
    }

    std::vector<BatchFindMethodItemBean> result;
    for (auto &[key, value]: find_result_map) {
        BatchFindMethodItemBean bean;
        bean.union_key = key;
        bean.methods = value;
        result.emplace_back(bean);
    }

    auto fbb = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::BatchMethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateBatchMethodMeta(*fbb);
        fbb->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateBatchMethodMetaArrayHolder(*fbb, fbb->CreateVector(offsets));
    fbb->Finish(array_holder);
    return fbb;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassData(const std::string_view descriptor) {
    auto [dex, type_id] = this->GetClassDeclaredPair(descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetClassBean(type_id);
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean.CreateClassMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodData(const std::string_view descriptor) {
    auto class_descriptor = descriptor.substr(0, descriptor.find("->"));
    auto [dex, type_id] = this->GetClassDeclaredPair(class_descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetMethodBean(type_id, descriptor);
    if (!bean.has_value()) {
        return nullptr;
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean->CreateMethodMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldData(const std::string_view descriptor) {
    auto class_descriptor = descriptor.substr(0, descriptor.find("->"));
    auto [dex, type_id] = this->GetClassDeclaredPair(class_descriptor);
    if (dex == nullptr) {
        return nullptr;
    }
    auto bean = dex->GetFieldBean(type_id, descriptor);
    if (!bean.has_value()) {
        return nullptr;
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    auto res = bean->CreateFieldMeta(*builder);
    builder->Finish(res);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassByIds(const std::vector<int64_t> &encode_ids) {
    std::vector<ClassBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto class_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetClassBean(class_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::ClassMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateClassMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodByIds(const std::vector<int64_t> &encode_ids) {
    std::vector<MethodBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto method_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetMethodBean(method_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldByIds(const std::vector<int64_t> &encode_ids) {
    std::vector<FieldBean> result;
    for (auto encode_id: encode_ids) {
        auto dex_id = encode_id >> 32;
        auto field_id = encode_id & UINT32_MAX;
        result.emplace_back(dex_items[dex_id]->GetFieldBean(field_id));
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::FieldMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetClassAnnotations(int64_t encode_class_id) {
    auto dex_id = encode_class_id >> 32;
    auto class_id = encode_class_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetClassAnnotationBeans(class_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetFieldAnnotations(int64_t encode_field_id) {
    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetFieldAnnotationBeans(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetMethodAnnotations(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetMethodAnnotationBeans(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateAnnotationMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetParameterAnnotations(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetParameterAnnotationBeans(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::AnnotationMetaArrayHolder>> holder_offsets;
    for (auto &param_annotations: result) {
        std::vector<flatbuffers::Offset<schema::AnnotationMeta>> meta_offsets;
        for (auto &annotation: param_annotations) {
            auto res = annotation.CreateAnnotationMeta(*builder);
            builder->Finish(res);
            meta_offsets.emplace_back(res);
        }
        auto array_holder = schema::CreateAnnotationMetaArrayHolder(*builder, builder->CreateVector(meta_offsets));
        builder->Finish(array_holder);
        holder_offsets.emplace_back(array_holder);
    }
    auto array_holder = schema::CreateParametersAnnotationMetaArrayHoler(*builder, builder->CreateVector(holder_offsets));
    builder->Finish(array_holder);
    return builder;
}

std::optional<std::vector<std::optional<std::string_view>>>
DexKit::GetParameterNames(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetParameterNames(method_id);
}

std::vector<uint8_t>
DexKit::GetMethodOpCodes(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetMethodOpCodes(method_id);
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetCallMethods(int64_t encode_method_id) {
    InitDexCache(kCallerMethod | kMethodInvoking);

    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetCallMethods(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetInvokeMethods(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetInvokeMethods(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::vector<std::string_view>
DexKit::GetUsingStrings(int64_t encode_method_id) {
    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    return dex_items[dex_id]->GetUsingStrings(method_id);
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::GetUsingFields(int64_t encode_method_id) {
    InitDexCache(kMethodUsingField);

    auto dex_id = encode_method_id >> 32;
    auto method_id = encode_method_id & UINT32_MAX;
    auto result = dex_items[dex_id]->GetUsingFields(method_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::UsingFieldMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateUsingFieldMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateUsingFieldMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FieldGetMethods(int64_t encode_field_id) {
    InitDexCache(kRwFieldMethod | kMethodUsingField);

    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->FieldGetMethods(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FieldPutMethods(int64_t encode_field_id) {
    InitDexCache(kRwFieldMethod | kMethodUsingField);

    auto dex_id = encode_field_id >> 32;
    auto field_id = encode_field_id & UINT32_MAX;
    auto result = dex_items[dex_id]->FieldPutMethods(field_id);

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    std::vector<flatbuffers::Offset<schema::MethodMeta>> offsets;
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
        offsets.emplace_back(res);
    }
    auto array_holder = schema::CreateMethodMetaArrayHolder(*builder, builder->CreateVector(offsets));
    builder->Finish(array_holder);
    return builder;
}

std::pair<DexItem *, uint32_t> DexKit::GetClassDeclaredPair(std::string_view class_name) {
    auto find = this->class_declare_dex_map.find(class_name);
    if (find == this->class_declare_dex_map.end()) {
        return {nullptr, 0};
    }
    auto class_info = find->second;
    return {this->dex_items[class_info.first].get(), class_info.second};
}

DexItem *DexKit::GetDexItem(uint16_t dex_id) {
    return this->dex_items[dex_id].get();
}

void DexKit::PutDeclaredClass(std::string_view class_name, uint16_t dex_id, uint32_t type_idx) {
    std::lock_guard lock(this->_put_class_mutex);
    this->class_declare_dex_map[class_name] = {dex_id, type_idx};
}

void DexKit::InitDexCache(uint32_t init_flags) {
    static std::mutex init_mutex;
    std::lock_guard lock(init_mutex);

    bool need_init = false;
    bool need_put_cross_ref = false;
    uint32_t cross_ref_flags = init_flags & (kCallerMethod | kRwFieldMethod);
    for (auto &dex_item: dex_items) {
        if (dex_item->NeedInitCache(init_flags)) {
            need_init = true;
        }
        if (dex_item->NeedPutCrossRef(cross_ref_flags)) {
            need_put_cross_ref = true;
        }
    }

    if (need_init) {
        ThreadPool pool(std::min((int) _thread_num, (int) dex_items.size()));
        for (auto &dex_item: dex_items) {
            pool.enqueue([&init_flags, &dex_item]() {
                dex_item->InitCache(init_flags);
            });
        }
    }
    if (need_put_cross_ref) {
        ThreadPool pool(std::min((int) _thread_num, (int) dex_items.size()));
        for (auto &dex_item: dex_items) {
            pool.enqueue([&cross_ref_flags, &dex_item]() {
                dex_item->PutCrossRef(cross_ref_flags);
            });
        }
    }
}

void DexKit::BuildPackagesMatchTrie(
        const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *search_packages,
        const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *exclude_packages,
        const bool ignore_packages_case,
        trie::PackageTrie &trie
) {
    std::vector<std::string> packages;
    if (search_packages) {
        for (auto i = 0; i < search_packages->size(); ++i) {
            auto package = search_packages->Get(i);
            std::string package_str(package->string_view());
            std::replace(package_str.begin(), package_str.end(), '.', '/');
            if (package_str[0] != 'L') {
                package_str = "L" + package_str; // NOLINT
            }
            trie.insert(package_str, true, ignore_packages_case);
            packages.emplace_back(std::move(package_str));
        }
    }
    if (exclude_packages) {
        for (auto i = 0; i < exclude_packages->size(); ++i) {
            auto package = exclude_packages->Get(i);
            std::string package_str(package->string_view());
            std::replace(package_str.begin(), package_str.end(), '.', '/');
            if (package_str[0] != 'L') {
                package_str = "L" + package_str; // NOLINT
            }
            trie.insert(package_str, false, ignore_packages_case);
            packages.emplace_back(std::move(package_str));
        }
    }
}

std::map<std::string_view, std::set<std::string_view>>
DexKit::BuildBatchFindKeywordsMap(
        const flatbuffers::Vector<flatbuffers::Offset<dexkit::schema::BatchUsingStringsMatcher>> *matchers,
        std::vector<std::pair<std::string_view, bool>> &keywords,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    std::map<std::string_view, std::set<std::string_view>> keywords_map;
    for (int i = 0; i < matchers->size(); ++i) {
        auto matcher = matchers->Get(i);
        auto union_key = matcher->union_key()->string_view();
        for (int j = 0; j < matcher->using_strings()->size(); ++j) {
            auto string_matcher = matcher->using_strings()->Get(j);
            auto value = string_matcher->value()->string_view();
            auto type = string_matcher->match_type();
            auto ignore_case = string_matcher->ignore_case();
            if (type == schema::StringMatchType::SimilarRegex) {
                type = schema::StringMatchType::Contains;
                int l = 0, r = (int) value.size();
                if (value.starts_with('^')) {
                    l = 1;
                    type = schema::StringMatchType::StartWith;
                }
                if (value.ends_with('$')) {
                    r = (int) value.size() - 1;
                    if (type == schema::StringMatchType::StartWith) {
                        type = schema::StringMatchType::Equal;
                    } else {
                        type = schema::StringMatchType::EndWith;
                    }
                }
                value = value.substr(l, r - l);
            }
            keywords_map[union_key].insert(value);
            keywords.emplace_back(value, ignore_case);
            match_type_map[value] = type;
        }
    }
    return keywords_map;
}

} // namespace dexkit