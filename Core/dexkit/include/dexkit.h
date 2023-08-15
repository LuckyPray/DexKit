#pragma once

#include <string_view>
#include <thread>
#include <vector>
#include <atomic>

#include "flatbuffers/flatbuffers.h"
#include "file_helper.h"
#include "error.h"
#include "dex_item.h"

namespace dexkit {

class DexKit {
public:

    explicit DexKit() = default;
    explicit DexKit(std::string_view apk_path, int unzip_thread_num = 0);
    ~DexKit() = default;

    void SetThreadNum(int num);
    Error AddDex(uint8_t *data, size_t size);
    Error AddImage(std::unique_ptr<MemMap> dex_image);
    Error AddImage(std::vector<std::unique_ptr<MemMap>> dex_images);
    Error AddZipPath(std::string_view apk_path, int unzip_thread_num = 0);
    Error ExportDexFile(std::string_view path);
    [[nodiscard]] int GetDexNum() const;

    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindClass(const schema::FindClass *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindMethod(const schema::FindMethod *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FindField(const schema::FindField *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> BatchFindClassUsingStrings(const schema::BatchFindClassUsingStrings *query);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query);

private:
    std::mutex _mutex;
    std::atomic<uint32_t> dex_cnt = 0;
    uint32_t _thread_num = std::thread::hardware_concurrency();
    std::vector<std::unique_ptr<DexItem>> dex_items;

    static std::map<std::string_view, std::set<std::string_view>>
    BuildBatchFindKeywordsMap(
            const flatbuffers::Vector<flatbuffers::Offset<dexkit::schema::BatchUsingStringsMatcher>> *matchers,
            std::vector<std::pair<std::string_view, bool>> &keywords,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );
};

} // namespace dexkit