#pragma once

#include "schema/enums_generated.h"
#include "schema/results_generated.h"

namespace dexkit {

class ClassBean {
public:
    int32_t id;
    int32_t dex_id;
    std::string_view source_file;
    std::vector<int32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    int32_t super_class_id;
    std::vector<int32_t> interface_ids;
    std::vector<int32_t> method_ids;
    std::vector<int32_t> field_ids;

public:
    flatbuffers::Offset<schema::ClassMeta>
    CreateClassMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class MethodBean {
public:
    int32_t id;
    int32_t dex_id;
    int32_t class_id;
    std::vector<int32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    int32_t return_type;
    std::vector<int32_t> parameter_types;

public:
    flatbuffers::Offset<schema::MethodMeta>
    CreateMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class FieldBean {
public:
    int32_t id;
    int32_t dex_id;
    int32_t class_id;
    std::vector<int32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    int32_t type;

public:
    flatbuffers::Offset<schema::FieldMeta>
    CreateFieldMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationMemberBean;
class AnnotationElementValueArray;
union AnnotationElementValue;

class AnnotationElementValueArray {
public:
    schema::AnnotationElementValueType type;
    std::vector<AnnotationElementValue> values;

public:
    flatbuffers::Offset<schema::AnnotationElementValueArray>
    CreateAnnotationElementValueArray(flatbuffers::FlatBufferBuilder &fbb) const;
};

class EnumValueBean {
public:
    int32_t id;
    int32_t dex_id;
    int32_t class_id;
    std::string_view name;

public:
    flatbuffers::Offset<schema::EnumValueMeta>
    CreateEnumValueMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationBean {
public:
    int32_t id;
    int32_t dex_id;
    int32_t class_id;
    std::vector<schema::TargetElementType> target_element_types;
    schema::RetentionPolicyType retention_policy;
    std::vector<AnnotationMemberBean> elements;

public:
    flatbuffers::Offset<schema::AnnotationMeta>
    CreateAnnotationMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

union AnnotationElementValue {
    int8_t byte_value;
    int16_t short_value;
    int16_t char_value;
    int32_t int_value;
    int64_t long_value;
    float float_value;
    double double_value;
    std::string_view string_value;
    // dex class id
    ClassBean type_value;
    // dex field id
    EnumValueBean enum_value;
    AnnotationElementValueArray array_value;
    AnnotationBean annotation_value;
    bool bool_value;

public:
    std::pair<dexkit::schema::AnnotationElementValue, flatbuffers::Offset<void>>
    CreateAnnotationElementValue(flatbuffers::FlatBufferBuilder &fbb, schema::AnnotationElementValueType type) const;
};

class AnnotationMemberBean {
public:
    std::string_view name;
    schema::AnnotationElementValueType type;
    AnnotationElementValue value;

public:
    flatbuffers::Offset<schema::AnnotationMemberMeta>
    CreateAnnotationMember(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindClassItemBean {
public:
    std::string_view union_key;
    std::vector<ClassBean> classes;

public:
    flatbuffers::Offset<schema::BatchFindClassItem>
    CreateBatchFindClassItem(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindMethodItemBean {
public:
    std::string_view union_key;
    std::vector<MethodBean> methods;

public:
    flatbuffers::Offset<schema::BatchFindMethodItem>
    CreateBatchFindMethodItem(flatbuffers::FlatBufferBuilder &fbb) const;
};

}