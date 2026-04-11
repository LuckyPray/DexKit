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

#include <array>
#include <string_view>
#include <vector>
#include <condition_variable>

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
#include "query_executor.h"
#include "mmap.h"
#include "package_trie.h"
#include "query_context.h"
#include "dexkit.h"
#include "analyze.h"

namespace dexkit {

class DexKit;

namespace internal {
struct UsingStringsPrefilterPlan;
}

class DexItem {
public:

    explicit DexItem(uint32_t id, std::shared_ptr<MemMap> mmap, uint32_t header_off, DexKit *dexkit);
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
            IQueryExecutor &executor,
            uint32_t split_num,
            QueryContext &query_context
    );
    std::vector<std::future<std::vector<MethodBean>>>
    FindMethod(
            const schema::FindMethod *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            IQueryExecutor &executor,
            uint32_t split_num,
            QueryContext &query_context
    );
    std::vector<std::future<std::vector<FieldBean>>>
    FindField(
            const schema::FindField *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_field_set,
            trie::PackageTrie &packageTrie,
            IQueryExecutor &executor,
            uint32_t split_num,
            QueryContext &query_context
    );
    std::vector<ClassBean> FindClass(
            const schema::FindClass *query,
            const std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            QueryContext &query_context
    );
    std::vector<MethodBean> FindMethod(
            const schema::FindMethod *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            QueryContext &query_context
    );
    std::vector<FieldBean> FindField(
            const schema::FindField *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_field_set,
            trie::PackageTrie &packageTrie,
            uint32_t start,
            uint32_t end,
            QueryContext &query_context
    );
    std::vector<ClassBean> FindClass(
            const schema::FindClass *query,
            const std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie,
            uint32_t type_idx,
            QueryContext &query_context
    );
    std::vector<MethodBean> FindMethod(
            const schema::FindMethod *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            uint32_t type_idx,
            QueryContext &query_context
    );
    std::vector<FieldBean> FindField(
            const schema::FindField *query,
            const std::set<uint32_t> &in_class_set,
            const std::set<uint32_t> &in_field_set,
            trie::PackageTrie &packageTrie,
            uint32_t type_idx,
            QueryContext &query_context
    );
    std::vector<BatchFindClassItemBean> BatchFindClassUsingStrings(
            const schema::BatchFindClassUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set,
            trie::PackageTrie &packageTrie,
            QueryContext &query_context
    );
    std::vector<BatchFindMethodItemBean> BatchFindMethodUsingStrings(
            const schema::BatchFindMethodUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set,
            std::set<uint32_t> &in_method_set,
            trie::PackageTrie &packageTrie,
            QueryContext &query_context
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

    // Member-scoped metadata getters may be called without a DexKit-level warm-up barrier.
    // They either read immutable base dex data directly or own a local lazy/fallback path.
    std::vector<AnnotationBean> GetClassAnnotationBeans(uint32_t class_idx);
    std::vector<AnnotationBean> GetMethodAnnotationBeans(uint32_t method_idx);
    std::vector<AnnotationBean> GetFieldAnnotationBeans(uint32_t field_idx);
    std::vector<std::vector<AnnotationBean>> GetParameterAnnotationBeans(uint32_t method_idx);
    std::optional<std::vector<std::optional<std::string_view>>> GetParameterNames(uint32_t method_idx);
    std::vector<uint8_t> GetMethodOpCodes(uint32_t method_idx);
    std::vector<std::string_view> GetUsingStrings(uint32_t method_idx);

    // Cross-ref accessors read final shared indexes and therefore require the outer
    // DexKit query barrier to guarantee the corresponding ready flags.
    std::vector<MethodBean> GetCallMethods(uint32_t method_idx);
    std::vector<MethodBean> GetInvokeMethods(uint32_t method_idx);
    std::vector<UsingFieldBean> GetUsingFields(uint32_t method_idx);
    std::vector<MethodBean> FieldGetMethods(uint32_t field_idx);
    std::vector<MethodBean> FieldPutMethods(uint32_t field_idx);

    bool CheckAllTypeNamesDeclared(std::vector<std::string_view> &types);
    [[nodiscard]] bool NeedPutCrossRef(uint32_t need_cross_flag) const;
    void PutCrossRef(uint32_t put_cross_flag);
    [[nodiscard]] bool NeedInitCache(uint32_t need_flag) const;
    void InitCache(uint32_t init_flags);
    uint32_t BeginInitCache(uint32_t init_flags);
    void FinishInitCache(uint32_t init_flags);
    void WaitInitCache(uint32_t init_flags) const;
    uint32_t BeginPutCrossRef(uint32_t put_cross_flag);
    void FinishPutCrossRef(uint32_t put_cross_flag);
    void WaitPutCrossRef(uint32_t put_cross_flag) const;

private:

    void InitBaseCache();
    inline std::mutex &GetTypeDefMutex(uint32_t type_idx);

    std::string_view GetMethodDescriptor(uint32_t method_idx);
    std::string_view GetFieldDescriptor(uint32_t field_idx);

    std::vector<uint8_t> GetOpSeqFromCode(uint32_t method_idx);
    std::vector<uint32_t> GetUsingStringsFromCode(uint32_t method_idx);
    std::vector<uint32_t> GetInvokeMethodsFromCode(uint32_t method_idx);
    // These helpers are the "member-scoped lazy" exceptions to the global warm-up barrier.
    const std::vector<uint8_t> &GetLazyMethodOpCodes(uint32_t method_idx);
    const std::vector<uint32_t> &GetLazyMethodUsingStringIds(uint32_t method_idx);
    std::vector<EncodeNumber> ParseUsingNumbersFromCode(uint32_t method_idx);
    const std::vector<EncodeNumber> &GetUsingNumbers(uint32_t method_idx);

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
    bool IsProtoShortyMatched(uint32_t shorty_idx, const ::flatbuffers::String *matcher);
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
    bool MayMatchMethodUsingStringsPrefilter(uint32_t method_idx, internal::UsingStringsPrefilterPlan &plan);
    bool MayMatchClassUsingStringsPrefilter(uint32_t type_idx, internal::UsingStringsPrefilterPlan &plan);

private:
    friend class DexKit;

    struct PendingAggregateMethodWorkItem {
        uint32_t source_method_idx;
        uint16_t target_dex_id;
        uint32_t target_method_idx;
    };

    struct PendingAggregateFieldWorkItem {
        uint32_t source_field_idx;
        uint16_t target_dex_id;
        uint32_t target_field_idx;
    };

    enum class LazyMethodFeatureState : uint8_t {
        Empty = 0,
        Building = 1,
        Ready = 2,
    };

    struct LazyMethodOpCodesSlot {
        std::atomic<uint8_t> state{static_cast<uint8_t>(LazyMethodFeatureState::Empty)};
        std::unique_ptr<const std::vector<uint8_t>> data;
    };

    struct LazyMethodUsingStringsSlot {
        std::atomic<uint8_t> state{static_cast<uint8_t>(LazyMethodFeatureState::Empty)};
        std::unique_ptr<const std::vector<uint32_t>> data;
    };

    struct LazyUsingNumbersSlot {
        std::atomic<uint8_t> state{static_cast<uint8_t>(LazyMethodFeatureState::Empty)};
        std::unique_ptr<const std::vector<EncodeNumber>> data;
    };

    DexKit *dexkit;
    std::shared_ptr<MemMap> _image;
    dex::Reader reader;

    std::atomic<uint32_t> dex_cross_flag = 0;
    std::atomic<uint32_t> dex_flag = 0;
    uint32_t dex_id;
    mutable std::mutex init_cache_state_mutex;
    mutable std::condition_variable init_cache_state_cv;
    uint32_t init_cache_inflight_flags = 0;
    mutable std::mutex cross_ref_state_mutex;
    mutable std::condition_variable cross_ref_state_cv;
    uint32_t cross_ref_inflight_flags = 0;

    uint32_t empty_string_id = dex::kNoIndex;
    uint32_t annotation_target_class_id = dex::kNoIndex;
    uint32_t annotation_retention_class_id = dex::kNoIndex;
    phmap::flat_hash_map<uint32_t /*field_id*/, schema::TargetElementType> target_element_map;
    phmap::flat_hash_map<uint32_t /*field_id*/, schema::RetentionPolicyType> retention_map;

    // mutex hash pool
    std::unique_ptr<std::array<std::mutex, 32>> type_def_mutexes = std::make_unique<std::array<std::mutex, 32>>();

    // string constants, sorted by string value
    std::vector<std::string_view> strings;
    // type descriptor
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
    // stable base member indexes; after init only members declared in this dex stay here
    std::vector<std::vector<uint32_t /*method_id*/>> class_method_ids;
    // one-shot worklists for cross-ref against members whose declaring class is outside this dex
    std::vector<std::vector<uint32_t /*method_id*/>> pending_cross_ref_method_ids;
    std::vector<uint32_t /*access_flag*/> method_access_flags;
    std::vector<std::optional<std::string>> field_descriptors;
    std::vector<std::vector<uint32_t /*field_id*/>> class_field_ids;
    std::vector<std::vector<uint32_t /*field_id*/>> pending_cross_ref_field_ids;
    std::vector<uint32_t /*access_flag*/> field_access_flags;
    std::vector<const dex::Code *> method_codes;
    // method parameter types
    std::vector<const dex::TypeList *> proto_type_list;
    std::unique_ptr<LazyMethodOpCodesSlot[]> lazy_method_opcode_slots;
    std::vector<std::optional<std::vector<uint8_t /*opcode*/>>> method_opcode_seq;
    std::vector<ir::AnnotationSet *> class_annotations;
    std::vector<ir::AnnotationSet *> method_annotations;
    std::vector<ir::AnnotationSet *> field_annotations;
    std::vector<std::vector<ir::AnnotationSet *>> method_parameter_annotations;

    std::vector<std::optional<std::pair<uint16_t, uint32_t>>> method_cross_info;
    std::vector<std::optional<std::pair<uint16_t, uint32_t>>> field_cross_info;

    std::unique_ptr<LazyMethodUsingStringsSlot[]> lazy_method_using_string_slots;
    std::vector<std::vector<uint32_t /*using_string*/>> method_using_string_ids;
    std::vector<std::vector<EncodeNumber /*using_number*/>> method_using_numbers;
    std::unique_ptr<LazyUsingNumbersSlot[]> lazy_using_numbers_slots;
    std::unique_ptr<std::array<std::mutex, 64>> lazy_method_wait_mutexes = std::make_unique<std::array<std::mutex, 64>>();
    std::unique_ptr<std::array<std::condition_variable, 64>> lazy_method_wait_cvs = std::make_unique<std::array<std::condition_variable, 64>>();
    std::vector<std::vector<uint32_t /*invoke_method_id*/>> method_invoking_ids;
    std::vector<std::vector<std::pair<uint32_t /*method_id*/, bool /*is_getting*/>>> method_using_field_ids;
    // local reverse edges are collected during InitCache;
    // cross-dex contributions are merged into these final indexes by DexKit during aggregate phase
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*call_method_id*/>>> method_caller_ids;
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*field_id*/>>> field_get_method_ids;
    std::vector<std::vector<std::pair<uint16_t /*dex_id*/, uint32_t /*field_id*/>>> field_put_method_ids;
    // one-shot aggregate worklists: pre-resolved source->target bindings that also
    // carry reverse-edge payload, so BuildCrossRefAggregates can skip re-reading cross_info
    std::vector<PendingAggregateMethodWorkItem> pending_aggregate_method_work_items;
    std::vector<PendingAggregateFieldWorkItem> pending_aggregate_field_work_items;
};

} // namespace dexkit
