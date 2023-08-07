#include "dex_item.h"
#include "opcode_util.h"

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

std::vector<BatchFindClassItemBean>
DexItem::BatchFindClassUsingStrings(
        const schema::BatchFindClassUsingStrings *query,
        acdat::AhoCorasickDoubleArrayTrie<std::string_view> &acTrie,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    phmap::flat_hash_map<dex::u4, std::vector<std::string_view>> strings_map;
    for (auto i = 0; i < this->strings.size(); ++i) {
        std::string_view string = this->strings[i];
        auto hits = acTrie.ParseText(string);
        for (auto &hit : hits) {
            auto match_type = match_type_map[hit.value];
            bool match = false;
            switch (match_type) {
                case schema::StringMatchType::Contains:
                    match = true;
                    break;
                case schema::StringMatchType::StartWith:
                    if (hit.begin == 0) {
                        match = true;
                    }
                    break;
                case schema::StringMatchType::EndWith:
                    if (hit.end == string.size()) {
                        match = true;
                    }
                    break;
                case schema::StringMatchType::Equal:
                    if (hit.begin == 0 && hit.end == string.size()) {
                        match = true;
                    }
                    break;
                default:
                    break;
            }
            if (match) {
                if (strings_map.contains(i)) {
                    strings_map[i].emplace_back(hit.value);
                } else {
                    strings_map[i] = {hit.value};
                }
            }
        }
    }

    if (strings_map.empty()) {
        return {};
    }

    // TODO: check query->in_class

    std::optional<std::string_view> find_package;
    schema::StringMatchType package_match_type;
    auto find_package_name = query->find_package_name();
    if (find_package_name) {
        // TODO: matcher
        find_package = find_package_name->value()->string_view();
        package_match_type = find_package_name->type();
    }

    std::map<std::string_view, std::vector<dex::u4>> find_result;
    for (int class_idx = 0; class_idx < this->type_names.size(); ++class_idx) {
        auto class_name = type_names[class_idx];
        if (class_method_ids[class_idx].empty()) {
            continue;
        }
        std::set<std::string_view> search_set;
        for (auto method_idx : class_method_ids[class_idx]) {
            auto code = this->method_codes[method_idx];
            if (code == nullptr) {
                continue;
            }
            auto p = code->insns;
            auto end_p = p + code->insns_size;
            while (p < end_p) {
                auto op = *p & 0xff;
                auto ptr = p;
                auto width = GetBytecodeWidth(ptr++);
                switch (op) {
                    case 0x1a: {
                        auto string_idx = ReadShort(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string : strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    case 0x1b: {
                        auto string_idx = ReadInt(ptr);
                        if (strings_map.contains(string_idx)) {
                            for (auto &string : strings_map[string_idx]) {
                                search_set.emplace(string);
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
                p += width;
            }
        }
        if (search_set.empty()) continue;
        auto matchers = query->matchers();
        for (int i = 0; i < matchers->size(); ++i) {
            std::set<std::string_view> matched_set;
            auto matcher = matchers->Get(i);
            for (int j = 0; j < matcher->using_strings()->size(); ++j) {
                matched_set.emplace(matcher->using_strings()->Get(j)->value()->string_view());
            }
            std::vector<std::string_view> vec;
            std::set_intersection(search_set.begin(), search_set.end(),
                                  matched_set.begin(), matched_set.end(),
                                  std::inserter(vec, vec.begin()));
            if (vec.size() == matched_set.size()) {
                find_result[matcher->union_key()->string_view()].emplace_back(class_idx);
            }
        }
    }

    std::vector<BatchFindClassItemBean> result;
    for (auto &[key, values] : find_result) {
        std::vector<ClassBean> classes;
        for (auto id : values) {
            classes.emplace_back(this->GetClassBean(id));
        }
        BatchFindClassItemBean itemBean;
        itemBean.union_key = key;
        itemBean.classes =classes;
        result.emplace_back(itemBean);
    }

    return result;
}

std::vector<BatchFindMethodItemBean>
DexItem::BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query) {
    return {};
}

}