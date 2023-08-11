#include "dex_item.h"

namespace dexkit {

void ConvertSimilarRegex(std::string_view &str, schema::StringMatchType &type) {
    if (type == schema::StringMatchType::SimilarRegex) {
        type = schema::StringMatchType::Contains;
        if (str.front() == '^') {
            type = schema::StringMatchType::StartWith;
            str = str.substr(1);
        }
        if (str.back() == '$') {
            if (type == schema::StringMatchType::StartWith) {
                type = schema::StringMatchType::Equal;
            } else {
                type = schema::StringMatchType::EndWith;
            }
            str = str.substr(0, str.size() - 1);
        }
    }
}

bool DexItem::IsStringMatched(std::string_view str, const schema::StringMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto match_type = matcher->match_type();
    auto match_str = matcher->value()->string_view();
    ConvertSimilarRegex(match_str, match_type);
    DEXKIT_CHECK(match_type == schema::StringMatchType::Contains && match_str.empty());
    auto index = kmp::FindIndex(str, match_str, matcher->ignore_case());
    switch (match_type) {
        case schema::StringMatchType::Contains: return index != -1;
        case schema::StringMatchType::StartWith: return index == 0;
        case schema::StringMatchType::EndWith: return index == str.size() - matcher->value()->string_view().size();
        case schema::StringMatchType::Equal: return index == 0 && str.size() == matcher->value()->string_view().size();
        case schema::StringMatchType::SimilarRegex: abort();
    }
}

bool DexItem::IsAccessFlagsMatched(uint32_t access_flags, const schema::AccessFlagsMatcher *matcher) {
    DEXKIT_CHECK(matcher->flags() == 0);
    switch (matcher->match_type()) {
        case schema::MatchType::Equal: return access_flags == matcher->flags();
        case schema::MatchType::Contain: return (access_flags & matcher->flags()) == matcher->flags();
    }
}

bool DexItem::IsAnnotationsMatched(ir::AnnotationSet *annotationSet, const schema::AnnotationsMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsAnnotationElementsMatched(ir::AnnotationElement *annotationElement, const schema::AnnotationElementsMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsClassMatched(uint32_t class_idx, const schema::ClassMatcher *matcher) {
    if (!IsClassSmaliSourceMatched(class_idx, matcher->smali_source())) {
        return false;
    }
    if (!IsClassNameMatched(class_idx, matcher->class_name())) {
        return false;
    }
    auto access_flags = this->class_access_flags[class_idx];
    if (!IsAccessFlagsMatched(access_flags, matcher->access_flags())) {
        return false;
    }
    auto &class_def = this->reader.ClassDefs()[this->type_id_class_id_map[class_idx]];
    auto super_class_idx = class_def.superclass_idx;
    if (!IsClassMatched(super_class_idx, matcher->super_class())) {
        return false;
    }
    if (!IsInterfacesMatched(class_idx, matcher->interfaces())) {
        return false;
    }
    if (!IsAnnotationsMatched(this->class_annotations[class_idx], matcher->annotations())) {
        return false;
    }
    if (!IsFieldsMatched(class_idx, matcher->fields())) {
        return false;
    }
    if (!IsMethodsMatched(class_idx, matcher->methods())) {
        return false;
    }
    // TODO using_strings
    return true;
}

bool DexItem::IsClassNameMatched(uint32_t class_idx, const schema::StringMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto type_name = this->type_names[class_idx];
    DEXKIT_CHECK(type_name[0] == 'L' && type_name.back() == ';');
    type_name = type_name.substr(1, type_name.size() - 2);

    auto match_str = matcher->value()->string_view();
    auto match_type = matcher->match_type();
    ConvertSimilarRegex(match_str, match_type);

    std::string match_name(match_str);
    std::replace(match_name.begin(), match_name.end(), '.', '/');
    auto index = kmp::FindIndex(type_name, match_name, matcher->ignore_case());
    switch (match_type) {
        case schema::StringMatchType::Contains: return index != -1;
        case schema::StringMatchType::StartWith: return index == 0;
        case schema::StringMatchType::EndWith: return index == type_name.size() - match_name.size();
        case schema::StringMatchType::Equal: return index == 0 && type_name.size() == match_name.size();
        case schema::StringMatchType::SimilarRegex: abort();
    }
}

bool DexItem::IsClassSmaliSourceMatched(uint32_t class_idx, const schema::StringMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto smali_source = this->class_source_files[class_idx];
    return IsStringMatched(smali_source, matcher);
}

bool DexItem::IsInterfacesMatched(uint32_t class_idx, const schema::InterfacesMatcher *matcher) {
    return true;
}

bool DexItem::IsFieldsMatched(uint32_t class_idx, const schema::FieldsMatcher *matcher) {
    return true;
}

bool DexItem::IsMethodsMatched(uint32_t class_idx, const schema::MethodsMatcher *matcher) {
    return true;
}

bool DexItem::IsMethodMatched(uint32_t method_idx, const schema::MethodMatcher *matcher) {
    auto &method_def = this->reader.MethodIds()[method_idx];
    auto method_name = this->type_names[method_def.name_idx];
    if (!IsStringMatched(method_name, matcher->method_name())) {
        return false;
    }
    if (!IsAccessFlagsMatched(this->method_access_flags[method_idx], matcher->access_flags())) {
        return false;
    }
    if (!IsClassMatched(method_def.class_idx, matcher->declaring_class())) {
        return false;
    }
    auto &proto_def = this->reader.ProtoIds()[method_def.proto_idx];
    if (!IsClassMatched(proto_def.return_type_idx, matcher->return_type())) {
        return false;
    }
    if (!IsParametersMatched(method_idx, matcher->parameters())) {
        return false;
    }
    if (!IsAnnotationsMatched(this->method_annotations[method_idx], matcher->annotations())) {
        return false;
    }
    if (!IsOpCodesMatched(method_idx, matcher->op_codes())) {
        return false;
    }
    if (!IsDexCodesMatched(method_idx, matcher->op_codes(), matcher->using_strings(), matcher->using_fiels(), matcher->using_numbers(), matcher->invoke_methods())) {
        return false;
    }
    if (!IsCallMethodsMatched(method_idx, matcher->call_methods())) {
        return false;
    }
    return true;
}

bool DexItem::IsParametersMatched(uint32_t method_idx, const schema::ParametersMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsOpCodesMatched(uint32_t method_idx, const schema::OpCodesMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsDexCodesMatched(
        uint32_t method_idx,
        const schema::OpCodesMatcher *op_codes_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::UsingFieldMatcher>> *using_fields_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::UsingNumberMatcher>> *using_numbers_matcher,
        const schema::MethodsMatcher *invoke_methods_matcher
) {
    // TODO
    return true;
}

bool DexItem::IsCallMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsFieldMatched(uint32_t field_idx, const schema::FieldMatcher *matcher) {
    auto &field_def = this->reader.FieldIds()[field_idx];
    auto field_name = this->type_names[field_def.name_idx];
    if (!IsStringMatched(field_name, matcher->field_name())) {
        return false;
    }
    if (!IsAccessFlagsMatched(this->field_access_flags[field_idx], matcher->access_flags())) {
        return false;
    }
    if (!IsClassMatched(field_def.class_idx, matcher->declaring_class())) {
        return false;
    }
    if (!IsClassMatched(field_def.type_idx, matcher->type_class())) {
        return false;
    }
    if (!IsAnnotationsMatched(this->field_annotations[field_idx], matcher->annotations())) {
        return false;
    }
    if (!IsFieldGetMethodsMatched(field_idx, matcher->get_methods())) {
        return false;
    }
    if (!IsFieldPutMethodsMatched(field_idx, matcher->put_methods())) {
        return false;
    }
    return true;
}

bool DexItem::IsFieldGetMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher) {
    // TODO
    return true;
}

bool DexItem::IsFieldPutMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher) {
    // TODO
    return true;
}

}