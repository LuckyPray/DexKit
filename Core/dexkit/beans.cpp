#include "beans.h"

namespace dexkit {

flatbuffers::Offset<schema::ClassMeta>
ClassBean::CreateClassMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto class_meta = schema::CreateClassMeta(
            fbb,
            this->id,
            this->dex_id,
            fbb.CreateString(this->source_file),
            fbb.CreateVector(std::vector<int32_t>(this->annotation_ids.begin(), this->annotation_ids.end())),
            this->access_flags,
            fbb.CreateString(this->dex_descriptor),
            this->super_class_id,
            fbb.CreateVector(std::vector<int32_t>(this->interface_ids.begin(), this->interface_ids.end())),
            fbb.CreateVector(std::vector<int32_t>(this->method_ids.begin(), this->method_ids.end())),
            fbb.CreateVector(std::vector<int32_t>(this->field_ids.begin(), this->field_ids.end()))
    );
    fbb.Finish(class_meta);
    return class_meta;
}

flatbuffers::Offset<schema::MethodMeta>
MethodBean::CreateMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto method_meta = schema::CreateMethodMeta(
            fbb,
            this->id,
            this->dex_id,
            this->class_id,
            fbb.CreateVector(std::vector<int32_t>(this->annotation_ids.begin(), this->annotation_ids.end())),
            this->access_flags,
            fbb.CreateString(this->dex_descriptor),
            this->return_type,
            fbb.CreateVector(std::vector<int32_t>(this->parameter_types.begin(), this->parameter_types.end()))
    );
    fbb.Finish(method_meta);
    return method_meta;
}

flatbuffers::Offset<schema::FieldMeta>
FieldBean::CreateFieldMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto field_meta = schema::CreateFieldMeta(
            fbb,
            this->id,
            this->dex_id,
            this->class_id,
            fbb.CreateVector(std::vector<int32_t>(this->annotation_ids.begin(), this->annotation_ids.end())),
            this->access_flags,
            fbb.CreateString(this->dex_descriptor),
            this->type_id
    );
    fbb.Finish(field_meta);
    return field_meta;
}

flatbuffers::Offset<schema::AnnotationElementValueArray>
AnnotationElementValueArray::CreateAnnotationElementValueArray(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<dexkit::schema::AnnotationElementValue> element_types;
    std::vector<flatbuffers::Offset<void>> element_values;
    for (auto &value : this->values) {
        auto [element_type, element_value] = value.CreateAnnotationElementValue(fbb, this->type);
        element_types.push_back(element_type);
        element_values.push_back(element_value);
    }
    assert(element_types.size() == this->values.size());
    auto annotation_element_value_array = schema::CreateAnnotationElementValueArray(
            fbb,
            fbb.CreateVector(element_types),
            fbb.CreateVector(element_values)
    );
    fbb.Finish(annotation_element_value_array);
    return annotation_element_value_array;
}

flatbuffers::Offset<schema::EnumValueMeta>
EnumValueBean::CreateEnumValueMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto enum_value_meta = schema::CreateEnumValueMeta(
            fbb,
            this->id,
            this->dex_id,
            this->class_id,
            fbb.CreateString(this->name)
    );
    fbb.Finish(enum_value_meta);
    return enum_value_meta;
}

