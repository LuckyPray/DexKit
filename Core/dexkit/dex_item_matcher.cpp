#include "dex_item.h"

namespace dexkit {

template<typename T, typename U>
class Hungarian {
private:
    std::vector<U> left;
    std::vector<T> right;
    std::vector<std::vector<bool>> map;
    std::vector<int> p;
    std::vector<bool> vis;
    std::function<bool(T, U)> judge;
public:
    Hungarian(const std::vector<T> &targets, const std::vector<U> &matchers, std::function<bool(T, U)> match) {
        this->left = matchers;
        this->right = targets;
        map.resize(left.size());
        for (int i = 0; i < left.size(); ++i) {
            map[i].resize(right.size());
        }
        p.resize(matchers.size());
        vis.resize(matchers.size());
        this->judge = match;
    }

    // NOLINTNEXTLINE
    bool dfs(int i) {
        for (int j = 0; j < right.size(); ++j) {
            if (!map[i][j]) map[i][j] = judge(right[j], left[i]);
            if (map[i][j] && !vis[j]) {
                vis[j] = true;
                if (!p[j] || dfs(p[j])) {
                    p[j] = i;
                    return true;
                }
            }
        }
        return false;
    }

    int solve() {
        int ans = 0;
        for (int i = 0; i < left.size(); ++i) {
            std::fill(vis.begin(), vis.end(), false);
            if (dfs(i)) {
                ans++;
            }
        }
        return ans;
    }

};

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

bool DexItem::IsAnnotationMatched(const ir::Annotation *annotation, const schema::AnnotationMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!IsClassNameMatched(annotation->type->orig_index, matcher->type_name())) {
        return false;
    }
    auto type_annotations = this->class_annotations[annotation->type->orig_index];
    if (matcher->target_element_types()) {
        // TODO TargetElementTypesMatcher
        return false;
    }
    if (!IsAnnotationsMatched(type_annotations, matcher->annotations())) {
        return false;
    }
    if (!IsAnnotationElementsMatched(annotation->elements, matcher->elements())) {
        return false;
    }
    return true;
}

bool DexItem::IsAnnotationsMatched(const ir::AnnotationSet *annotationSet, const schema::AnnotationsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto &annotations = annotationSet->annotations;
    if (matcher->annotation_count()) {
        if (annotations.size() < matcher->annotation_count()->min()
        || annotations.size() > matcher->annotation_count()->max()) {
            return false;
        }
    }
    if (matcher->annotations()) {
        static auto IsAnnotationMatched = [this](ir::Annotation *annotation, const schema::AnnotationMatcher *matcher) {
            return this->IsAnnotationMatched(annotation, matcher);
        };

        typedef std::vector<const schema::AnnotationMatcher *> AnnotationMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationMatcher *>(POINT_CASE(matcher->annotations()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::AnnotationMatcher *>();
            for (auto annotation : *matcher->annotations()) {
                vec->push_back(annotation);
            }
            ThreadVariable::SetThreadVariable<AnnotationMatcher *>(POINT_CASE(matcher->annotations()), vec);
            ptr = ThreadVariable::GetThreadVariable<AnnotationMatcher *>(POINT_CASE(matcher->annotations()));
        }

        auto annotation_matches = **ptr;
        Hungarian<ir::Annotation *, const schema::AnnotationMatcher *> hungarian(annotationSet->annotations, annotation_matches, IsAnnotationMatched);
        auto count = hungarian.solve();
        if (count != annotation_matches.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != annotationSet->annotations.size()) {
                return false;
            }
        }
    }
    return true;
}

static uint8_t AnnotationEncodeValueTypeCvt(schema::AnnotationEncodeValueMatcher value_type) {
    switch (value_type) {
        case schema::AnnotationEncodeValueMatcher::EncodeValueByte: return dex::kEncodedByte;
        case schema::AnnotationEncodeValueMatcher::EncodeValueShort: return dex::kEncodedShort;
        case schema::AnnotationEncodeValueMatcher::EncodeValueChar: return dex::kEncodedChar;
        case schema::AnnotationEncodeValueMatcher::EncodeValueInt: return dex::kEncodedInt;
        case schema::AnnotationEncodeValueMatcher::EncodeValueLong: return dex::kEncodedLong;
        case schema::AnnotationEncodeValueMatcher::EncodeValueFloat: return dex::kEncodedFloat;
        case schema::AnnotationEncodeValueMatcher::EncodeValueDouble: return dex::kEncodedDouble;
        case schema::AnnotationEncodeValueMatcher::StringMatcher: return dex::kEncodedString;
        case schema::AnnotationEncodeValueMatcher::ClassMatcher: return dex::kEncodedType;
        case schema::AnnotationEncodeValueMatcher::FieldMatcher: return dex::kEncodedEnum;
        case schema::AnnotationEncodeValueMatcher::AnnotationEncodeValuesMatcher: return dex::kEncodedArray;
        case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: return dex::kEncodedAnnotation;
        case schema::AnnotationEncodeValueMatcher::EncodeValueBoolean: return dex::kEncodedBoolean;
        default: abort();
    }
}

