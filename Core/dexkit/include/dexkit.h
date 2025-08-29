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

#pragma once

#include <string_view>
#include <thread>
#include <vector>
#include <atomic>

#include "flatbuffers/flatbuffers.h"
#include "zip_archive.h"
#include "dexkit_error.h"
#include "dex_item.h"
#include "package_trie.h"
#include "analyze.h"

#define BATCH_SIZE 5000

namespace dexkit {

class DexItem;

class DexKit {
public:

    explicit DexKit() = default;
    explicit DexKit(std::string_view apk_path, int unzip_thread_num = 0);
    ~DexKit() = default;

    void SetThreadNum(int num);
    Error InitFullCache();
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

    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassData(std::string_view descriptor);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodData(std::string_view descriptor);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldData(std::string_view descriptor);

    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldByIds(const std::vector<int64_t> &encode_ids);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetClassAnnotations(int64_t encode_class_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetFieldAnnotations(int64_t encode_field_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetMethodAnnotations(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetParameterAnnotations(int64_t encode_method_id);
    std::optional<std::vector<std::optional<std::string_view>>> GetParameterNames(int64_t encode_method_id);
    std::vector<uint8_t> GetMethodOpCodes(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetCallMethods(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetInvokeMethods(int64_t encode_method_id);
    std::vector<std::string_view> GetUsingStrings(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> GetUsingFields(int64_t encode_method_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldGetMethods(int64_t encode_field_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldPutMethods(int64_t encode_field_id);

    std::pair<DexItem *, uint32_t> GetClassDeclaredPair(std::string_view class_name);
    DexItem *GetDexItem(uint16_t dex_id);
    void PutDeclaredClass(std::string_view class_name, uint16_t dex_id, uint32_t type_idx);

private:
    std::mutex _mutex;
    std::shared_mutex _put_class_mutex;
    std::atomic<uint32_t> dex_cnt = 0;
    uint32_t _thread_num = std::thread::hardware_concurrency();
    std::vector<std::unique_ptr<DexItem>> dex_items;
    phmap::flat_hash_map<std::string_view, std::pair<uint16_t /*dex_id*/, uint32_t /*type_idx*/>> class_declare_dex_map;

    void InitDexCache(uint32_t init_flags);

    static void BuildPackagesMatchTrie(
            const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *search_packages,
            const flatbuffers::Vector<flatbuffers::Offset<flatbuffers::String>> *exclude_packages,
            bool ignore_package_case,
            trie::PackageTrie &trie
    );

    static std::map<std::string_view, std::set<std::string_view>>
    BuildBatchFindKeywordsMap(
            const flatbuffers::Vector<flatbuffers::Offset<dexkit::schema::BatchUsingStringsMatcher>> *matchers,
            std::vector<std::pair<std::string_view, bool>> &keywords,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );
};

} // namespace dexkit