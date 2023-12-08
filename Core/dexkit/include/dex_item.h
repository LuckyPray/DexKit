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
#include <vector>

#include "beans.h"
#include "common.h"
#include "constant.h"
#include "dexkit_error.h"
#include "string_match.h"

#include "parallel_hashmap/phmap.h"
#include "slicer/reader.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"
#include "acdat/Builder.h"
#include "ThreadVariable.h"
#include "ThreadPool.h"
#include "file_helper.h"
#include "package_trie.h"
#include "dexkit.h"
#include "analyze.h"

namespace dexkit {

class DexKit;

class DexItem {
public:

    explicit DexItem(uint32_t id, uint8_t *data, size_t size, DexKit *dexkit);
    explicit DexItem(uint32_t id, std::unique_ptr<MemMap> mmap, DexKit *dexkit);
    ~DexItem() = default;
    DexItem(DexItem &&) = default;
    DexItem &operator=(DexItem &&) = default;
    DexItem(const DexItem &) = delete;
    DexItem &operator=(const DexItem &) = delete;

    [[nodiscard]] MemMap *GetImage() const {
        return _image.get();
    }
    [[nodiscard]] uint32_t GetDexId() const {
        return dex_id;
    }

    std::vector<std::future<std::vector<ClassBean>>>
    FindClass(
            const schema::FindClass *query,
            const std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie,
            ThreadPool &pool,
            uint32_t split_num,
            bool &find_fist_flag
    );
    std::vector<std::future<std::vector<MethodBean>>>
    FindMethod(
            const schema::FindMethod *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            ThreadPool &pool,
            uint32_t split_num,
            bool &find_fist_flag
    );
    std::vector<std::future<std::vector<FieldBean>>>
    FindField(
            const schema::FindField *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_field_set,
            trie::PackageTrie &packageTrie,
            ThreadPool &pool,
            uint32_t split_num,
            bool &find_fist_flag
    );
    std::vector<ClassBean> FindClass(
            const schema::FindClass *query,
            const std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            bool &find_fist_flag
    );
    std::vector<MethodBean> FindMethod(
            const schema::FindMethod *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            bool &find_fist_flag
    );
    std::vector<FieldBean> FindField(
            const schema::FindField *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_field_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            bool &find_fist_flag
    );
    std::vector<ClassBean> FindClass(
            const schema::FindClass *query,
            trie::PackageTrie &packageTrie,
            uint32_t class_idx
    );
    std::vector<MethodBean> FindMethod(
            const schema::FindMethod *query,
            trie::PackageTrie &packageTrie,
            uint32_t class_idx
    );
    std::vector<FieldBean> FindField(
            const schema::FindField *query,
            trie::PackageTrie &packageTrie,
            uint32_t class_idx
    );
    std::vector<BatchFindClassItemBean> BatchFindClassUsingStrings(
            const schema::BatchFindClassUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie
    );
    std::vector<BatchFindMethodItemBean> BatchFindMethodUsingStrings(
            const schema::BatchFindMethodUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set,
            std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie
    );

    ClassBean GetClassBean(uint32_t type_idx);
    MethodBean GetMethodBean(uint32_t method_idx);
    FieldBean GetFieldBean(uint32_t field_idx);

    std::optional<MethodBean> GetMethodBean(uint32_t type_idx, std::string_view method_descriptor);
    std::optional<FieldBean> GetFieldBean(uint32_t type_idx, std::string_view method_descriptor);

    AnnotationBean GetAnnotationBean(ir::Annotation *annotation);
    AnnotationEncodeValueBean GetAnnotationEncodeValueBean(ir::EncodedValue *encoded_value);
    AnnotationElementBean GetAnnotationElementBean(ir::AnnotationElement *annotation_element);
    AnnotationEncodeArrayBean GetAnnotationEncodeArrayBean(ir::EncodedArray *encoded_array);