template<typename T>
T NonNullCase(const void *value) {
    if (value) {
        return static_cast<T>(value);
    }
    abort();
}

bool DexItem::IsAnnotationEncodeValueMatched(const ir::EncodedValue *encodedValue, const schema::AnnotationEncodeValueMatcher type, const void *value) {
    auto value_type = AnnotationEncodeValueTypeCvt(type);
    if (encodedValue->type != value_type) {
        return false;
    }
    switch (value_type) {
        case dex::kEncodedByte: return encodedValue->u.byte_value == NonNullCase<const dexkit::schema::EncodeValueByte *>(value)->value();
        case dex::kEncodedShort: return encodedValue->u.short_value == NonNullCase<const dexkit::schema::EncodeValueShort *>(value)->value();
        case dex::kEncodedChar: return encodedValue->u.char_value == NonNullCase<const dexkit::schema::EncodeValueChar *>(value)->value();
        case dex::kEncodedInt: return encodedValue->u.int_value == NonNullCase<const dexkit::schema::EncodeValueInt *>(value)->value();
        case dex::kEncodedLong: return encodedValue->u.long_value == NonNullCase<const dexkit::schema::EncodeValueLong *>(value)->value();
        case dex::kEncodedFloat: return encodedValue->u.float_value == NonNullCase<const dexkit::schema::EncodeValueFloat *>(value)->value();
        case dex::kEncodedDouble: return encodedValue->u.double_value == NonNullCase<const dexkit::schema::EncodeValueDouble *>(value)->value();
        case dex::kEncodedString: return IsStringMatched(encodedValue->u.string_value->c_str(), NonNullCase<const dexkit::schema::StringMatcher *>(value));
        case dex::kEncodedType: return IsClassMatched(encodedValue->u.type_value->orig_index, NonNullCase<const dexkit::schema::ClassMatcher *>(value));
        case dex::kEncodedEnum: return IsFieldMatched(encodedValue->u.enum_value->orig_index, NonNullCase<const dexkit::schema::FieldMatcher *>(value));
        case dex::kEncodedArray: return IsAnnotationEncodeValuesMatched(encodedValue->u.array_value->values, NonNullCase<const dexkit::schema::AnnotationEncodeValuesMatcher *>(value));
        case dex::kEncodedAnnotation: return IsAnnotationMatched(encodedValue->u.annotation_value, NonNullCase<const dexkit::schema::AnnotationMatcher *>(value));
        case dex::kEncodedBoolean: return encodedValue->u.bool_value == NonNullCase<const dexkit::schema::EncodeValueBoolean *>(value)->value();
        default: abort();
    }
}

