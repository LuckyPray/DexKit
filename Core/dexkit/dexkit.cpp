#include "include/dexkit.h"

#include "ThreadPool.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"

namespace dexkit {

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    if (unzip_thread_num > 0) {
        _thread_num = unzip_thread_num;
    }
    std::lock_guard<std::mutex> lock(_mutex);
    AddZipPath(apk_path, unzip_thread_num);
    std::sort(dex_items.begin(), dex_items.end());
}

void DexKit::SetThreadNum(int num) {
    _thread_num = num;
}

Error DexKit::AddDex(uint8_t *data, size_t size) {
    std::lock_guard<std::mutex> lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, data, size));
    std::sort(dex_items.begin(), dex_items.end());
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::unique_ptr<MemMap> dex_image) {
    std::lock_guard<std::mutex> lock(_mutex);
    dex_items.emplace_back(std::make_unique<DexItem>(dex_cnt++, std::move(dex_image)));
    std::sort(dex_items.begin(), dex_items.end());
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
    ThreadPool pool(unzip_thread_num == 0 ? _thread_num : unzip_thread_num);
    std::vector<std::future<void>> futures;
    for (auto &dex_pair: dex_pairs) {
        auto v = pool.enqueue([this, &dex_pair, ort_size]() {
            auto dex_image = dex_pair.second->uncompress();
            auto ptr = std::make_unique<MemMap>(std::move(dex_image));
            if (!ptr->ok()) {
                return;
            }
            int idx = ort_size + dex_pair.first - 1;
            dex_items[idx] = std::make_unique<DexItem>(idx, std::move(ptr));
        });
        futures.emplace_back(std::move(v));
    }
    dex_cnt += (uint32_t) dex_pairs.size();
    for (auto &f: futures) {
        f.get();
    }
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

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindClass(const schema::FindClass *query) {
    std::map<uint32_t, std::set<uint32_t>> dex_class_map;
    if (query->in_classes()) {
        for (auto encode_idx: *query->in_classes()) {
            dex_class_map[encode_idx >> 32].insert(encode_idx & UINT32_MAX);
        }
    }

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<ClassBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &class_set]() {
            return dex_item->FindClass(query, class_set);
        }));
    }

    std::vector<ClassBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean: result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
    }
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindMethod(const schema::FindMethod *query) {
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

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<MethodBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &method_set = dex_method_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([this, &dex_item, &query, &class_set, &method_set]() {
            return dex_item->FindMethod(query, class_set, method_set);
        }));
    }

    std::vector<MethodBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean: result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
    }
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindField(const schema::FindField *query) {
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

    ThreadPool pool(_thread_num);
    std::vector<std::future<std::vector<FieldBean>>> futures;
    for (auto &dex_item: dex_items) {
        auto &class_set = dex_class_map[dex_item->GetDexId()];
        auto &field_set = dex_field_map[dex_item->GetDexId()];
        futures.push_back(pool.enqueue([&dex_item, &query, &class_set, &field_set]() {
            return dex_item->FindField(query, class_set, field_set);
        }));
    }

    std::vector<FieldBean> result;
    for (auto &f: futures) {
        auto vec = f.get();
        result.insert(result.end(), vec.begin(), vec.end());
    }

    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean: result) {
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
    }
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
        futures.push_back(pool.enqueue([&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_map]() {
            return dex_item->BatchFindClassUsingStrings(query, acTrie, keywords_map, match_type_map, class_map);
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
        futures.push_back(pool.enqueue([&dex_item, &query, &acTrie, &keywords_map, &match_type_map, &class_set, &method_set]() {
            return dex_item->BatchFindMethodUsingStrings(query, acTrie, keywords_map, match_type_map, class_set, method_set);
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