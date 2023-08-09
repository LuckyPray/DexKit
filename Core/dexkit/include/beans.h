#pragma once

#include <variant>
#include "schema/enums_generated.h"
#include "schema/results_generated.h"

namespace dexkit {

enum AnnotationTargetType {
    Class,
    Method,
    Field,
};

class ClassBean {
public:
    uint32_t id;
    uint32_t dex_id;
    std::string_view source_file;
    std::vector<uint32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    uint32_t super_class_id;
    std::vector<uint32_t> interface_ids;
    std::vector<uint32_t> method_ids;
    std::vector<uint32_t> field_ids;

public:
    flatbuffers::Offset<schema::ClassMeta>
    CreateClassMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class MethodBean {
public:
    uint32_t id;
    uint32_t dex_id;
    uint32_t class_id;
    std::vector<uint32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    uint32_t return_type;
    std::vector<uint32_t> parameter_types;

public:
    flatbuffers::Offset<schema::MethodMeta>
    CreateMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class FieldBean {
public:
    uint32_t id;
    uint32_t dex_id;
    uint32_t class_id;
    std::vector<uint32_t> annotation_ids;
    uint32_t access_flags;
    std::string_view dex_descriptor;
    uint32_t type_id;

public:
    flatbuffers::Offset<schema::FieldMeta>
    CreateFieldMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationBean;
class AnnotationElementBean;
class AnnotationElementValueArray;
union AnnotationElementValue;

//using AnnotationElementValue = std::variant<
//        int8_t,
//        int16_t,
//        int32_t,
//        int64_t,
//        float,
//        double,
//        std::string_view,
//        ClassBean,
//        FieldBean,
//        AnnotationElementValueArray,
//        AnnotationBean,
//        bool>;

class AnnotationElementValueArray {
public:
    schema::AnnotationElementValueType type;
    std::vector<AnnotationElementValue> values;

public:
    flatbuffers::Offset<schema::AnnotationElementValueArray>
    CreateAnnotationElementValueArray(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationBean {
public:
    uint32_t dex_id;
    uint32_t type_id;
    std::string_view type_descriptor;
    std::vector<schema::TargetElementType> target_element_types;
    schema::RetentionPolicyType retention_policy;
    std::vector<AnnotationElementBean> elements;

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
    ClassBean* type_value;
    // dex field id
    FieldBean* enum_value;
    AnnotationElementValueArray* array_value;
    AnnotationBean* annotation_value;
    bool bool_value;

public:
    std::pair<dexkit::schema::AnnotationElementValue, flatbuffers::Offset<void>>
    CreateAnnotationElementValue(flatbuffers::FlatBufferBuilder &fbb, schema::AnnotationElementValueType type) const;
};

class AnnotationElementBean {
public:
    std::string_view name;
    schema::AnnotationElementValueType type;
    AnnotationElementValue value;

public:
    flatbuffers::Offset<schema::AnnotationElementMeta>
    CreateAnnotationElementMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindClassItemBean {
public:
    std::string_view union_key;
    std::vector<ClassBean> classes;

public:
    flatbuffers::Offset<schema::BatchClassMeta>
    CreateBatchClassMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindMethodItemBean {
public:
    std::string_view union_key;
    std::vector<MethodBean> methods;

public:
    flatbuffers::Offset<schema::BatchMethodMeta>
    CreateBatchMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

}