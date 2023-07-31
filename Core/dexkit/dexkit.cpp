#include "include/dexkit.h"

namespace dexkit {

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    AddZipPath(apk_path, unzip_thread_num);
}

Error DexKit::AddDex(uint8_t *data, size_t size) {
    dex_items.emplace_back(std::make_unique<DexItem>(data, size));
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::unique_ptr<MemMap> dex_image) {
    dex_items.emplace_back(std::make_unique<DexItem>(std::move(dex_image)));
    return Error::SUCCESS;
}

Error DexKit::AddImage(std::vector<std::unique_ptr<MemMap>> dex_images) {
    for (auto &dex_image: dex_images) {
        dex_items.emplace_back(std::make_unique<DexItem>(std::move(dex_image)));
    }
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
            int idx = dex_pair.first - 1;
            dex_items[ort_size + idx] = std::make_unique<DexItem>(std::move(ptr));
        });
        futures.emplace_back(std::move(v));
    }
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

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindClass(const schema::FindClass *query) {
    std::vector<ClassBean> result;
    for (auto &dex_item: dex_items) {
        auto classes = dex_item->FindClass(query);
        result.insert(result.end(), classes.begin(), classes.end());
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean : result) {
        auto res = bean.CreateClassMeta(*builder);
        builder->Finish(res);
    }
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindMethod(const schema::FindMethod *query) {
    std::vector<MethodBean> result;
    for (auto &dex_item: dex_items) {
        auto methods = dex_item->FindMethod(query);
        result.insert(result.end(), methods.begin(), methods.end());
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean : result) {
        auto res = bean.CreateMethodMeta(*builder);
        builder->Finish(res);
    }
    return builder;
}

std::unique_ptr<flatbuffers::FlatBufferBuilder> DexKit::FindField(const schema::FindField *query) {
    std::vector<FieldBean> result;
    for (auto &dex_item: dex_items) {
        auto fields = dex_item->FindField(query);
        result.insert(result.end(), fields.begin(), fields.end());
    }
    auto builder = std::make_unique<flatbuffers::FlatBufferBuilder>();
    for (auto &bean : result) {
        auto res = bean.CreateFieldMeta(*builder);
        builder->Finish(res);
    }
    return builder;
}

} // namespace dexkit