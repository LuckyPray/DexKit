#pragma once

#include <string_view>
#include <vector>


#include "beans.h"
#include "common.h"
#include "constant.h"
#include "error.h"
#include "kmp.h"

#include "parallel_hashmap/phmap.h"
#include "slicer/reader.h"
#include "schema/querys_generated.h"
#include "schema/results_generated.h"
#include "acdat/Builder.h"
#include "ThreadVariable.h"
#include "file_helper.h"

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
    [[nodiscard]] uint32_t GetDexId() const {
        return dex_id;
    }

    std::vector<ClassBean> FindClass(const schema::FindClass *query, std::set<uint32_t> &in_class_set);
    std::vector<MethodBean> FindMethod(const schema::FindMethod *query, std::set<uint32_t> &in_class_set, std::set<uint32_t> &in_method_set);
    std::vector<FieldBean> FindField(const schema::FindField *query, std::set<uint32_t> &in_class_set, std::set<uint32_t> &in_field_set);
    std::vector<BatchFindClassItemBean> BatchFindClassUsingStrings(
            const schema::BatchFindClassUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set
    );
    std::vector<BatchFindMethodItemBean> BatchFindMethodUsingStrings(
            const schema::BatchFindMethodUsingStrings *query,
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            std::map<std::string_view, std::set<std::string_view>> &keywords_map,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map,
            std::set<uint32_t> &in_class_set,
            std::set<uint32_t> &in_method_set
    );

    ClassBean GetClassBean(uint32_t type_idx);
    MethodBean GetMethodBean(uint32_t method_idx);
    FieldBean GetFieldBean(uint32_t field_idx);

    AnnotationBean GetAnnotationBean(ir::Annotation *annotation);
    AnnotationEncodeValueBean GetAnnotationEncodeValueBean(ir::EncodedValue *encoded_value);
    AnnotationElementBean GetAnnotationElementBean(ir::AnnotationElement *annotation_element);
    AnnotationEncodeArrayBean GetAnnotationEncodeArrayBean(ir::EncodedArray *encoded_array);

    std::vector<AnnotationBean> GetClassAnnotationBeans(uint32_t class_idx);
    std::vector<AnnotationBean> GetMethodAnnotationBeans(uint32_t method_idx);
    std::vector<AnnotationBean> GetFieldAnnotationBeans(uint32_t field_idx);
    std::vector<std::vector<AnnotationBean>> GetParameterAnnotationBeans(uint32_t method_idx);

private:

    int InitCache();

    phmap::flat_hash_map<uint32_t, std::vector<std::string_view>>
    InitBatchFindStringsMap(
            acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    std::string_view GetMethodDescriptor(uint32_t method_idx);
    std::string_view GetFieldDescriptor(uint32_t field_idx);

    static bool IsStringMatched(std::string_view str, const schema::StringMatcher *matcher);
    static bool IsTypeNameMatched(std::string_view type_name, const schema::StringMatcher *matcher);
    static bool IsAccessFlagsMatched(uint32_t access_flags, const schema::AccessFlagsMatcher *matcher);
    static std::set<std::string_view> BuildBatchFindKeywordsMap(
            const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
            std::vector<std::pair<std::string_view, bool>> &keywords,
            phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
    );

    bool IsAnnotationMatched(const ir::Annotation *annotation, const schema::AnnotationMatcher *matcher);
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
    bool IsUsingFieldsMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsUsingNumbersMatched(uint32_t method_idx, const schema::MethodMatcher *matcher);
    bool IsInvokingMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher);
    bool IsCallMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher);

    bool IsUsingFieldMatched(std::pair<uint32_t, bool> field, const schema::UsingFieldMatcher *matcher);
    bool IsFieldMatched(uint32_t field_idx, const schema::FieldMatcher *matcher);
    bool IsFieldGetMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher);
    bool IsFieldPutMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher);

private:
    std::unique_ptr<MemMap> _image;
    dex::Reader reader;

    uint32_t dex_flag = 0;
    uint32_t dex_id;

    uint32_t annotation_target_class_id = dex::kNoIndex;
    phmap::flat_hash_map<uint32_t /*field_id*/, schema::TargetElementType> target_element_map;

    // string constants, sorted by string value
    std::vector<std::string_view> strings;
    std::vector<std::string_view> type_names;
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

    std::vector<std::vector<uint32_t /*call_method_id*/>> method_caller_ids;
    std::vector<std::vector<uint32_t /*invoke_method_id*/>> method_invoking_ids;
    std::vector<std::vector<EncodeNumber /*using_number*/>> method_using_number;
    std::vector<std::vector<uint32_t /*using_string*/>> method_using_string_ids;
    std::vector<std::vector<std::pair<uint32_t /*method_id*/, bool /*is_getting*/>>> method_using_field_ids;
    std::vector<std::vector<uint32_t /*field_id*/>> field_get_method_ids;
    std::vector<std::vector<uint32_t /*field_id*/>> field_put_method_ids;
};

} // namespace dexkit