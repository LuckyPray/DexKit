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

    class_annotations.resize(reader.TypeIds().size(), nullptr);
    for (auto &class_def: reader.ClassDefs()) {
        if (class_def.annotations_off == 0) {
            continue;
        }
        auto annotations = reader.ExtractAnnotations(class_def.annotations_off);
        class_annotations[class_def.class_idx] = annotations;
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
    bean.dex_descriptor = this->GetMethodDescriptors(method_idx);
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
    return {};
}

AnnotationBean DexItem::GetAnnotationBean(uint32_t target_id, AnnotationTargetType type) {
    return {};
}

std::string_view DexItem::GetMethodDescriptors(uint32_t method_idx) {
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

std::string_view DexItem::GetFieldDescriptors(uint32_t field_idx) {
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