#include "dexkit.h"

namespace dexkit {

#define MAX_DEPTH 2

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ClassMatcher *matcher, int depth) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->class_name() && matcher->class_name()->match_type() == schema::StringMatchType::Equal && !matcher->class_name()->ignore_case()) {
        result.emplace_back(matcher->class_name()->value()->string_view());
    }
    if (depth >= MAX_DEPTH) return result;
    // maybe declared other dex
    if (matcher->super_class()) {
        auto res = ExtractUseTypeNames(matcher->super_class(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    // maybe declared other dex
    if (matcher->interfaces()) {
        auto res = ExtractUseTypeNames(matcher->interfaces(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->fields()) {
        auto res = ExtractUseTypeNames(matcher->fields(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->methods()) {
        auto res = ExtractUseTypeNames(matcher->methods(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::InterfacesMatcher *matcher, int depth) {
    if (!matcher || !matcher->interfaces()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->interfaces()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->interfaces()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationsMatcher *matcher, int depth) {
    if (!matcher || !matcher->annotations()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->annotations()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->annotations()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationMatcher *matcher, int depth) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->type()) {
        auto res = ExtractUseTypeNames(matcher->type(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->elements()) {
        auto res = ExtractUseTypeNames(matcher->elements(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationElementsMatcher *matcher, int depth) {
    if (!matcher || !matcher->elements()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->elements()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->elements()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationElementMatcher *matcher, int depth) {
    if (!matcher || !matcher->value()) return {};
    std::vector<std::string_view> result;
    switch (matcher->value_type()) {
        case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_ClassMatcher(), depth + 1);
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_FieldMatcher(), depth + 1);
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_AnnotationEncodeArrayMatcher(), depth);
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
            auto res = ExtractUseTypeNames(matcher->value_as_AnnotationMatcher(), depth);
            result.insert(result.end(), res.begin(), res.end());
            break;
        }
        default:
            break;
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::AnnotationEncodeArrayMatcher *matcher, int depth) {
    if (!matcher || !matcher->values()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->values()->size(); ++i) {
        auto type = matcher->values_type()->Get(i);
        switch (type) {
            case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::ClassMatcher>(i), depth + 1);
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::FieldMatcher>(i), depth + 1);
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::AnnotationEncodeArrayMatcher>(i), depth);
                result.insert(result.end(), res.begin(), res.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
                auto res = ExtractUseTypeNames(matcher->values()->GetAs<schema::AnnotationMatcher>(i), depth);
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
DexKit::ExtractUseTypeNames(const schema::FieldsMatcher *matcher, int depth) {
    if (!matcher || !matcher->fields()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->fields()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->fields()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::FieldMatcher *matcher, int depth) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->declaring_class()) {
        auto res = ExtractUseTypeNames(matcher->declaring_class(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->type_class()) {
        auto res = ExtractUseTypeNames(matcher->type_class(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (depth >= MAX_DEPTH) return result;
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
//    if (matcher->get_methods()) {
//        auto res = ExtractUseTypeNames(matcher->get_methods());
//        result.insert(result.end(), res.begin(), res.end());
//    }
//    if (matcher->put_methods()) {
//        auto res = ExtractUseTypeNames(matcher->put_methods());
//        result.insert(result.end(), res.begin(), res.end());
//    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::MethodsMatcher *matcher, int depth) {
    if (!matcher || !matcher->methods()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->methods()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->methods()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::MethodMatcher *matcher, int depth) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->declaring_class()) {
        auto res = ExtractUseTypeNames(matcher->declaring_class(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->return_type()) {
        auto res = ExtractUseTypeNames(matcher->return_type(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->parameters()) {
        auto res = ExtractUseTypeNames(matcher->parameters(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (depth >= MAX_DEPTH) return result;
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->using_fields()) {
        for (auto i = 0; i < matcher->using_fields()->size(); ++i) {
            auto res = ExtractUseTypeNames(matcher->using_fields()->Get(i)->field(), depth + 1);
            result.insert(result.end(), res.begin(), res.end());
        }
    }
    if (matcher->invoking_methods()) {
        auto res = ExtractUseTypeNames(matcher->invoking_methods(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
//    if (matcher->method_callers()) {
//        auto res = ExtractUseTypeNames(matcher->method_callers());
//        result.insert(result.end(), res.begin(), res.end());
//    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ParametersMatcher *matcher, int depth) {
    if (!matcher || !matcher->parameters()) return {};
    std::vector<std::string_view> result;
    for (auto i = 0; i < matcher->parameters()->size(); ++i) {
        auto res = ExtractUseTypeNames(matcher->parameters()->Get(i), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

std::vector<std::string_view> // NOLINTNEXTLINE
DexKit::ExtractUseTypeNames(const schema::ParameterMatcher *matcher, int depth) {
    if (!matcher) return {};
    std::vector<std::string_view> result;
    if (matcher->parameter_type()) {
        auto res = ExtractUseTypeNames(matcher->parameter_type(), depth + 1);
        result.insert(result.end(), res.begin(), res.end());
    }
    if (matcher->annotations()) {
        auto res = ExtractUseTypeNames(matcher->annotations(), depth);
        result.insert(result.end(), res.begin(), res.end());
    }
    return result;
}

}