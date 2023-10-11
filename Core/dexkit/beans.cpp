#include "beans.h"

namespace dexkit {

flatbuffers::Offset<schema::ClassMeta>
ClassBean::CreateClassMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto class_meta = schema::CreateClassMeta(
            fbb,
            this->id,
            this->dex_id,
            fbb.CreateString(this->source_file),
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
            this->access_flags,
            fbb.CreateString(this->dex_descriptor),
            this->type_id
    );
    fbb.Finish(field_meta);
    return field_meta;
}

flatbuffers::Offset<schema::AnnotationEncodeValueMeta> // NOLINTNEXTLINE
AnnotationEncodeValueBean::CreateAnnotationEncodeValueMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    using namespace schema;
    flatbuffers::Offset<void> offset;
    switch (this->type) {
        case AnnotationEncodeValueType::ByteValue: offset = schema::CreateEncodeValueByte(fbb, get<int8_t>(this->value)).Union(); break;
        case AnnotationEncodeValueType::ShortValue: offset = schema::CreateEncodeValueShort(fbb, get<int16_t>(this->value)).Union(); break;
        case AnnotationEncodeValueType::CharValue: offset = schema::CreateEncodeValueChar(fbb, get<int16_t>(this->value)).Union(); break;
        case AnnotationEncodeValueType::IntValue: offset = schema::CreateEncodeValueInt(fbb, get<int32_t>(this->value)).Union(); break;
        case AnnotationEncodeValueType::LongValue: offset = schema::CreateEncodeValueLong(fbb, get<int64_t>(this->value)).Union(); break;
        case AnnotationEncodeValueType::FloatValue: offset = schema::CreateEncodeValueFloat(fbb, get<float>(this->value)).Union(); break;
        case AnnotationEncodeValueType::DoubleValue: offset = schema::CreateEncodeValueDouble(fbb, get<double>(this->value)).Union(); break;
        case AnnotationEncodeValueType::StringValue: offset = schema::CreateEncodeValueString(fbb, fbb.CreateString(get<std::string_view>(this->value))).Union(); break;
        case AnnotationEncodeValueType::TypeValue: offset = get<std::unique_ptr<ClassBean>>(this->value)->CreateClassMeta(fbb).Union(); break;
        case AnnotationEncodeValueType::MethodValue: offset = get<std::unique_ptr<MethodBean>>(this->value)->CreateMethodMeta(fbb).Union(); break;
        case AnnotationEncodeValueType::EnumValue: offset = get<std::unique_ptr<FieldBean>>(this->value)->CreateFieldMeta(fbb).Union(); break;
        case AnnotationEncodeValueType::ArrayValue: offset = get<std::unique_ptr<AnnotationEncodeArrayBean>>(this->value)->CreateAnnotationEncodeArray(fbb).Union(); break;
        case AnnotationEncodeValueType::AnnotationValue: offset = get<std::unique_ptr<AnnotationBean>>(this->value)->CreateAnnotationMeta(fbb).Union(); break;
        case AnnotationEncodeValueType::NullValue: offset = schema::CreateEncodeValueNull(fbb).Union(); break;
        case AnnotationEncodeValueType::BoolValue: offset = schema::CreateEncodeValueBoolean(fbb, get<bool>(this->value)).Union(); break;
    }
    auto annotation_encode_value_meta = schema::CreateAnnotationEncodeValueMeta(
            fbb,
            this->type,
            schema::AnnotationEncodeValue((int) this->type + 1),
            offset
    );
    fbb.Finish(annotation_encode_value_meta);
    return annotation_encode_value_meta;
}

flatbuffers::Offset<schema::AnnotationEncodeArray> // NOLINTNEXTLINE
AnnotationEncodeArrayBean::CreateAnnotationEncodeArray(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<flatbuffers::Offset<schema::AnnotationEncodeValueMeta>> array;
    array.reserve(this->values.size());
    for (auto &value: this->values) {
        array.emplace_back(value.CreateAnnotationEncodeValueMeta(fbb));
    }
    auto annotation_element_value_array = schema::CreateAnnotationEncodeArray(
            fbb,
            fbb.CreateVector(array)
    );
    fbb.Finish(annotation_element_value_array);
    return annotation_element_value_array;
}

flatbuffers::Offset<schema::AnnotationElementMeta> // NOLINTNEXTLINE
AnnotationElementBean::CreateAnnotationElementMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    auto annotation_member_meta = schema::CreateAnnotationElementMeta(
            fbb,
            fbb.CreateString(this->name),
            this->value.CreateAnnotationEncodeValueMeta(fbb)
    );
    fbb.Finish(annotation_member_meta);
    return annotation_member_meta;
}

flatbuffers::Offset<schema::AnnotationMeta> // NOLINTNEXTLINE
AnnotationBean::CreateAnnotationMeta(flatbuffers::FlatBufferBuilder &fbb) const {
    std::vector<flatbuffers::Offset<schema::AnnotationElementMeta>> annotation_members;
    annotation_members.reserve(this->elements.size());
    for (auto &member : this->elements) {
        annotation_members.push_back(member.CreateAnnotationElementMeta(fbb));
    }
    auto annotation_meta = schema::CreateAnnotationMeta(
            fbb,
            this->dex_id,
            this->type_id,
            fbb.CreateString(this->type_descriptor),
            this->visibility,
            fbb.CreateVector(annotation_members)
    );
    fbb.Finish(annotation_meta);
    return annotation_meta;
}

flatbuffers::Offset<schema::BatchClassMeta>
BatchFindClassItemBean::CreateBatchClassMeta(flatbuffers::FlatBufferBuilder &fbb) const {
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
BatchFindMethodItemBean::CreateBatchMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const {
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