    std::vector<AnnotationBean> GetClassAnnotationBeans(uint32_t class_idx);
    std::vector<AnnotationBean> GetMethodAnnotationBeans(uint32_t method_idx);
    std::vector<AnnotationBean> GetFieldAnnotationBeans(uint32_t field_idx);
    std::vector<std::vector<AnnotationBean>> GetParameterAnnotationBeans(uint32_t method_idx);
    std::optional<std::vector<std::optional<std::string_view>>> GetParameterNames(uint32_t method_idx);
    std::vector<uint8_t> GetMethodOpCodes(uint32_t method_idx);
    std::vector<MethodBean> GetCallMethods(uint32_t method_idx);
    std::vector<MethodBean> GetInvokeMethods(uint32_t method_idx);
    std::vector<std::string_view> GetUsingStrings(uint32_t method_idx);
    std::vector<UsingFieldBean> GetUsingFields(uint32_t method_idx);
    std::vector<MethodBean> FieldGetMethods(uint32_t field_idx);
    std::vector<MethodBean> FieldPutMethods(uint32_t field_idx);

    bool CheckAllTypeNamesDeclared(std::vector<std::string_view> &types);
    [[nodiscard]] bool NeedPutCrossRef(uint32_t need_cross_flag) const;
    void PutCrossRef(uint32_t put_cross_flag);
    [[nodiscard]] bool NeedInitCache(uint32_t need_flag) const;
    void InitCache(uint32_t init_flags);

private:

    void InitBaseCache();
    inline std::mutex &GetTypeDefMutex(uint32_t type_idx);

    std::string_view GetMethodDescriptor(uint32_t method_idx);
    std::string_view GetFieldDescriptor(uint32_t field_idx);

    std::vector<uint8_t> GetOpSeqFromCode(uint32_t method_idx);
    std::vector<uint32_t> GetUsingStringsFromCode(uint32_t method_idx);
    std::vector<uint32_t> GetInvokeMethodsFromCode(uint32_t method_idx);
    std::vector<EncodeNumber> GetUsingNumbersFromCode(uint32_t method_idx);

    static bool IsStringMatched(std::string_view str, const schema::StringMatcher *matcher);
    static bool IsAccessFlagsMatched(uint32_t access_flags, const schema::AccessFlagsMatcher *matcher);
    static std::set<std::string_view> BuildBatchFindKeywordsMap(
            const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
            std::vector<std::pair<std::string_view, bool>> &keywords,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    bool IsAnnotationMatched(const ir::Annotation *annotation, const schema::AnnotationMatcher *matcher);
    bool IsAnnotationUsingStringsMatched(const ir::Annotation *annotation, const schema::AnnotationMatcher *matcher);
    bool IsAnnotationsMatched(const ir::AnnotationSet *annotationSet, const schema::AnnotationsMatcher *matcher);
    bool IsAnnotationEncodeValueMatched(const ir::EncodedValue *encodedValue, schema::AnnotationEncodeValueMatcher type, const void *value);
    bool IsAnnotationEncodeArrayMatcher(const std::vector<ir::EncodedValue *> &encodedValues, const dexkit::schema::AnnotationEncodeArrayMatcher *matcher);
    bool IsAnnotationElementMatched(const ir::AnnotationElement *annotationElement, const schema::AnnotationElementMatcher *matcher);
    bool IsAnnotationElementsMatched(const std::vector<ir::AnnotationElement *> &annotationElement, const schema::AnnotationElementsMatcher *matcher);

    bool IsClassMatched(uint32_t type_idx, const schema::ClassMatcher *matcher);
    bool IsTypeNameMatched(uint32_t type_idx, const schema::StringMatcher *matcher);
    bool IsClassAccessFlagsMatched(uint32_t type_idx, const schema::AccessFlagsMatcher *matcher);
    bool IsClassSmaliSourceMatched(uint32_t type_idx, const schema::StringMatcher *matcher);
    bool IsClassUsingStringsMatched(uint32_t type_idx, const schema::ClassMatcher *matcher);
    bool IsSuperClassMatched(uint32_t type_idx, const schema::ClassMatcher *matcher);
    bool IsInterfacesMatched(uint32_t type_idx, const schema::InterfacesMatcher *matcher);
    bool IsClassAnnotationMatched(uint32_t type_idx, const schema::AnnotationsMatcher *matcher);
    bool IsFieldsMatched(uint32_t type_idx, const schema::FieldsMatcher *matcher);
    bool IsMethodsMatched(uint32_t type_idx, const schema::MethodsMatcher *matcher);

    bool IsMethodMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsParametersMatched(uint32_t method_idx, const schema::ParametersMatcher *matcher);
    bool IsOpCodesMatched(uint32_t method_idx, const schema::OpCodesMatcher *matcher);
    bool IsMethodUsingStringsMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsMethodAnnotationMatched(uint32_t method_idx, const schema::AnnotationsMatcher *matcher);
    bool IsUsingFieldsMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsUsingNumbersMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsInvokingMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher);
    bool IsCallMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher);