std::pair<dexkit::schema::AnnotationElementValue, flatbuffers::Offset<void>>
AnnotationElementValue::CreateAnnotationElementValue(flatbuffers::FlatBufferBuilder &fbb, schema::AnnotationElementValueType type) const {
    schema::AnnotationElementValue value_type;
    flatbuffers::Offset<void> value;
    switch (type) {
        case schema::AnnotationElementValueType::Byte:
            value_type = schema::AnnotationElementValue::EncodeValueByte;
            value = schema::CreateEncodeValueByte(fbb, this->byte_value).Union();
            break;
        case schema::AnnotationElementValueType::Short:
            value_type = schema::AnnotationElementValue::EncodeValueShort;
            value = schema::CreateEncodeValueShort(fbb, this->short_value).Union();
            break;
        case schema::AnnotationElementValueType::Char:
            value_type = schema::AnnotationElementValue::EncodeValueChar;
            value = schema::CreateEncodeValueChar(fbb, this->char_value).Union();
            break;
        case schema::AnnotationElementValueType::Int:
            value_type = schema::AnnotationElementValue::EncodeValueInt;
            value = schema::CreateEncodeValueInt(fbb, this->int_value).Union();
            break;
        case schema::AnnotationElementValueType::Long:
            value_type = schema::AnnotationElementValue::EncodeValueLong;
            value = schema::CreateEncodeValueLong(fbb, this->long_value).Union();
            break;
        case schema::AnnotationElementValueType::Float:
            value_type = schema::AnnotationElementValue::EncodeValueFloat;
            value = schema::CreateEncodeValueFloat(fbb, this->float_value).Union();
            break;
        case schema::AnnotationElementValueType::Double:
            value_type = schema::AnnotationElementValue::EncodeValueDouble;
            value = schema::CreateEncodeValueDouble(fbb, this->double_value).Union();
            break;
        case schema::AnnotationElementValueType::String:
            value_type = schema::AnnotationElementValue::EncodeValueString;
            value = schema::CreateEncodeValueString(fbb, fbb.CreateString(this->string_value)).Union();
            break;
        case schema::AnnotationElementValueType::Type:
            value_type = schema::AnnotationElementValue::ClassMeta;
            value = this->type_value.CreateClassMeta(fbb).Union();
            break;
        case schema::AnnotationElementValueType::Enum:
            value_type = schema::AnnotationElementValue::EnumValueMeta;
            value = this->enum_value.CreateEnumValueMeta(fbb).Union();
            break;
        case schema::AnnotationElementValueType::Array:
            value_type = schema::AnnotationElementValue::AnnotationElementValueArray;
            value = this->array_value.CreateAnnotationElementValueArray(fbb).Union();
            break;
        case schema::AnnotationElementValueType::Annotation:
            value_type = schema::AnnotationElementValue::AnnotationMeta;
            value = this->annotation_value.CreateAnnotationMeta(fbb).Union();
            break;
        case schema::AnnotationElementValueType::Bool:
            value_type = schema::AnnotationElementValue::EncodeValueBoolean;
            value = schema::CreateEncodeValueBoolean(fbb, this->bool_value).Union();
            break;
    }
    return std::make_pair(value_type, value);
}

flatbuffers::Offset<schema::AnnotationMemberMeta>
AnnotationMemberBean::CreateAnnotationMember(flatbuffers::FlatBufferBuilder &fbb) const {
    auto enum_pair = this->value.CreateAnnotationElementValue(fbb, this->type);
    auto annotation_member_meta = schema::CreateAnnotationMemberMeta(
            fbb,
            fbb.CreateString(this->name),
            enum_pair.first,
            enum_pair.second
    );
    fbb.Finish(annotation_member_meta);
    return annotation_member_meta;
}

flatbuffers::Offset<schema::AnnotationMeta>
AnnotationBean::CreateAnnotationMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<flatbuffers::Offset<schema::AnnotationMemberMeta>> annotation_members;
    annotation_members.reserve(this->elements.size());
    for (auto &member : this->elements) {
        annotation_members.push_back(member.CreateAnnotationMember(fbb));
    }
    auto annotation_meta = schema::CreateAnnotationMeta(
            fbb,
            this->id,
            this->dex_id,
            this->class_id,
            fbb.CreateVector(this->target_element_types),
            this->retention_policy,
            fbb.CreateVector(annotation_members)
    );
    fbb.Finish(annotation_meta);
    return annotation_meta;
}

flatbuffers::Offset<schema::BatchClassMeta>
BatchFindClassItemBean::CreateBatchFindClassItem(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<flatbuffers::Offset<schema::ClassMeta>> class_metas;
    class_metas.reserve(this->classes.size());
    for (auto &clazz : this->classes) {
        class_metas.push_back(clazz.CreateClassMeta(fbb));
    }
    auto batch_find_class_item = schema::CreateBatchClassMeta(
            fbb,
            fbb.CreateString(this->union_key),
            fbb.CreateVector(class_metas)
    );
    fbb.Finish(batch_find_class_item);
    return batch_find_class_item;
}

flatbuffers::Offset<schema::BatchMethodMeta>
BatchFindMethodItemBean::CreateBatchFindMethodItem(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<flatbuffers::Offset<schema::MethodMeta>> method_metas;
    method_metas.reserve(this->methods.size());
    for (auto &method : this->methods) {
        method_metas.push_back(method.CreateMethodMeta(fbb));
    }
    auto batch_find_method_item = schema::CreateBatchMethodMeta(
            fbb,
            fbb.CreateString(this->union_key),
            fbb.CreateVector(method_metas)
    );
    fbb.Finish(batch_find_method_item);
    return batch_find_method_item;
}

}