bool DexItem::IsAnnotationEncodeValuesMatched(const std::vector<ir::EncodedValue *> &encodedValues, const dexkit::schema::AnnotationEncodeValuesMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (matcher->value_count()) {
        if (encodedValues.size() < matcher->value_count()->min()
        || encodedValues.size() > matcher->value_count()->max()) {
            return false;
        }
    }
    if (matcher->values()) {
        if (matcher->values()->size() > encodedValues.size()) {
            return false;
        }
        static auto IsAnnotationEncodeValueMatched = [this](const ir::EncodedValue *encodedValue, const std::pair<schema::AnnotationEncodeValueMatcher, const void *> &value) {
            return this->IsAnnotationEncodeValueMatched(encodedValue, value.first, value.second);
        };

        typedef std::vector<std::pair<schema::AnnotationEncodeValueMatcher, const void *>> AnnotationEncodeValueMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationEncodeValueMatcher *>(POINT_CASE(matcher->values()));
        if (ptr == nullptr) {
            auto values = new std::vector<std::pair<schema::AnnotationEncodeValueMatcher, const void *>>();
            for (auto i = 0; i < matcher->values()->size(); ++i) {
                auto type = matcher->values_type()->Get(i);
                auto value = matcher->values()->GetAs<void>(i);
                values->emplace_back(type, value);
            }
            ThreadVariable::SetThreadVariable<AnnotationEncodeValueMatcher *>(POINT_CASE(matcher), values);
            ptr = ThreadVariable::GetThreadVariable<AnnotationEncodeValueMatcher *>(POINT_CASE(matcher));
        }

        auto values = **ptr;
        Hungarian<ir::EncodedValue *, std::pair<schema::AnnotationEncodeValueMatcher, const void *>> hungarian(encodedValues, values, IsAnnotationEncodeValueMatched);
        int count = hungarian.solve();
        if (count != matcher->values()->size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != encodedValues.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsAnnotationElementMatched(const ir::AnnotationElement *annotationElement, const schema::AnnotationElementMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!IsStringMatched(annotationElement->name->c_str(), matcher->name())) {
        return false;
    }
    if (matcher->value()) {
        DEXKIT_CHECK(matcher->value_type() == schema::AnnotationEncodeValueMatcher::NONE);
        if (!IsAnnotationEncodeValueMatched(annotationElement->value, matcher->value_type(), matcher->value())) {
            return false;
        }
    }
    return true;
}

bool DexItem::IsAnnotationElementsMatched(const std::vector<ir::AnnotationElement *> &annotationElement, const schema::AnnotationEncodeArrayMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (matcher->element_count()) {
        if (annotationElement.size() < matcher->element_count()->min()
        || annotationElement.size() > matcher->element_count()->max()) {
            return false;
        }
    }
    if (matcher->elements()) {
        static auto IsAnnotationElementMatched = [this](const ir::AnnotationElement *annotationElement, const schema::AnnotationElementMatcher *matcher) {
            return this->IsAnnotationElementMatched(annotationElement, matcher);
        };

        typedef std::vector<const schema::AnnotationElementMatcher *> AnnotationElementMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationElementMatcher *>(POINT_CASE(matcher->elements()));
        if (ptr == nullptr) {
            auto matchers = new std::vector<const schema::AnnotationElementMatcher *>();
            for (auto element : *matcher->elements()) {
                matchers->push_back(element);
            }
            ThreadVariable::SetThreadVariable<std::vector<const schema::AnnotationElementMatcher *> *>(POINT_CASE(matcher->elements()), matchers);
            ptr = ThreadVariable::GetThreadVariable<std::vector<const schema::AnnotationElementMatcher *> *>(POINT_CASE(matcher->elements()));
        }

        auto matchers = **ptr;
        Hungarian<ir::AnnotationElement *, const schema::AnnotationElementMatcher *> hungarian(annotationElement, matchers, IsAnnotationElementMatched);
        auto count = hungarian.solve();
        if (count != matcher->elements()->size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != annotationElement.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsClassMatched(uint32_t class_idx, const schema::ClassMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
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
    if (matcher == nullptr) {
        return true;
    }
    const auto &interfaces = this->class_interface_ids[class_idx];
    if (matcher->interface_count()) {
        if (interfaces.size() < matcher->interface_count()->min()
        || interfaces.size() > matcher->interface_count()->max()) {
            return false;
        }
    }
    if (matcher->interfaces()) {
        static auto IsClassMatched = [this](uint32_t class_idx, const schema::ClassMatcher *matcher) {
            return this->IsClassMatched(class_idx, matcher);
        };

        typedef std::vector<const schema::ClassMatcher *> ClassMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<ClassMatcher *>(POINT_CASE(matcher->interfaces()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::ClassMatcher *>();
            for (auto interface_matcher : *matcher->interfaces()) {
                vec->push_back(interface_matcher);
            }
            ThreadVariable::SetThreadVariable<ClassMatcher *>(POINT_CASE(matcher->interfaces()), vec);
            ptr = ThreadVariable::GetThreadVariable<ClassMatcher *>(POINT_CASE(matcher->interfaces()));
        }

        auto interface_matchers = **ptr;
        Hungarian<uint32_t, const schema::ClassMatcher *> hungarian(interfaces, interface_matchers, IsClassMatched);
        auto count = hungarian.solve();
        if (count != interfaces.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != interface_matchers.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsFieldsMatched(uint32_t class_idx, const schema::FieldsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto &fields = this->class_field_ids[class_idx];
    if (matcher->field_count()) {
        if (fields.size() < matcher->field_count()->min()
        || fields.size() > matcher->field_count()->max()) {
            return false;
        }
    }
    if (matcher->fields()) {
        static auto IsFieldMatched = [this](uint32_t field_idx, const schema::FieldMatcher *matcher) {
            return this->IsFieldMatched(field_idx, matcher);
        };

        typedef std::vector<const schema::FieldMatcher *> FieldMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<FieldMatcher *>(POINT_CASE(matcher->fields()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::FieldMatcher *>();
            for (auto field_matcher : *matcher->fields()) {
                vec->push_back(field_matcher);
            }
            ThreadVariable::SetThreadVariable<FieldMatcher *>(POINT_CASE(matcher->fields()), vec);
            ptr = ThreadVariable::GetThreadVariable<FieldMatcher *>(POINT_CASE(matcher->fields()));
        }

        auto field_matchers = **ptr;
        Hungarian<uint32_t, const schema::FieldMatcher *> hungarian(fields, field_matchers, IsFieldMatched);
        auto count = hungarian.solve();
        if (count != fields.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != field_matchers.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsMethodsMatched(uint32_t class_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto &methods = this->class_method_ids[class_idx];
    if (matcher->method_count()) {
        if (methods.size() < matcher->method_count()->min()
        || methods.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        static auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::MethodMatcher *>();
            for (auto method_matcher : *matcher->methods()) {
                vec->push_back(method_matcher);
            }
            ThreadVariable::SetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()), vec);
            ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        }

        auto method_matchers = **ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(methods, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != methods.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != method_matchers.size()) {
                return false;
            }
        }
    }
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
    if (!IsDexCodesMatched(method_idx, matcher->op_codes(), matcher->using_strings(), matcher->using_fiels(), matcher->using_numbers(), matcher->invoking_methods())) {
        return false;
    }
    if (!IsCallMethodsMatched(method_idx, matcher->method_callers())) {
        return false;
    }
    return true;
}

bool DexItem::IsParametersMatched(uint32_t method_idx, const schema::ParametersMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto type_list = proto_type_list[method_idx];
    if (matcher->parameter_count()) {
        if (type_list->size < matcher->parameter_count()->min()
        || type_list->size > matcher->parameter_count()->max()) {
            return false;
        }
    }
    if (matcher->parameters()) {
        if (type_list->size != matcher->parameters()->size()) {
            return false;
        }
        auto &parameter_annotations = this->method_parameter_annotations[method_idx];
        for (size_t i = 0; i < type_list->size; ++i) {
            auto parameter_matcher = matcher->parameters()->Get(i);
            if (!IsClassMatched(type_list->list[i].type_idx, parameter_matcher->prameter_type())) {
                return false;
            }
            if (!IsAnnotationsMatched(parameter_annotations[i], parameter_matcher->annotations())) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsOpCodesMatched(uint32_t method_idx, const schema::OpCodesMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto &opt_opcodes = this->method_opcode_seq[method_idx];
    DEXKIT_CHECK(opt_opcodes == std::nullopt);
    const auto opcodes = opt_opcodes.value();
    if (matcher->op_code_count()) {
        if (opcodes.size() < matcher->op_code_count()->min()
        || opcodes.size() > matcher->op_code_count()->max()) {
            return false;
        }
    }
    if (matcher->op_codes()) {
        if (matcher->op_codes()->size() > opcodes.size()) {
            return false;
        }

        typedef std::vector<std::optional<uint8_t>> OpCodeMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<OpCodeMatcher *>(POINT_CASE(matcher->op_codes()));
        if (ptr == nullptr) {
            auto vec = new std::vector<std::optional<uint8_t>>();
            for (auto opcode : *matcher->op_codes()) {
                if (opcode < 0) {
                    vec->emplace_back(std::nullopt);
                } else {
                    vec->emplace_back(opcode);
                }
            }
            ThreadVariable::SetThreadVariable<OpCodeMatcher *>(POINT_CASE(matcher->op_codes()), vec);
            ptr = ThreadVariable::GetThreadVariable<OpCodeMatcher *>(POINT_CASE(matcher->op_codes()));
        }

        auto matcher_opcodes = **ptr;
        auto index = kmp::FindIndex(opcodes, matcher_opcodes);
        if (index == -1) {
            return false;
        }
        bool condition = false;
        switch (matcher->match_type()) {
            case schema::OpCodeMatchType::Equal: condition = index == 0 && matcher_opcodes.size() == opcodes.size(); break;
            case schema::OpCodeMatchType::StartWith: condition = index == 0; break;
            case schema::OpCodeMatchType::EndWith: condition = index + matcher_opcodes.size() == opcodes.size(); break;
            case schema::OpCodeMatchType::Contains: condition = true; break;
        }
        if (!condition) {
            return false;
        }
    }
    return true;
}

std::set<std::string_view> *
BuildBatchFindKeywordsMap(
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
        std::vector<std::pair<std::string_view, bool>> &keywords,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    auto result = new std::set<std::string_view>();
    for (int i = 0; i < using_strings_matcher->size(); ++i) {
        auto string_matcher = using_strings_matcher->Get(i);
        auto value = string_matcher->value()->string_view();
        auto type = string_matcher->match_type();
        auto ignore_case = string_matcher->ignore_case();
        if (type == schema::StringMatchType::SimilarRegex) {
            type = schema::StringMatchType::Contains;
            int l = 0, r = (int) value.size();
            if (value.starts_with('^')) {
                l = 1;
                type = schema::StringMatchType::StartWith;
            }
            if (value.ends_with('$')) {
                r = (int) value.size() - 1;
                if (type == schema::StringMatchType::StartWith) {
                    type = schema::StringMatchType::Equal;
                } else {
                    type = schema::StringMatchType::EndWith;
                }
            }
            value = value.substr(l, r - l);
        }
        result->insert(value);
        keywords.emplace_back(value, ignore_case);
        match_type_map[value] = type;
    }
    return result;
}

bool DexItem::IsDexCodesMatched(
        uint32_t method_idx,
        const schema::OpCodesMatcher *op_codes_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::UsingFieldMatcher>> *using_fields_matcher,
        const flatbuffers::Vector<flatbuffers::Offset<schema::UsingNumberMatcher>> *using_numbers_matcher,
        const schema::MethodsMatcher *invoke_methods_matcher
) {
    if (!IsOpCodesMatched(method_idx, op_codes_matcher)) {
        return false;
    }
    typedef acdat::AhoCorasickDoubleArrayTrie<std::string_view> KeywordTrie;
    typedef phmap::flat_hash_map<std::string_view, schema::StringMatchType> KeywordMap;
    KeywordTrie *acTrie;
    KeywordMap *match_type_map;
    std::set<std::string_view> *real_keywords;
    if (using_strings_matcher) {
        typedef std::tuple<KeywordTrie *, KeywordMap *, std::set<std::string_view> *> KeywordTrieMap;
        std::vector<std::pair<std::string_view, bool>> keywords;
        auto ptr = ThreadVariable::GetThreadVariable<KeywordTrieMap *>(POINT_CASE(using_strings_matcher));

        if (ptr == nullptr) {
            auto trie = new KeywordTrie();
            auto map = new KeywordMap();
            auto result = BuildBatchFindKeywordsMap(using_strings_matcher, keywords, *map);
            acdat::Builder<std::string_view>().Build(keywords, acTrie);
            auto pair = new KeywordTrieMap(trie, map, result);
            ThreadVariable::SetThreadVariable<KeywordTrieMap *>(POINT_CASE(using_strings_matcher), pair);
            ptr = ThreadVariable::GetThreadVariable<KeywordTrieMap *>(POINT_CASE(using_strings_matcher));
        }
        acTrie = std::get<0>(**ptr);
        match_type_map = std::get<1>(**ptr);
        real_keywords = std::get<2>(**ptr);
    }
    if (using_fields_matcher) {
        // TODO
    }
    if (using_numbers_matcher) {
        // TODO
    }
    return true;
}

bool DexItem::IsCallMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto &ids = method_caller_ids[method_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        static auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::MethodMatcher *>();
            for (auto method_matcher : *matcher->methods()) {
                vec->push_back(method_matcher);
            }
            ThreadVariable::SetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()), vec);
            ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        }

        auto method_matchers = **ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != method_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            return count == ids.size();
        }
    }
    return true;
}

bool DexItem::IsFieldMatched(uint32_t field_idx, const schema::FieldMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
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
    if (matcher == nullptr) {
        return true;
    }
    const auto ids = field_get_method_ids[field_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        static auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::MethodMatcher *>();
            for (auto method_matcher : *matcher->methods()) {
                vec->push_back(method_matcher);
            }
            ThreadVariable::SetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()), vec);
            ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        }

        auto method_matchers = **ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != method_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            return count == ids.size();
        }
    }
    return true;
}

bool DexItem::IsFieldPutMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto ids = field_put_method_ids[field_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        static auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = new std::vector<const schema::MethodMatcher *>();
            for (auto method_matcher : *matcher->methods()) {
                vec->push_back(method_matcher);
            }
            ThreadVariable::SetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()), vec);
            ptr = ThreadVariable::GetThreadVariable<MethodMatcher *>(POINT_CASE(matcher->methods()));
        }

        auto method_matchers = **ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != method_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            return count == ids.size();
        }
    }
    return true;
}

}