    bool IsUsingFieldMatched(std::pair<uint32_t, bool> field, const schema::UsingFieldMatcher *matcher);
    bool IsFieldMatched(uint32_t field_idx, const schema::FieldMatcher *matcher);
    bool IsFieldAnnotationMatched(uint32_t field_idx, const schema::AnnotationsMatcher *matcher);
    bool IsFieldGetMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher);
    bool IsFieldPutMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher);

private:
    DexKit *dexkit;
    std::unique_ptr<MemMap> _image;
    dex::Reader reader;

    uint32_t dex_cross_flag = 0;
    uint32_t dex_flag = 0;
    uint32_t dex_id;

    uint32_t empty_string_id = dex::kNoIndex;
    uint32_t annotation_target_class_id = dex::kNoIndex;
    uint32_t annotation_retention_class_id = dex::kNoIndex;
    phmap::flat_hash_map<uint32_t /*field_id*/, schema::TargetElementType> target_element_map;
    phmap::flat_hash_map<uint32_t /*field_id*/, schema::RetentionPolicyType> retention_map;

    // mutex hash pool
    std::unique_ptr<std::array<std::mutex, 32>> type_def_mutexes = std::make_unique<std::array<std::mutex, 32>>();

    // string constants, sorted by string value
    std::vector<std::string_view> strings;
    std::vector<std::string_view> type_names;
    std::vector<uint8_t> type_name_array_count;
    phmap::flat_hash_map<std::string_view /*type_name*/, uint32_t /*type_id*/> type_ids_map;
    std::vector<uint32_t /*class_def_id*/> type_def_idx;
    // dex declared types flag
    std::vector<bool /*def_in_class_def*/> type_def_flag;
    // class source file name, eg: "HelloWorld.java", maybe obfuscated
    std::vector<std::string_view> class_source_files;
    std::vector<uint32_t /*access_flag*/> class_access_flags;
    std::vector<std::vector<uint32_t>> class_interface_ids;
    std::vector<std::optional<std::string>> method_descriptors;
    std::vector<std::vector<uint32_t /*method_id*/>> class_method_ids;
    std::vector<uint32_t /*access_flag*/> method_access_flags;
    std::vector<std::optional<std::string>> field_descriptors;
    std::vector<std::vector<uint32_t /*field_id*/>> class_field_ids;
    std::vector<uint32_t /*access_flag*/> field_access_flags;
    std::vector<const dex::Code *> method_codes;
    // method parameter types
    std::vector<const dex::TypeList *> proto_type_list;
    std::vector<std::optional<std::vector<uint8_t /*opcode*/>>> method_opcode_seq;
    std::vector<ir::AnnotationSet *> class_annotations;
    std::vector<ir::AnnotationSet *> method_annotations;
    std::vector<ir::AnnotationSet *> field_annotations;
    std::vector<std::vector<ir::AnnotationSet *>> method_parameter_annotations;

    std::vector<std::optional<std::pair<uint16_t, uint32_t>>> method_cross_info;
    std::vector<std::optional<std::pair<uint16_t, uint32_t>>> field_cross_info;

    std::vector<std::vector<EncodeNumber /*using_number*/>> method_using_numbers;
    std::vector<std::vector<uint32_t /*using_string*/>> method_using_string_ids;
    std::vector<std::vector<uint32_t /*invoke_method_id*/>> method_invoking_ids;
    std::vector<std::vector<std::pair<uint32_t /*method_id*/, bool /*is_getting*/>>> method_using_field_ids;
    // maybe cross dex
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*call_method_id*/>>> method_caller_ids;
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*field_id*/>>> field_get_method_ids;
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*field_id*/>>> field_put_method_ids;
};

} // namespace dexkit