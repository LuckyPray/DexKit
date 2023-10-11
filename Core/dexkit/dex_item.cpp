#include "dex_item.h"

#include "utils/byte_code_util.h"
#include "utils/opcode_util.h"
#include "utils/dex_descriptor_util.h"

namespace dexkit {

DexItem::DexItem(uint32_t id, uint8_t *data, size_t size, DexKit *dexkit) :
        _image(std::make_unique<MemMap>(data, size)),
        dexkit(dexkit),
        reader(_image->addr(), _image->len()),
        dex_id(id) {
    InitBaseCache();
}

DexItem::DexItem(uint32_t id, std::unique_ptr<MemMap> mmap, DexKit *dexkit) :
        _image(std::move(mmap)),
        dexkit(dexkit),
        reader(_image->addr(), _image->len()),
        dex_id(id) {
    InitBaseCache();
}

void DexItem::InitBaseCache() {
    strings.resize(reader.StringIds().size());
    auto strings_it = strings.begin();
    for (auto &str: reader.StringIds()) {
        auto *str_ptr = reader.dataPtr<dex::u1>(str.string_data_off);
        ReadULeb128(&str_ptr);
        *strings_it++ = reinterpret_cast<const char *>(str_ptr);
    }
    if (strings[0].empty()) {
        empty_string_id = 0;
    }

    type_names.resize(reader.TypeIds().size());
    type_name_array_count.resize(reader.TypeIds().size());
    auto type_names_it = type_names.begin();
    int idx = 0;
    for (auto &type_id: reader.TypeIds()) {
        *type_names_it = strings[type_id.descriptor_idx];
        auto array_count = type_names_it->find_first_not_of('[');
        DEXKIT_CHECK(array_count != std::string::npos);
        type_name_array_count[idx] = array_count;
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

    uint32_t retention_policy_type_idx = dex::kNoIndex;
    auto retention_policy_class_desc = "Ljava/lang/annotation/RetentionPolicy;";
    auto annotation_retention_class_desc = "Ljava/lang/annotation/Retention;";
    if (type_ids_map.contains(retention_policy_class_desc)) {
        retention_policy_type_idx = type_ids_map[retention_policy_class_desc];
    }
    if (type_ids_map.contains(annotation_retention_class_desc)) {
        annotation_retention_class_id = type_ids_map[annotation_retention_class_desc];
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
        } else if (field.class_idx == retention_policy_type_idx) {
            auto name = strings[field.name_idx];
            if (name == "SOURCE") {
                retention_map[field_idx] = schema::RetentionPolicyType::Source;
            } else if (name == "CLASS") {
                retention_map[field_idx] = schema::RetentionPolicyType::Class;
            } else if (name == "RUNTIME") {
                retention_map[field_idx] = schema::RetentionPolicyType::Runtime;
            }
        }
        class_field_ids[field.class_idx].emplace_back(field_idx);
        ++field_idx;
    }

    type_def_flag.resize(reader.TypeIds().size());
    type_def_idx.resize(reader.TypeIds().size());
    class_source_files.resize(reader.TypeIds().size());
    class_access_flags.resize(reader.TypeIds().size());
    class_interface_ids.resize(reader.TypeIds().size());
    class_method_ids.resize(reader.TypeIds().size());
    method_descriptors.resize(reader.MethodIds().size());
    method_access_flags.resize(reader.MethodIds().size());
    method_codes.resize(reader.MethodIds().size());
    field_descriptors.resize(reader.FieldIds().size());
    field_access_flags.resize(reader.FieldIds().size());

    method_cross_info.resize(reader.MethodIds().size());
    field_cross_info.resize(reader.FieldIds().size());

    auto class_def_idx = 0;
    for (auto &class_def: reader.ClassDefs()) {
        auto def_idx = class_def_idx++;
        if (class_def.source_file_idx != dex::kNoIndex) {
            class_source_files[class_def.class_idx] = strings[class_def.source_file_idx];
        }
        type_def_flag[class_def.class_idx] = true;
        type_def_idx[class_def.class_idx] = def_idx;
        class_access_flags[class_def.class_idx] = class_def.access_flags;
        if (class_def.class_data_off == 0) {
            continue;
        }

        if (class_def.interfaces_off) {
            auto interface_type_list = this->reader.dataPtr<dex::TypeList>(class_def.interfaces_off);
            if (interface_type_list != nullptr) {
                auto &interfaces = this->class_interface_ids[class_def.class_idx];
                interfaces.reserve(interface_type_list->size);
                for (auto i = 0; i < interface_type_list->size; ++i) {
                    interfaces.emplace_back(interface_type_list->list[i].type_idx);
                }
            }
        }

        const auto *class_data = reader.dataPtr<dex::u1>(class_def.class_data_off);
        uint32_t static_fields_size = ReadULeb128(&class_data);
        uint32_t instance_fields_count = ReadULeb128(&class_data);
        uint32_t direct_methods_count = ReadULeb128(&class_data);
        uint32_t virtual_methods_count = ReadULeb128(&class_data);

        auto &methods = class_method_ids[class_def.class_idx];

        for (uint32_t i = 0, class_field_idx = 0; i < static_fields_size; ++i) {
            class_field_idx += ReadULeb128(&class_data);
            field_access_flags[class_field_idx] = ReadULeb128(&class_data);
        }

        for (uint32_t i = 0, class_field_idx = 0; i < instance_fields_count; ++i) {
            class_field_idx += ReadULeb128(&class_data);
            field_access_flags[class_field_idx] = ReadULeb128(&class_data);
        }

        for (uint32_t i = 0, class_method_idx = 0; i < direct_methods_count; ++i) {
            class_method_idx += ReadULeb128(&class_data);
            method_access_flags[class_method_idx] = ReadULeb128(&class_data);
            uint32_t code_off = ReadULeb128(&class_data);
            if (code_off) {
                method_codes[class_method_idx] = reader.dataPtr<const dex::Code>(code_off);
            }
            methods.emplace_back(class_method_idx);
        }
        for (uint32_t i = 0, class_method_idx = 0; i < virtual_methods_count; ++i) {
            class_method_idx += ReadULeb128(&class_data);
            method_access_flags[class_method_idx] = ReadULeb128(&class_data);
            uint32_t code_off = ReadULeb128(&class_data);
            if (code_off) {
                method_codes[class_method_idx] = reader.dataPtr<const dex::Code>(code_off);
            }
            methods.emplace_back(class_method_idx);
        }
    }
    {
        static std::mutex put_declare_class_mutex;
        std::lock_guard<std::mutex> lock(put_declare_class_mutex);
        for (auto &class_def: reader.ClassDefs()) {
            dexkit->PutDeclaredClass(type_names[class_def.class_idx], dex_id, class_def.class_idx);
        }
    }
}

bool DexItem::NeedInitCache(uint32_t need_flag) const {
    return (dex_flag & need_flag) != need_flag;
}

void DexItem::InitCache(uint32_t init_flags) {
    if ((dex_flag & init_flags) == init_flags) {
        return;
    }
    bool need_foreach_method = false;
    bool need_op_seq = init_flags & kOpSequence && (dex_flag & kOpSequence) == 0;
    bool need_method_using_string = init_flags & kUsingString && (dex_flag & kUsingString) == 0;
    bool need_method_using_field = init_flags & kMethodUsingField && (dex_flag & kMethodUsingField) == 0;
    bool need_method_invoking = init_flags & kMethodInvoking && (dex_flag & kMethodInvoking) == 0;
    bool need_method_caller = init_flags & kCallerMethod && (dex_flag & kCallerMethod) == 0;
    bool need_field_rw_method = init_flags & kRwFieldMethod && (dex_flag & kRwFieldMethod) == 0;
    bool need_annotation = init_flags & kAnnotation && (dex_flag & kAnnotation) == 0;
    bool need_method_using_number = init_flags & kUsingNumber && (dex_flag & kUsingNumber) == 0;

    if (need_op_seq) {
        method_opcode_seq.resize(reader.MethodIds().size(), std::nullopt);
        need_foreach_method = true;
    }
    if (need_method_invoking) {
        method_invoking_ids.resize(reader.MethodIds().size());
        need_foreach_method = true;
    }
    if (need_method_caller) {
        method_caller_ids.resize(reader.MethodIds().size());
        need_foreach_method = true;
    }
    if (need_method_using_string) {
        method_using_string_ids.resize(reader.MethodIds().size());
        need_foreach_method = true;
    }
    if (need_method_using_field) {
        method_using_field_ids.resize(reader.MethodIds().size());
        need_foreach_method = true;
    }
    if (need_field_rw_method) {
        field_get_method_ids.resize(reader.FieldIds().size());
        field_put_method_ids.resize(reader.FieldIds().size());
        need_foreach_method = true;
    }

    if (need_foreach_method) {
        for (auto &class_def: reader.ClassDefs()) {
            for (auto method_id: class_method_ids[class_def.class_idx]) {
                auto code = method_codes[method_id];
                if (code == nullptr) {
                    continue;
                }

                std::optional<std::vector<uint8_t>> *op_seq_ptr = nullptr;
                std::vector<uint32_t> *method_using_string_ptr = nullptr;
                std::vector<std::pair<uint32_t, bool>> *method_using_field_ptr = nullptr;
                std::vector<uint32_t> *method_invoking_ptr = nullptr;

                if (need_op_seq) {
                    op_seq_ptr = &method_opcode_seq[method_id];
                    *op_seq_ptr = std::vector<uint8_t>();
                }
                if (need_method_using_string) {
                    method_using_string_ptr = &method_using_string_ids[method_id];
                }
                if (need_method_using_field) {
                    method_using_field_ptr = &method_using_field_ids[method_id];
                }
                if (need_method_invoking) {
                    method_invoking_ptr = &method_invoking_ids[method_id];
                }

                auto p = code->insns;
                auto end_p = p + code->insns_size;
                while (p < end_p) {
                    auto op = (uint8_t) *p;
                    if (need_op_seq) {
                        op_seq_ptr->emplace(op);
                    }
                    auto ptr = p;
                    auto width = GetBytecodeWidth(ptr++);
                    auto op_format = ins_formats[op];

                    if (need_method_using_string) {
                        if (op == 0x1a) { // const-string
                            auto index = ReadShort(ptr);
                            method_using_string_ptr->emplace_back(index);
                        } else if (op == 0x1b) { // const-string-jumbo
                            auto index = ReadInt(ptr);
                            method_using_string_ptr->emplace_back(index);
                        }
                    }

                    if (need_method_using_field) {
                        if (op >= 0x52 && op <= 0x6d) {
                            // iget, iget-wide, iget-object, iget-boolean, iget-byte, iget-char, iget-short
                            // sget, sget-wide, sget-object, sget-boolean, sget-byte, sget-char, sget-short
                            auto is_getter = ((op >= 0x52 && op <= 0x58) ||
                                              (op >= 0x60 && op <= 0x66));
                            // iput, iput-wide, iput-object, iput-boolean, iput-byte, iput-char, iput-short
                            // sput, sput-wide, sput-object, sput-boolean, sput-byte, sput-char, sput-short
                            auto is_setter = ((op >= 0x59 && op <= 0x5f) ||
                                              (op >= 0x67 && op <= 0x6d));
                            auto index = ReadShort(ptr);
                            method_using_field_ptr->emplace_back(index, is_getter);
                        }
                    }

                    if (need_method_invoking) {
                        if ((op >= 0x6e && op <= 0x72) // invoke-kind
                            || (op >= 0x74 && op <= 0x78)) { // invoke-kind/range
                            auto index = ReadShort(ptr);
                            method_invoking_ptr->emplace_back(index);
                        }
                    }
                    p += width;
                }
            }
        }
    }

    if (need_method_caller) {
        for (auto &class_def: reader.ClassDefs()) {
            for (auto method_id: class_method_ids[class_def.class_idx]) {
                for (auto invoke_id: method_invoking_ids[method_id]) {
                    method_caller_ids[invoke_id].emplace_back(dex_id, method_id);
                }
            }
        }
    }

    if (need_field_rw_method) {
        for (auto &class_def: reader.ClassDefs()) {
            for (auto method_id: class_method_ids[class_def.class_idx]) {
                for (auto &field_using: method_using_field_ids[method_id]) {
                    auto field_id = field_using.first;
                    auto is_getter = field_using.second;
                    if (is_getter) {
                        field_get_method_ids[field_id].emplace_back(dex_id, method_id);
                    } else {
                        field_put_method_ids[field_id].emplace_back(dex_id, method_id);
                    }
                }
            }
        }
    }

    if (need_annotation) {
        class_annotations.resize(reader.TypeIds().size());
        field_annotations.resize(reader.FieldIds().size());
        method_annotations.resize(reader.MethodIds().size());
        method_parameter_annotations.resize(reader.MethodIds().size());
        for (auto &class_def: reader.ClassDefs()) {
            if (class_def.annotations_off == 0) {
                continue;
            }
            auto annotations = reader.ExtractAnnotations(class_def.annotations_off);
            if (annotations->class_annotation) {
                auto &class_annotation = class_annotations[class_def.class_idx];
                for (auto &annotation: annotations->class_annotation->annotations) {
                    class_annotation.emplace_back(annotation);
                }
            }
            for (auto value: annotations->field_annotations) {
                auto &field_annotation = field_annotations[value->field_decl->orig_index];
                for (auto &annotation: value->annotations->annotations) {
                    field_annotation.emplace_back(annotation);
                }
            }
            for (auto value: annotations->method_annotations) {
                auto &method_annotation = method_annotations[value->method_decl->orig_index];
                for (auto &annotation: value->annotations->annotations) {
                    method_annotation.emplace_back(annotation);
                }
            }
            for (auto value: annotations->param_annotations) {
                auto &method_parameter_annotation = method_parameter_annotations[value->method_decl->orig_index];
                for (auto &annotation: value->annotations->annotations) {
                    std::vector<ir::Annotation *> ann_vec;
                    ann_vec.reserve(annotation->annotations.size());
                    for (auto &parameter_annotation: annotation->annotations) {
                        ann_vec.emplace_back(parameter_annotation);
                    }
                    method_parameter_annotation.emplace_back(ann_vec);
                }
            }
        }
    }
    dex_flag |= init_flags;
}

bool DexItem::NeedPutCrossRef(uint32_t need_cross_flag) const {
    DEXKIT_CHECK(((dex_cross_flag | kCallerMethod | kRwFieldMethod) ^ (kCallerMethod | kRwFieldMethod)) == 0);
    return (dex_cross_flag & need_cross_flag) != need_cross_flag;
}

void DexItem::PutCrossRef(uint32_t put_cross_flag) {
    DEXKIT_CHECK(((put_cross_flag | kCallerMethod | kRwFieldMethod) ^ (kCallerMethod | kRwFieldMethod)) == 0);
    if ((dex_cross_flag & put_cross_flag) == put_cross_flag) {
        return;
    }
    bool need_caller_cross = put_cross_flag & kCallerMethod && (dex_cross_flag & kCallerMethod) == 0;
    bool need_rw_field_cross = put_cross_flag & kRwFieldMethod && (dex_cross_flag & kRwFieldMethod) == 0;

    for (int type_idx = 0; type_idx < type_names.size(); ++type_idx) {
        if (!this->type_def_flag[type_idx] && type_names[type_idx][0] != '[') {
            auto declared_pair = dexkit->GetClassDeclaredPair(type_names[type_idx]);
            auto origin_dex = declared_pair.first;
            auto origin_type_idx = declared_pair.second;

            // no declared in any dex
            if (origin_dex == nullptr) {
                continue;
            }
            auto &mutex = origin_dex->GetTypeDefMutex(origin_type_idx);
            std::lock_guard lock(mutex);

            if (need_caller_cross) {
                std::vector<uint32_t> method_ids;
                std::swap(method_ids, this->class_method_ids[type_idx]);

                auto &origin_method_ids = origin_dex->class_method_ids[origin_type_idx];
                for (int ori_i = 0, cur_i = 0; ori_i < origin_method_ids.size() && cur_i < method_ids.size(); ++ori_i) {
                    auto origin_method_idx = origin_method_ids[ori_i];
                    auto curr_method_idx = method_ids[cur_i];
                    auto origin_method_descriptor = origin_dex->GetMethodDescriptor(origin_method_idx);
                    auto curr_method_descriptor = this->GetMethodDescriptor(curr_method_idx);
                    if (curr_method_descriptor != origin_method_descriptor) {
                        continue;
                    }
                    method_cross_info[curr_method_idx] = {origin_dex->dex_id, origin_method_idx};
                    auto &origin_caller_id = origin_dex->method_caller_ids[origin_method_idx];
                    auto &curr_caller_id = this->method_caller_ids[curr_method_idx];
                    origin_caller_id.insert(origin_caller_id.end(), curr_caller_id.begin(), curr_caller_id.end());
                    ++cur_i;
                }
            }

            if (need_rw_field_cross) {
                std::vector<uint32_t> field_ids;
                std::swap(field_ids, this->class_field_ids[type_idx]);

                auto &origin_field_ids = origin_dex->class_field_ids[origin_type_idx];
                for (int ori_i = 0, cur_i = 0; ori_i < origin_field_ids.size() && cur_i < field_ids.size(); ++ori_i) {
                    auto origin_field_idx = origin_field_ids[ori_i];
                    auto curr_field_idx = field_ids[cur_i];
                    auto origin_field_descriptor = origin_dex->GetFieldDescriptor(origin_field_idx);
                    auto curr_field_descriptor = this->GetFieldDescriptor(curr_field_idx);
                    if (origin_field_descriptor != curr_field_descriptor) {
                        continue;
                    }
                    field_cross_info[curr_field_idx] = {origin_dex->dex_id, origin_field_idx};
                    auto &origin_get_method_id = origin_dex->field_get_method_ids[origin_field_idx];
                    auto &curr_get_method_id = this->field_get_method_ids[curr_field_idx];
                    origin_get_method_id.insert(origin_get_method_id.end(), curr_get_method_id.begin(),
                                                curr_get_method_id.end());
                    auto &origin_put_method_id = origin_dex->field_put_method_ids[origin_field_idx];
                    auto &curr_put_method_id = this->field_put_method_ids[curr_field_idx];
                    origin_put_method_id.insert(origin_put_method_id.end(), curr_put_method_id.begin(),
                                                curr_put_method_id.end());
                    ++cur_i;
                }
            }
        }
    }
    dex_cross_flag |= put_cross_flag;
}

std::mutex &DexItem::GetTypeDefMutex(uint32_t type_idx) {
    return (*type_def_mutexes)[type_idx % type_def_mutexes->size()];
}

// NOLINTNEXTLINE
ClassBean DexItem::GetClassBean(uint32_t type_idx) {
    if (!this->type_def_flag[type_idx]) {
        auto pair = dexkit->GetClassDeclaredPair(this->type_names[type_idx]);
        if (pair.first) {
            return pair.first->GetClassBean(pair.second);
        }
    }
    ClassBean bean;
    bean.id = type_idx;
    bean.dex_id = this->dex_id;
    bean.dex_descriptor = this->type_names[type_idx];
    if (this->type_def_flag[type_idx]) {
        auto &class_def = this->reader.ClassDefs()[this->type_def_idx[type_idx]];
        bean.source_file = this->class_source_files[type_idx];
        bean.access_flags = class_def.access_flags;
        bean.super_class_id = class_def.superclass_idx;
        if (class_def.interfaces_off) {
            auto interface_type_list = this->reader.dataPtr<dex::TypeList>(class_def.interfaces_off);
            for (auto i = 0; i < interface_type_list->size; ++i) {
                bean.interface_ids.push_back(interface_type_list->list[i].type_idx);
            }
        }
        bean.field_ids = this->class_field_ids[type_idx];
        bean.method_ids = this->class_method_ids[type_idx];
    }
    return bean;
}

// NOLINTNEXTLINE
MethodBean DexItem::GetMethodBean(uint32_t method_idx) {
    auto &method_def = this->reader.MethodIds()[method_idx];
    if (!this->type_def_flag[method_def.class_idx]) {
        auto cross_info = this->method_cross_info[method_idx];
        if (cross_info.has_value()) {
            return this->dexkit->GetDexItem(cross_info->first)->GetMethodBean(cross_info->second);
        }
    }
    auto &proto_def = this->reader.ProtoIds()[method_def.proto_idx];
    auto &type_list = this->proto_type_list[method_def.proto_idx];
    MethodBean bean;
    bean.id = method_idx;
    bean.dex_id = this->dex_id;
    bean.class_id = method_def.class_idx;
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

// NOLINTNEXTLINE
FieldBean DexItem::GetFieldBean(uint32_t field_idx) {
    auto &field_def = this->reader.FieldIds()[field_idx];
    if (!this->type_def_flag[field_def.class_idx]) {
        auto cross_info = this->field_cross_info[field_idx];
        if (cross_info.has_value()) {
            return this->dexkit->GetDexItem(cross_info->first)->GetFieldBean(cross_info->second);
        }
    }
    FieldBean bean;
    bean.id = field_idx;
    bean.dex_id = this->dex_id;
    bean.class_id = field_def.class_idx;
    bean.access_flags = this->field_access_flags[field_idx];
    bean.dex_descriptor = this->GetFieldDescriptor(field_idx);
    bean.type_id = field_def.type_idx;
    return bean;
}

std::optional<MethodBean> DexItem::GetMethodBean(uint32_t type_idx, std::string_view method_descriptor) {
    auto &methods = this->class_method_ids[type_idx];
    for (auto method_idx: methods) {
        if (this->GetMethodDescriptor(method_idx) == method_descriptor) {
            return this->GetMethodBean(method_idx);
        }
    }
    return std::nullopt;
}

std::optional<FieldBean> DexItem::GetFieldBean(uint32_t type_idx, std::string_view method_descriptor) {
    auto &fields = this->class_field_ids[type_idx];
    for (auto field_idx: fields) {
        if (this->GetFieldDescriptor(field_idx) == method_descriptor) {
            return this->GetFieldBean(field_idx);
        }
    }
    return std::nullopt;
}

// NOLINTNEXTLINE
AnnotationBean DexItem::GetAnnotationBean(ir::Annotation *annotation) {
    AnnotationBean bean;
    bean.dex_id = this->dex_id;
    bean.type_id = annotation->type->orig_index;
    bean.type_descriptor = type_names[annotation->type->orig_index];
    bean.visibility = ((int8_t) annotation->visibility == -1)
            ? schema::AnnotationVisibilityType::None
            : (schema::AnnotationVisibilityType) annotation->visibility;
    for (auto &element : annotation->elements) {
        bean.elements.emplace_back(GetAnnotationElementBean(element));
    }
    return bean;
}

// NOLINTNEXTLINE
AnnotationEncodeValueBean DexItem::GetAnnotationEncodeValueBean(ir::EncodedValue *encoded_value) {
    AnnotationEncodeValueBean bean;
    switch (encoded_value->type) {
        case 0x00: bean.type = schema::AnnotationEncodeValueType::ByteValue; break;
        case 0x02: bean.type = schema::AnnotationEncodeValueType::ShortValue; break;
        case 0x03: bean.type = schema::AnnotationEncodeValueType::CharValue; break;
        case 0x04: bean.type = schema::AnnotationEncodeValueType::IntValue; break;
        case 0x06: bean.type = schema::AnnotationEncodeValueType::LongValue; break;
        case 0x10: bean.type = schema::AnnotationEncodeValueType::FloatValue; break;
        case 0x11: bean.type = schema::AnnotationEncodeValueType::DoubleValue; break;
        case 0x17: bean.type = schema::AnnotationEncodeValueType::StringValue; break;
        case 0x18: bean.type = schema::AnnotationEncodeValueType::TypeValue; break;
        case 0x1a: bean.type = schema::AnnotationEncodeValueType::MethodValue; break;
        case 0x1b: bean.type = schema::AnnotationEncodeValueType::EnumValue; break;
        case 0x1c: bean.type = schema::AnnotationEncodeValueType::ArrayValue; break;
        case 0x1d: bean.type = schema::AnnotationEncodeValueType::AnnotationValue; break;
        case 0x1e: bean.type = schema::AnnotationEncodeValueType::NullValue; break;
        case 0x1f: bean.type = schema::AnnotationEncodeValueType::BoolValue; break;
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
        case 0x1a: bean.value = std::make_unique<MethodBean>(GetMethodBean(encoded_value->u.method_value->orig_index)); break;
        case 0x1b: bean.value = std::make_unique<FieldBean>(GetFieldBean(encoded_value->u.enum_value->orig_index)); break;
        case 0x1c: bean.value = std::make_unique<AnnotationEncodeArrayBean>(GetAnnotationEncodeArrayBean(encoded_value->u.array_value)); break;
        case 0x1d: bean.value = std::make_unique<AnnotationBean>(GetAnnotationBean(encoded_value->u.annotation_value)); break;
        case 0x1e: bean.value = 0; break;
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
    if (this->class_annotations.empty()) {
        auto class_def = reader.ClassDefs()[type_def_idx[class_idx]];
        auto annotationsDirectory = reader.ExtractAnnotations(class_def.annotations_off);
        if (!annotationsDirectory) return {};
        auto annotationSet = annotationsDirectory->class_annotation
                             ? annotationsDirectory->class_annotation->annotations
                             : std::vector<ir::Annotation *>();
        std::vector<AnnotationBean> beans;
        for (auto annotation: annotationSet) {
            if (annotation->visibility == 2) {
                continue;
            }
            AnnotationBean bean = GetAnnotationBean(annotation);
            beans.emplace_back(std::move(bean));
        }
        return beans;
    }
    auto annotationSet = this->class_annotations[class_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet) {
        AnnotationBean bean = GetAnnotationBean(annotation);
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<AnnotationBean>
DexItem::GetMethodAnnotationBeans(uint32_t method_idx) {
    if (this->method_annotations.empty()) {
        auto method_def = reader.MethodIds()[method_idx];
        auto class_def = reader.ClassDefs()[type_def_idx[method_def.class_idx]];
        auto annotationsDirectory = reader.ExtractAnnotations(class_def.annotations_off);
        if (!annotationsDirectory) return {};
        for (auto ann_ptr: annotationsDirectory->method_annotations) {
            auto method_decl = ann_ptr->method_decl;
            if (method_decl->orig_index != method_idx) {
                continue;
            }
            auto annotationSet = ann_ptr->annotations
                                 ? ann_ptr->annotations->annotations
                                 : std::vector<ir::Annotation *>();
            std::vector<AnnotationBean> beans;
            for (auto annotation: annotationSet) {
                if (annotation->visibility == 2) {
                    continue;
                }
                AnnotationBean bean = GetAnnotationBean(annotation);
                beans.emplace_back(std::move(bean));
            }
            return beans;
        }
        return {};
    }
    auto annotationSet = this->method_annotations[method_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet) {
        AnnotationBean bean = GetAnnotationBean(annotation);
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<AnnotationBean>
DexItem::GetFieldAnnotationBeans(uint32_t field_idx) {
    if (field_annotations.empty()) {
        auto field_def = reader.FieldIds()[field_idx];
        auto class_def = reader.ClassDefs()[type_def_idx[field_def.class_idx]];
        auto annotationsDirectory = reader.ExtractAnnotations(class_def.annotations_off);
        if (!annotationsDirectory) return {};
        for (auto ann_ptr: annotationsDirectory->field_annotations) {
            auto field_decl = ann_ptr->field_decl;
            if (field_decl->orig_index != field_idx) {
                continue;
            }
            auto annotationSet = ann_ptr->annotations
                                 ? ann_ptr->annotations->annotations
                                 : std::vector<ir::Annotation *>();
            std::vector<AnnotationBean> beans;
            for (auto annotation: annotationSet) {
                if (annotation->visibility == 2) {
                    continue;
                }
                AnnotationBean bean = GetAnnotationBean(annotation);
                beans.emplace_back(std::move(bean));
            }
            return beans;
        }
        return {};
    }
    auto annotationSet = this->field_annotations[field_idx];
    std::vector<AnnotationBean> beans;
    for (auto annotation: annotationSet) {
        AnnotationBean bean = GetAnnotationBean(annotation);
        beans.emplace_back(std::move(bean));
    }
    return beans;
}

std::vector<std::vector<AnnotationBean>>
DexItem::GetParameterAnnotationBeans(uint32_t method_idx) {
    auto param_annotations = this->method_parameter_annotations[method_idx];
    std::vector<std::vector<AnnotationBean>> beans;
    for (auto &annotationSet: param_annotations) {
        std::vector<AnnotationBean> annotationBeans;
        for (auto annotation: annotationSet) {
            AnnotationBean bean = GetAnnotationBean(annotation);
            annotationBeans.emplace_back(std::move(bean));
        }
        beans.emplace_back(std::move(annotationBeans));
    }
    return beans;
}

std::optional<std::vector<std::optional<std::string_view>>>
DexItem::GetParameterNames(uint32_t method_idx) {
    auto code = method_codes[method_idx];
    if (code == nullptr || code->debug_info_off == 0) {
        return {};
    }
    auto *ptr = reader.dataPtr<dex::u1>(code->debug_info_off);
    ReadULeb128(&ptr); // line_start
    auto parameter_count = ReadULeb128(&ptr);
    std::vector<std::optional<std::string_view>> names;
    names.reserve(parameter_count);
    for (auto i = 0; i < parameter_count; ++i) {
        auto name_idx = ReadULeb128(&ptr) - 1;
        if (name_idx == dex::kNoIndex) {
            names.emplace_back(std::nullopt);
        } else {
            names.emplace_back(strings[name_idx]);
        }
    }
    return names;
}

std::vector<uint8_t>
DexItem::GetMethodOpCodes(uint32_t method_idx) {
    if (method_opcode_seq.empty()) {
        return GetOpSeqFromCode(method_idx);
    }
    auto &op_seq = method_opcode_seq[method_idx];
    return op_seq.has_value() ? op_seq.value() : std::vector<uint8_t>();
}

std::vector<MethodBean> DexItem::GetCallMethods(uint32_t method_idx) {
    auto &method_caller = this->method_caller_ids[method_idx];
    std::vector<MethodBean> beans;
    for (auto &[ori_dex_id, caller_id]: method_caller) {
        if (ori_dex_id == this->dex_id) {
            beans.emplace_back(GetMethodBean(caller_id));
        } else {
            auto dex = dexkit->GetDexItem(ori_dex_id);
            beans.emplace_back(dex->GetMethodBean(caller_id));
        }
    }
    return beans;
}

std::vector<MethodBean> DexItem::GetInvokeMethods(uint32_t method_idx) {
    std::vector<MethodBean> beans;
    if (method_invoking_ids.empty()) {
        auto method_invoking = GetInvokeMethodsFromCode(method_idx);
        for (auto invoking_id: method_invoking) {
            beans.emplace_back(GetMethodBean(invoking_id));
        }
    } else {
        auto &method_invoking = this->method_invoking_ids[method_idx];
        for (auto invoking_id: method_invoking) {
            beans.emplace_back(GetMethodBean(invoking_id));
        }
    }
    return beans;
}

std::vector<std::string_view> DexItem::GetUsingStrings(uint32_t method_idx) {
    std::vector<std::string_view> using_strings;
    if (method_using_string_ids.empty()) {
        auto method_using_strings = GetUsingStringsFromCode(method_idx);
        for (auto string_id: method_using_strings) {
            using_strings.emplace_back(this->strings[string_id]);
        }
    } else {
        auto &method_using_strings = method_using_string_ids[method_idx];
        for (auto string_id: method_using_strings) {
            using_strings.emplace_back(this->strings[string_id]);
        }
    }
    return using_strings;
}

std::vector<MethodBean> DexItem::FieldGetMethods(uint32_t field_idx) {
    auto &method_ids = this->field_get_method_ids[field_idx];
    std::vector<MethodBean> beans;
    for (auto &[ori_dex_id, method_id]: method_ids) {
        if (ori_dex_id == this->dex_id) {
            beans.emplace_back(GetMethodBean(method_id));
        } else {
            auto dex = dexkit->GetDexItem(ori_dex_id);
            beans.emplace_back(dex->GetMethodBean(method_id));
        }
    }
    return beans;
}

std::vector<MethodBean> DexItem::FieldPutMethods(uint32_t field_idx) {
    auto &method_ids = this->field_put_method_ids[field_idx];
    std::vector<MethodBean> beans;
    for (auto &[ori_dex_id, method_id]: method_ids) {
        if (ori_dex_id == this->dex_id) {
            beans.emplace_back(GetMethodBean(method_id));
        } else {
            auto dex = dexkit->GetDexItem(ori_dex_id);
            beans.emplace_back(dex->GetMethodBean(method_id));
        }
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

std::vector<uint8_t> DexItem::GetOpSeqFromCode(uint32_t method_idx) {
    auto code = method_codes[method_idx];
    if (code == nullptr) {
        return {};
    }
    std::vector<uint8_t> op_seq;
    auto p = code->insns;
    auto end_p = p + code->insns_size;
    while (p < end_p) {
        op_seq.emplace_back((uint8_t) *p);
        p += GetBytecodeWidth(p);
    }
    return std::move(op_seq);
}

std::vector<uint32_t> DexItem::GetUsingStringsFromCode(uint32_t method_idx) {
    auto code = method_codes[method_idx];
    if (code == nullptr) {
        return {};
    }
    std::vector<uint32_t> using_strings;
    auto p = code->insns;
    auto end_p = p + code->insns_size;
    while (p < end_p) {
        auto op = (uint8_t) *p;
        auto ptr = p;
        auto width = GetBytecodeWidth(ptr++);
        if (op == 0x1a) { // const-string
            auto index = ReadShort(ptr);
            using_strings.emplace_back(index);
        } else if (op == 0x1b) { // const-string-jumbo
            auto index = ReadInt(ptr);
            using_strings.emplace_back(index);
        }
        p += width;
    }
    return std::move(using_strings);
}

std::vector<uint32_t> DexItem::GetInvokeMethodsFromCode(uint32_t method_idx) {
    auto code = method_codes[method_idx];
    if (code == nullptr) {
        return {};
    }
    std::vector<uint32_t> invoke_methods;
    auto p = code->insns;
    auto end_p = p + code->insns_size;
    while (p < end_p) {
        auto op = (uint8_t) *p;
        auto ptr = p;
        auto width = GetBytecodeWidth(ptr++);
        auto op_format = ins_formats[op];
        if (op_format == dex::k35c // invoke-kind
            || op_format == dex::k3rc) { // invoke-kind/range
            auto index = ReadShort(ptr);
            invoke_methods.emplace_back(index);
        }
        p += width;
    }
    return std::move(invoke_methods);
}

std::vector<EncodeNumber> DexItem::GetUsingNumberFromCode(uint32_t method_idx) {
    auto code = method_codes[method_idx];
    if (code == nullptr) {
        return {};
    }
    std::vector<EncodeNumber> using_numbers;
    auto p = code->insns;
    auto end_p = p + code->insns_size;
    while (p < end_p) {
        auto op = (uint8_t) *p;
        auto ptr = p;
        auto width = GetBytecodeWidth(ptr++);
        auto op_format = ins_formats[op];
        switch (op_format) {
            // using number
            case dex::k11n: { // const/4
                uint8_t value = *(ptr - 1) >> 12;
                if (value & 0x8) {
                    value |= 0xf0;
                }
                using_numbers.emplace_back(EncodeNumber{.type = BYTE, .value = {.L8 = (int8_t) value}});
                break;
            }
            case dex::k21s: { // const/16, const-wide/16
                uint16_t value = *ptr;
                if (value & 0x8000) {
                    value |= 0xffff0000;
                }
                using_numbers.emplace_back(EncodeNumber{.type = SHORT, .value = {.L16 = (int16_t) value}});
                break;
            }
            case dex::k21h: { // const/high16, const-wide/high16
                if (op == 0x15) {
                    using_numbers.emplace_back(EncodeNumber{.type = FLOAT, .value = {.L32 = {.int_value = (int32_t) (*ptr << 16)}}});
                } else { // 0x19
                    using_numbers.emplace_back(EncodeNumber{.type = DOUBLE, .value = {.L64 = {.long_value = (int64_t) (((uint64_t) *ptr) << 48)}}});
                }
                break;
            }
            case dex::k31i: { // const, const-wide/32
                if (op == 0x14) {
                    using_numbers.emplace_back(EncodeNumber{.type = FLOAT, .value = {.L32 = {.int_value = (int32_t) ReadInt(ptr)}}});
                } else { // 0x17
                    using_numbers.emplace_back(EncodeNumber{.type = INT, .value = {.L32 = {.int_value = (int32_t) ReadInt(ptr)}}});
                }
                break;
            }
            case dex::k51l: // const-wide
                using_numbers.emplace_back(EncodeNumber{.type = LONG, .value = {.L64 = {.long_value = (int64_t) ReadLong(ptr)}}});
                break;
            case dex::k22s: // binop/lit16
                using_numbers.emplace_back(EncodeNumber{.type = SHORT, .value = {.L16 = (int16_t) *ptr}});
                break;
            case dex::k22b: // binop/lit8
                using_numbers.emplace_back(EncodeNumber{.type = BYTE, .value = {.L8 = (int8_t) (*ptr >> 8)}});
                break;
            default:
                break;
        }
        p += width;
    }
    return std::move(using_numbers);
}

bool DexItem::CheckAllTypeNamesDeclared(std::vector<std::string_view> &types) {
    for (auto &type: types) { // NOLINT
        if (!this->type_ids_map.contains(NameToDescriptor(type))) {
            return false;
        }
    }
    return true;
}

}