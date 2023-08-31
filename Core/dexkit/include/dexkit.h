#pragma once

#include <string_view>
#include <thread>
#include <vector>
#include <atomic>

#include "flatbuffers/flatbuffers.h"
#include "file_helper.h"
#include "error.h"
#include "dex_item.h"
#include "package_trie.h"

namespace dexkit {

class DexItem;

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
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldGetMethods(int64_t encode_field_id);
    std::unique_ptr<flatbuffers::FlatBufferBuilder> FieldPutMethods(int64_t encode_field_id);

    DexItem *GetClassDeclaredDexItem(std::string_view class_name);
    void PutDeclaredClass(std::string_view class_name, uint16_t dex_id);

private:
    std::mutex _mutex;
    std::shared_mutex _put_class_mutex;
    std::atomic<uint32_t> dex_cnt = 0;
    uint32_t _thread_num = std::thread::hardware_concurrency();
    std::vector<std::unique_ptr<DexItem>> dex_items;
    phmap::flat_hash_map<std::string_view, uint16_t /*dex_id*/> class_declare_dex_map;

    static void
    BuildPackagesMatchTrie(
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

    std::vector<std::string_view> ExtractUseTypeNames(const schema::ClassMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::InterfacesMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::AnnotationsMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::AnnotationMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::AnnotationElementsMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::AnnotationElementMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::AnnotationEncodeArrayMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::FieldsMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::FieldMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::MethodsMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::MethodMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::ParametersMatcher *matcher);
    std::vector<std::string_view> ExtractUseTypeNames(const schema::ParameterMatcher *matcher);
};

} // namespace dexkit