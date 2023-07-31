#include "dex_item.h"

namespace dexkit {

DexItem::DexItem(uint8_t *data, size_t size) :
        _image(std::make_unique<MemMap>(data, size)), reader(_image->addr(), _image->len()) {
    InitCache();
}

DexItem::DexItem(std::unique_ptr<MemMap> mmap) :
        _image(std::move(mmap)), reader(_image->addr(), _image->len()) {
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
    method_access_flags.resize(reader.MethodIds().size());
    method_codes.resize(reader.MethodIds().size(), nullptr);
    field_access_flags.resize(reader.FieldIds().size());
    method_opcode_seq.resize(reader.MethodIds().size(), std::nullopt);

    for (auto &class_def: reader.ClassDefs()) {
        if (class_def.source_file_idx != dex::kNoIndex) {
            class_source_files[class_def.class_idx] = strings[class_def.source_file_idx];
        }
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

std::vector<ClassBean>
DexItem::FindClass(const schema::FindClass *query) {
    return {};
}

std::vector<MethodBean>
DexItem::FindMethod(const schema::FindMethod *query) {
    return {};
}

std::vector<FieldBean>
DexItem::FindField(const schema::FindField *query) {
    return {};
}

}