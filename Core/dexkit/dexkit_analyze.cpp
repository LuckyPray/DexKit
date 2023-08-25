#include "dexkit.h"

namespace dexkit {

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ClassMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->class_name() && matcher->class_name()->match_type() == schema::StringMatchType::Equal && !matcher->class_name()->ignore_case()) {
        result.emplace_back(matcher->class_name()->value()->string_view());
    }
    if (matcher->super_class()) {
        auto res = ExtractUseTypeNames(matcher->super_class());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->interfaces()) {
        auto res = ExtractUseTypeNames(matcher->interfaces());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->fields()) {
        auto res = ExtractUseTypeNames(matcher->fields());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->methods()) {
        auto res = ExtractUseTypeNames(matcher->methods());
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::InterfacesMatcher *matcher) {
    if (!matcher || !matcher->interfaces()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->interfaces()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->interfaces()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationsMatcher *matcher) {
    if (!matcher || !matcher->annotations()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->annotations()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->annotations()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->type_name() && matcher->type_name()->match_type() == schema::StringMatchType::Equal && !matcher->type_name()->ignore_case()) {
        result.emplace_back(matcher->type_name()->value()->string_view());
    }
    if (matcher->elements()) {
        auto res = ExtractUseTypeNames(matcher->elements());
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationElementsMatcher *matcher) {
    if (!matcher || !matcher->elements()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->elements()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->elements()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationElementMatcher *matcher) {
    if (!matcher || !matcher->value()) return {};
    std::vector<std::string_view> result;
    switch (matcher->value_type()) {
        case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_ClassMatcher());
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_FieldMatcher());
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_AnnotationEncodeArrayMatcher());
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_AnnotationMatcher());
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        default:
            break;
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationEncodeArrayMatcher *matcher) {
    if (!matcher || !matcher->values()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->values()->size(); ++i) {
        auto type = matcher->values_type()->Get(i);
        switch (type) {
            case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::ClassMatcher>(i));
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::FieldMatcher>(i));
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::AnnotationEncodeArrayMatcher>(i));
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::AnnotationMatcher>(i));
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            default:
                break;
        }
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::FieldsMatcher *matcher) {
    if (!matcher || !matcher->fields()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->fields()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->fields()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::FieldMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->declaring_class()) {
        auto res = ExtractUseTypeNames(matcher->declaring_class());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->type_class()) {
        auto res = ExtractUseTypeNames(matcher->type_class());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->get_methods()) {
        auto res = ExtractUseTypeNames(matcher->get_methods());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->put_methods()) {
        auto res = ExtractUseTypeNames(matcher->put_methods());
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::MethodsMatcher *matcher) {
    if (!matcher || !matcher->methods()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->methods()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->methods()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::MethodMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->declaring_class()) {
        auto res = ExtractUseTypeNames(matcher->declaring_class());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->return_type()) {
        auto res = ExtractUseTypeNames(matcher->return_type());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->parameters()) {
        auto res = ExtractUseTypeNames(matcher->parameters());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->using_fields()) {
        for (auto i = 0; i < matcher->using_fields()->size(); ++i) {
            auto res = ExtractUseTypeNames(matcher->using_fields()->Get(i)->field());
            result.insert(result.end(), res.begin(), res.end());
        }
    }
    if (matcher->invoking_methods()) {
        auto res = ExtractUseTypeNames(matcher->invoking_methods());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->method_callers()) {
        auto res = ExtractUseTypeNames(matcher->method_callers());
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ParametersMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->parameters()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->parameters()->Get(i));
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ParameterMatcher *matcher) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->parameter_type()) {
        auto res = ExtractUseTypeNames(matcher->parameter_type());
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations());
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

}