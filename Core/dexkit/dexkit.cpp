#include "include/dexkit.h"

#include "ThreadPool.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"

namespace dexkit {

bool comp(std::unique_ptr<DexItem> &a, std::unique_ptr<DexItem> &b) {
    return a->GetDexId() < b->GetDexId();
}

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    if (unzip_thread_num > 0) {
        _thread_num = unzip_thread_num;
    }
    std::lock_guard<std::mutex> lock(_mutex);
    AddZipPath(apk_path, unzip_thread_num);
    std::sort(dex_items.begin(), dex_items.end(), comp);
}

void DexKit::SetThreadNum(int num) {
    _thread_num = num;
}

Error DexKit::AddDex(uint8_t *data, size_t size) {
    std::lock_guard<std::mutex> lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, data, size));
    std::sort(dex_items.begin(), dex_items.end(), comp);
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::unique_ptr<MemMap> dex_image) {
    std::lock_guard<std::mutex> lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, std::move(dex_image)));
    std::sort(dex_items.begin(), dex_items.end(), comp);
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::vector<std::unique_ptr<MemMap>> dex_images) {
    std::lock_guard<std::mutex> lock(_mutex);
    for (auto &dex_image: dex_images) {
        dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, std::move(dex_image)));
    }
    std::sort(dex_items.begin(), dex_items.end());
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
    int ort_size = (int) dex_items.size();
    int new_size = (int) (ort_size + dex_pairs.size());
    dex_items.resize(new_size);
    {
        ThreadPool pool(unzip_thread_num == 0 ? _thread_num : unzip_thread_num);
        for (auto &dex_pair: dex_pairs) {
            pool.enqueue([this, &dex_pair, ort_size]() {
                auto dex_image = dex_pair.second->uncompress();
                auto ptr = std::make_unique<MemMap>(std::move(dex_image));
                if (!ptr->ok()) {
                    return;
                }
                int idx = ort_size + dex_pair.first - 1;
                dex_items[idx] = std::make_unique<DexItem>(idx, std::move(ptr));
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
        fwrite(image, 1, image->len(), fp);
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
    auto resolve_types = ExtractUseTypeNames(query->matcher());

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<ClassBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &class_set, &resolve_types, &packageTrie]() -> std::vector<ClassBean> {
            if (dex_item->CheckAllTypeNamesDeclared(resolve_types)) {
                return dex_item->FindClass(query, class_set, packageTrie);
            }
            return {};
        }));
    }

    std::vector<ClassBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
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
    auto resolve_types = ExtractUseTypeNames(query->matcher());

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<MethodBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &method_set = dex_method_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &class_set, &method_set, &resolve_types, &packageTrie]() -> std::vector<MethodBean> {
            if (dex_item->CheckAllTypeNamesDeclared(resolve_types)) {
                return dex_item->FindMethod(query, class_set, method_set, packageTrie);
            }
            return {};
        }));
    }

    std::vector<MethodBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
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
    auto resolve_types = ExtractUseTypeNames(query->matcher());

    trie::PackageTrie packageTrie;
    // build package match trie
    BuildPackagesMatchTrie(query->search_packages(), query->exclude_packages(), query->ignore_packages_case(), packageTrie);

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<FieldBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &field_set = dex_field_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &class_set, &field_set, &resolve_types, &packageTrie]() -> std::vector<FieldBean> {
            if (dex_item->CheckAllTypeNamesDeclared(resolve_types)) {
                return dex_item->FindField(query, class_set, field_set, packageTrie);
            }
            return {};
        }));
    }

    std::vector<FieldBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
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

std::unique_ptr<flatbuffers::FlatBufferBuilder>
DexKit::FieldGetMethods(int64_t encode_field_id) {
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

void
DexKit::BuildPackagesMatchTrie(
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