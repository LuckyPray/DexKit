#include "dex_item.h"

namespace dexkit {

DexItem::DexItem(uint32_t id, uint8_t *data, size_t size) :
        dex_id(id),
        _image(std::make_unique<MemMap>(data, size)),
        reader(_image->addr(), _image->len()) {
    InitCache();
}

DexItem::DexItem(uint32_t id, std::unique_ptr<MemMap> mmap) :
        dex_id(id),
        _image(std::move(mmap)),
        reader(_image->addr(), _image->len()) {
    InitCache();
}

int DexItem::InitCache() {
    strings.resize(reader.StringIds().size());
    auto strings_it = strings.begin();
    for (auto &str: reader.StringIds()) {
        auto *str_ptr = reader.dataPtr<dex::u1>(str.string_data_off);
        ReadULeb128(&str_ptr);
        *strings_it++ = reinterpret_cast<const char *>(str_ptr);
    }

    type_names.resize(reader.TypeIds().size());
    auto type_names_it = type_names.begin();
    int idx = 0;
    for (auto &type_id: reader.TypeIds()) {
        *type_names_it = strings[type_id.descriptor_idx];
        type_ids_map[*type_names_it++] = idx++;
    }

    uint32_t element_type_idx = dex::kNoIndex;
    auto element_type_class_desc = "Ljava/lang/annotation/ElementType;";
    auto annotation_target_class_desc = "Ljava/lang/annotation/Target;";
    if (type_ids_map.contains(element_type_class_desc)) {
        element_type_idx = type_ids_map[element_type_class_desc];
    }
    if (type_ids_map.contains(annotation_target_class_desc)) {
        annotation_target_class_id = type_ids_map[annotation_target_class_desc];
    }

    proto_type_list.resize(reader.ProtoIds().size());
    auto proto_it = proto_type_list.begin();
    for (auto &proto: reader.ProtoIds()) {
        if (proto.parameters_off != 0) {
            auto *type_list_ptr = reader.dataPtr<dex::TypeList>(proto.parameters_off);
            *proto_it = type_list_ptr;
        }
        ++proto_it;
    }

    class_field_ids.resize(reader.TypeIds().size());
    int field_idx = 0;
    for (auto &field: reader.FieldIds()) {
        if (field.class_idx == element_type_idx) {
            auto name = strings[field.name_idx];
            if (name == "TYPE") {
                target_element_map[field_idx] = schema::TargetElementType::Type;
            } else if (name == "FIELD") {
                target_element_map[field_idx] = schema::TargetElementType::Field;
            } else if (name == "METHOD") {
                target_element_map[field_idx] = schema::TargetElementType::Method;
            } else if (name == "PARAMETER") {
                target_element_map[field_idx] = schema::TargetElementType::Parameter;
            } else if (name == "CONSTRUCTOR") {
                target_element_map[field_idx] = schema::TargetElementType::Constructor;
            } else if (name == "LOCAL_VARIABLE") {
                target_element_map[field_idx] = schema::TargetElementType::LocalVariable;
            } else if (name == "ANNOTATION_TYPE") {
                target_element_map[field_idx] = schema::TargetElementType::AnnotationType;
            } else if (name == "PACKAGE") {
                target_element_map[field_idx] = schema::TargetElementType::Package;
            } else if (name == "TYPE_PARAMETER") {
                target_element_map[field_idx] = schema::TargetElementType::TypeParameter;
            } else if (name == "TYPE_USE") {
                target_element_map[field_idx] = schema::TargetElementType::TypeUse;
            }
        }
        class_field_ids[field.class_idx].emplace_back(field_idx);
        ++field_idx;
    }

    type_declared_flag.resize(reader.TypeIds().size(), false);
    class_source_files.resize(reader.TypeIds().size());
    class_access_flags.resize(reader.TypeIds().size());
    class_method_ids.resize(reader.TypeIds().size());
    method_descriptors.resize(reader.MethodIds().size());
    method_access_flags.resize(reader.MethodIds().size());
    method_codes.resize(reader.MethodIds().size(), nullptr);
    field_descriptors.resize(reader.FieldIds().size());
    field_access_flags.resize(reader.FieldIds().size());
    method_opcode_seq.resize(reader.MethodIds().size(), std::nullopt);

    auto class_def_idx = 0;
    for (auto &class_def: reader.ClassDefs()) {
        if (class_def.source_file_idx != dex::kNoIndex) {
            class_source_files[class_def.class_idx] = strings[class_def.source_file_idx];
        }
        this->type_id_class_id_map[class_def.class_idx] = class_def_idx;
        type_declared_flag[class_def.class_idx] = true;
        if (class_def.class_data_off == 0) {
            continue;
        }
        const auto *class_data = reader.dataPtr<dex::u1>(class_def.class_data_off);
        dex::u4 static_fields_size = ReadULeb128(&class_data);
        dex::u4 instance_fields_count = ReadULeb128(&class_data);
        dex::u4 direct_methods_count = ReadULeb128(&class_data);
        dex::u4 virtual_methods_count = ReadULeb128(&class_data);

        auto &methods = class_method_ids[class_def.class_idx];

        for (dex::u4 i = 0, class_field_idx = 0; i < static_fields_size; ++i) {
            class_field_idx += ReadULeb128(&class_data);
            field_access_flags[class_field_idx] = ReadULeb128(&class_data);
        }

        for (dex::u4 i = 0, class_field_idx = 0; i < instance_fields_count; ++i) {
            class_field_idx += ReadULeb128(&class_data);
            field_access_flags[class_field_idx] = ReadULeb128(&class_data);
        }

        for (dex::u4 i = 0, class_method_idx = 0; i < direct_methods_count; ++i) {
            class_method_idx += ReadULeb128(&class_data);
            method_access_flags[class_method_idx] = ReadULeb128(&class_data);
            dex::u4 code_off = ReadULeb128(&class_data);
            if (code_off == 0) {
                method_codes[class_method_idx] = &emptyCode;
            } else {
                method_codes[class_method_idx] = reader.dataPtr<const dex::Code>(code_off);
            }
            methods.emplace_back(class_method_idx);
        }
        for (dex::u4 i = 0, class_method_idx = 0; i < virtual_methods_count; ++i) {
            class_method_idx += ReadULeb128(&class_data);
            method_access_flags[class_method_idx] = ReadULeb128(&class_data);
            dex::u4 code_off = ReadULeb128(&class_data);
            if (code_off == 0) {
                method_codes[class_method_idx] = &emptyCode;
            } else {
                method_codes[class_method_idx] = reader.dataPtr<const dex::Code>(code_off);
            }
            methods.emplace_back(class_method_idx);
        }
        ++class_def_idx;
    }

    class_annotations.resize(reader.TypeIds().size());
    field_annotations.resize(reader.FieldIds().size());
    method_annotations.resize(reader.MethodIds().size());
    method_parameter_annotations.resize(reader.MethodIds().size());
    for (auto &class_def: reader.ClassDefs()) {
        if (class_def.annotations_off == 0) {
            continue;
        }
        auto annotations = reader.ExtractAnnotations(class_def.annotations_off);
        class_annotations[class_def.class_idx] = annotations->class_annotation;
        for (auto value: annotations->field_annotations) {
            field_annotations[value->field_decl->orig_index] = value->annotations;
        }
        for (auto value: annotations->method_annotations) {
            method_annotations[value->method_decl->orig_index] = value->annotations;
        }
        for (auto value: annotations->param_annotations) {
            method_parameter_annotations[value->method_decl->orig_index] = value->annotations->annotations;
        }
    }
    return 0;
}

ClassBean DexItem::GetClassBean(uint32_t class_idx) {
    auto &class_def = this->reader.ClassDefs()[this->type_id_class_id_map[class_idx]];
    ClassBean bean;
    bean.id = class_idx;
    bean.dex_id = this->dex_id;
    bean.source_file = this->class_source_files[class_idx];
    // TODO: this->class_annotations[class_idx]
    bean.access_flags = class_def.access_flags;
    bean.dex_descriptor = this->type_names[class_idx];
    bean.super_class_id = class_def.superclass_idx;
    if (class_def.interfaces_off) {
        auto interface_type_list = this->reader.dataPtr<dex::TypeList>(class_def.interfaces_off);
        for (auto i = 0; i < interface_type_list->size; ++i) {
            bean.interface_ids.push_back(interface_type_list->list[i].type_idx);
        }
    }
    bean.field_ids = this->class_field_ids[class_idx];
    bean.method_ids = this->class_method_ids[class_idx];
    return bean;
}

MethodBean DexItem::GetMethodBean(uint32_t method_idx) {
    auto &method_def = this->reader.MethodIds()[method_idx];
    auto &proto_def = this->reader.ProtoIds()[method_def.proto_idx];
    auto &type_list = this->proto_type_list[method_def.proto_idx];
    MethodBean bean;
    bean.id = method_idx;
    bean.dex_id = this->dex_id;
    bean.class_id = method_def.class_idx;
    // TODO: this->method_annotations[method_idx]
    bean.access_flags = this->method_access_flags[method_idx];
    bean.dex_descriptor = this->GetMethodDescriptor(method_idx);
    bean.return_type = proto_def.return_type_idx;
    std::vector<uint32_t> parameter_type_ids;
    auto len = type_list ? type_list->size : 0;
    parameter_type_ids.reserve(len);
    for (int i = 0; i < len; ++i) {
        parameter_type_ids.emplace_back(type_list->list[i].type_idx);
    }
    bean.parameter_types = parameter_type_ids;
    return bean;
}

FieldBean DexItem::GetFieldBean(uint32_t field_idx) {
    auto &field_def = this->reader.FieldIds()[field_idx];
    FieldBean bean;
    bean.id = field_idx;
    bean.dex_id = this->dex_id;
    bean.class_id = field_def.class_idx;
    // TODO: this->field_annotations[method_idx]
    bean.access_flags = this->field_access_flags[field_idx];
    bean.dex_descriptor = this->GetFieldDescriptor(field_idx);
    bean.type_id = field_def.type_idx;
    return bean;
}

// NOLINTNEXTLINE
AnnotationBean DexItem::GetAnnotationBean(ir::Annotation *annotation) {
    AnnotationBean bean;
    bean.dex_id = this->dex_id;
    bean.type_id = annotation->type->orig_index;
    bean.type_descriptor = type_names[annotation->type->orig_index];
    bean.retention_policy = (schema::RetentionPolicyType) annotation->visibility;
    for (auto &element : annotation->elements) {
        bean.elements.emplace_back(GetAnnotationElementBean(element));
    }
    return bean;
}

// NOLINTNEXTLINE
AnnotationEncodeValueBean DexItem::GetAnnotationEncodeValueBean(ir::EncodedValue *encoded_value) {
    AnnotationEncodeValueBean bean;
    switch (encoded_value->type) {
        case 0x00: bean.type = schema::AnnotationEncodeValueType::Byte; break;
        case 0x02: bean.type = schema::AnnotationEncodeValueType::Short; break;
        case 0x03: bean.type = schema::AnnotationEncodeValueType::Char; break;
        case 0x04: bean.type = schema::AnnotationEncodeValueType::Int; break;
        case 0x06: bean.type = schema::AnnotationEncodeValueType::Long; break;
        case 0x10: bean.type = schema::AnnotationEncodeValueType::Float; break;
        case 0x11: bean.type = schema::AnnotationEncodeValueType::Double; break;
        case 0x17: bean.type = schema::AnnotationEncodeValueType::String; break;
        case 0x18: bean.type = schema::AnnotationEncodeValueType::Type; break;
        case 0x1b: bean.type = schema::AnnotationEncodeValueType::Enum; break;
        case 0x1c: bean.type = schema::AnnotationEncodeValueType::Array; break;
        case 0x1d: bean.type = schema::AnnotationEncodeValueType::Annotation; break;
        case 0x1f: bean.type = schema::AnnotationEncodeValueType::Bool; break;
        default: break;
    }
    switch (encoded_value->type) {
        case 0x00: bean.value = encoded_value->u.byte_value; break;
        case 0x02: bean.value = encoded_value->u.short_value; break;
        case 0x03: bean.value = encoded_value->u.char_value; break;
        case 0x04: bean.value = encoded_value->u.int_value; break;
        case 0x06: bean.value = encoded_value->u.long_value; break;
        case 0x10: bean.value = encoded_value->u.float_value; break;
        case 0x11: bean.value = encoded_value->u.double_value; break;
        case 0x17: bean.value = encoded_value->u.string_value->c_str(); break;
        case 0x18: bean.value = std::make_unique<ClassBean>(GetClassBean(encoded_value->u.type_value->orig_index)); break;
        case 0x1b: bean.value = std::make_unique<FieldBean>(GetFieldBean(encoded_value->u.enum_value->orig_index)); break;
        case 0x1c: bean.value = std::make_unique<AnnotationEncodeArrayBean>(GetAnnotationEncodeArrayBean(encoded_value->u.array_value)); break;
        case 0x1d: bean.value = std::make_unique<AnnotationBean>(GetAnnotationBean(encoded_value->u.annotation_value)); break;
        case 0x1f: bean.value = encoded_value->u.bool_value; break;
        default: break;
    }
    return bean;
}

// NOLINTNEXTLINE
AnnotationElementBean DexItem::GetAnnotationElementBean(ir::AnnotationElement *annotation_element) {
    AnnotationElementBean bean;
    bean.name = annotation_element->name->c_str();
    bean.value = GetAnnotationEncodeValueBean(annotation_element->value);
    return bean;
}

// NOLINTNEXTLINE
AnnotationEncodeArrayBean DexItem::GetAnnotationEncodeArrayBean(ir::EncodedArray *encoded_array) {
    AnnotationEncodeArrayBean array;
    for (auto value: encoded_array->values) {
        array.values.emplace_back(GetAnnotationEncodeValueBean(value));
    }
    return array;
}

std::vector<AnnotationBean>
DexItem::GetClassAnnotationBeans(uint32_t class_idx) {
    auto annotationSet = this->class_annotations[class_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet->annotations) {
        AnnotationBean bean;
        bean.dex_id = this->dex_id;
        bean.type_id = annotation->type->orig_index;
        bean.type_descriptor = this->type_names[bean.type_id];
        for (auto element: annotation->elements) {
            bean.elements.emplace_back(GetAnnotationElementBean(element));
        }
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<AnnotationBean>
DexItem::GetMethodAnnotationBeans(uint32_t class_idx) {
    auto annotationSet = this->method_annotations[class_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet->annotations) {
        AnnotationBean bean;
        bean.dex_id = this->dex_id;
        bean.type_id = annotation->type->orig_index;
        bean.type_descriptor = this->type_names[bean.type_id];
        for (auto element: annotation->elements) {
            bean.elements.emplace_back(GetAnnotationElementBean(element));
        }
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<AnnotationBean>
DexItem::GetFieldAnnotationBeans(uint32_t class_idx) {
    auto annotationSet = this->field_annotations[class_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet->annotations) {
        AnnotationBean bean;
        bean.dex_id = this->dex_id;
        bean.type_id = annotation->type->orig_index;
        bean.type_descriptor = this->type_names[bean.type_id];
        for (auto element: annotation->elements) {
            bean.elements.emplace_back(GetAnnotationElementBean(element));
        }
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<std::vector<AnnotationBean>>
DexItem::GetParameterAnnotationBeans(uint32_t method_idx) {
    auto param_annotations = this->method_parameter_annotations[method_idx];
    std::vector<std::vector<AnnotationBean>> beans;
    for (auto annotationSet: param_annotations) {
        std::vector<AnnotationBean> annotationBeans;
        for (auto annotation: annotationSet->annotations) {
            AnnotationBean bean;
            bean.dex_id = this->dex_id;
            bean.type_id = annotation->type->orig_index;
            bean.type_descriptor = this->type_names[bean.type_id];
            for (auto element: annotation->elements) {
                bean.elements.emplace_back(GetAnnotationElementBean(element));
            }
            annotationBeans.emplace_back(std::move(bean));
        }
        beans.emplace_back(std::move(annotationBeans));
    }
    return beans;
}

std::string_view DexItem::GetMethodDescriptor(uint32_t method_idx) {
    auto &method_desc = this->method_descriptors[method_idx];
    if (method_desc != std::nullopt) {
        return method_desc.value();
    }
    auto &method_def = this->reader.MethodIds()[method_idx];
    auto &proto_def = this->reader.ProtoIds()[method_def.proto_idx];
    auto &type_list = this->proto_type_list[method_def.proto_idx];
    auto type_defs = this->reader.TypeIds();

    std::string descriptor(this->type_names[method_def.class_idx]);
    descriptor += "->";
    descriptor += this->strings[method_def.name_idx];
    descriptor += "(";
    auto len = type_list ? type_list->size : 0;
    for (int i = 0; i < len; ++i) {
        descriptor += strings[type_defs[type_list->list[i].type_idx].descriptor_idx];
    }
    descriptor += ')';
    descriptor += strings[type_defs[proto_def.return_type_idx].descriptor_idx];

    method_desc = descriptor;
    return method_desc.value();
}

std::string_view DexItem::GetFieldDescriptor(uint32_t field_idx) {
    auto &field_desc = this->field_descriptors[field_idx];
    if (field_desc != std::nullopt) {
        return field_desc.value();
    }
    auto &field_id = this->reader.FieldIds()[field_idx];
    auto &type_id = this->reader.TypeIds()[field_id.type_idx];

    std::string descriptor(this->type_names[field_id.class_idx]);
    descriptor += "->";
    descriptor += this->strings[field_id.name_idx];
    descriptor += ":";
    descriptor += this->strings[type_id.descriptor_idx];

    field_desc = descriptor;
    return field_desc.value();
}

}