#pragma once

#include <string_view>
#include <vector>

#include "parallel_hashmap/phmap.h"
#include "slicer/reader.h"

#include "beans.h"
#include "byte_code_util.h"
#include "constant.h"
#include "error.h"
#include "file_helper.h"

#include "schema/querys_generated.h"
#include "schema/results_generated.h"
#include "acdat/Builder.h"

namespace dexkit {

class DexItem {
public:

    explicit DexItem(uint32_t id, uint8_t *data, size_t size);

    explicit DexItem(uint32_t id, std::unique_ptr<MemMap> mmap);

    ~DexItem() = default;

    DexItem(DexItem &&) = default;

    DexItem &operator=(DexItem &&) = default;

    DexItem(const DexItem &) = delete;

    DexItem &operator=(const DexItem &) = delete;

    [[nodiscard]] MemMap *GetImage() const {
        return _image.get();
    }

    std::vector<ClassBean> FindClass(const schema::FindClass *query);

    std::vector<MethodBean> FindMethod(const schema::FindMethod *query);

    std::vector<FieldBean> FindField(const schema::FindField *query);

    std::vector<BatchFindClassItemBean> BatchFindClassUsingStrings(
            const schema::BatchFindClassUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    std::vector<BatchFindMethodItemBean> BatchFindMethodUsingStrings(
            const schema::BatchFindMethodUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    ClassBean GetClassBean(dex::u4 class_idx);

    MethodBean GetMethodBean(dex::u4 method_idx);

    FieldBean GetFieldBean(dex::u4 field_idx);

    AnnotationBean GetAnnotationBean(dex::u4 annotation_idx);

private:

    int InitCache();

    phmap::flat_hash_map<dex::u4, std::vector<std::string_view>>
    InitBatchFindStringsMap(
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    std::string_view GetMethodDescriptors(uint32_t method_idx);
    std::string_view GetFieldDescriptors(uint32_t field_idx);

private:
    std::unique_ptr<MemMap> _image;
    dex::Reader reader;

    uint32_t dex_flag = 0;
    uint32_t dex_id;

    // string constants, sorted by string value
    std::vector<std::string_view> strings;
    std::vector<std::string_view> type_names;
    phmap::flat_hash_map<std::string_view /*type_name*/, uint32_t /*type_id*/> type_ids_map;
    phmap::flat_hash_map<std::uint32_t /*type_idx*/, uint32_t /*class_id*/> type_id_class_id_map;
    // dex declared types flag
    std::vector<bool /*declared_flag*/> type_declared_flag;
    // class source file name, eg. "HelloWorld.java", maybe obfuscated
    std::vector<std::string_view> class_source_files;
    std::vector<uint32_t /*access_flag*/> class_access_flags;
    std::vector<std::optional<std::string>> method_descriptors;
    std::vector<std::vector<uint32_t /*method_id*/>> class_method_ids;
    std::vector<uint32_t /*access_flag*/> method_access_flags;
    std::vector<std::optional<std::string>> field_descriptors;
    std::vector<std::vector<uint32_t /*field_id*/>> class_field_ids;
    std::vector<uint32_t /*access_flag*/> field_access_flags;
    std::vector<const dex::Code *> method_codes;
    std::vector<const dex::TypeList *> proto_type_list;
    std::vector<std::optional<std::vector<uint8_t /*opcode*/>>> method_opcode_seq;
    std::vector<const ir::AnnotationsDirectory *> class_annotations;
};

} // namespace dexkit