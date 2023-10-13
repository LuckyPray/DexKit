// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#include "dex_item.h"
#include "utils/dex_descriptor_util.h"

namespace dexkit {

template<typename T, typename U>
class Hungarian {
private:
    std::vector<U> left;
    std::vector<T> right;
    std::vector<std::vector<int8_t>> map;
    std::vector<int> p;
    std::vector<bool> vis;
    std::function<bool(T&, U&)> judge;
    bool fast_fail = false;
public:
    Hungarian(const std::vector<T> &targets, const std::vector<U> &matchers, std::function<bool(T&, U&)> match) {
        if (matchers.size() > targets.size()) {
            fast_fail = true;
            return;
        }
        this->left = matchers;
        this->right = targets;
        map.resize(left.size());
        for (int i = 0; i < left.size(); ++i) {
            map[i].resize(right.size());
        }
        p.resize(right.size(), -1);
        vis.resize(right.size());
        this->judge = match;
    }

    // NOLINTNEXTLINE
    bool dfs(int i) {
        for (int j = 0; j < right.size(); ++j) {
            if (vis[j]) continue;
            if (!map[i][j]) map[i][j] = judge(right[j], left[i]) ? 1 : -1;
            if (map[i][j] > 0) {
                vis[j] = true;
                if (p[j] < 0 || dfs(p[j])) {
                    p[j] = i;
                    return true;
                }
            }
        }
        return false;
    }

    int solve() {
        if (fast_fail || left.empty() || right.empty()) {
            return 0;
        }
        int ans = 0;
        for (int i = 0; i < left.size(); ++i) {
            std::fill(vis.begin(), vis.end(), false);
            if (!dfs(i)) {
                return 0;
            }
            ans++;
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

    bool condition;
    switch (match_type) {
        case schema::StringMatchType::StartWith: condition = kmp::starts_with(str, match_str, matcher->ignore_case()); break;
        case schema::StringMatchType::EndWith: condition = kmp::ends_with(str, match_str, matcher->ignore_case()); break;
        case schema::StringMatchType::Equal: condition = kmp::equals(str, match_str, matcher->ignore_case()); break;
        case schema::StringMatchType::Contains: {
            auto index = kmp::FindIndex(str, match_str, matcher->ignore_case());
            condition = index != -1;
            break;
        }
        case schema::StringMatchType::SimilarRegex: abort();
    }
    return condition;
}

bool DexItem::IsAccessFlagsMatched(uint32_t access_flags, const schema::AccessFlagsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    DEXKIT_CHECK(matcher->flags() != 0);
    switch (matcher->match_type()) {
        case schema::MatchType::Equal: return access_flags == matcher->flags();
        case schema::MatchType::Contains: return (access_flags & matcher->flags()) == matcher->flags();
    }
}

std::set<std::string_view> DexItem::BuildBatchFindKeywordsMap(
        const flatbuffers::Vector<flatbuffers::Offset<schema::StringMatcher>> *using_strings_matcher,
        std::vector<std::pair<std::string_view, bool>> &keywords,
        phmap::flat_hash_map<std::string_view, schema::StringMatchType> &match_type_map
) {
    auto result = std::set<std::string_view>();
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
        result.insert(value);
        keywords.emplace_back(value, ignore_case);
        match_type_map[value] = type;
    }
    return result;
}

bool DexItem::IsAnnotationMatched(const ir::Annotation *annotation, const schema::AnnotationMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (annotation == nullptr) {
        return false;
    }
    if (!IsClassMatched(annotation->type->orig_index, matcher->type())) {
        return false;
    }
    auto m_class_annotations = this->class_annotations[annotation->type->orig_index];
    if (matcher->target_element_types() || (uint8_t) matcher->policy()) {
        if (m_class_annotations == nullptr) {
            return false;
        }
        ir::Annotation *retention_annotation = nullptr;
        ir::Annotation *target_annotation = nullptr;
        for (auto ann: m_class_annotations->annotations) {
            if (ann->type->orig_index == this->annotation_retention_class_id) {
                retention_annotation = ann;
            } else if (ann->type->orig_index == this->annotation_target_class_id) {
                target_annotation = ann;
            }
        }
        if ((uint8_t) matcher->policy()) {
            DEXKIT_CHECK(retention_annotation->elements.size() == 1);
            auto element = retention_annotation->elements[0];
            auto field_idx = element->value->u.enum_value->orig_index;
            DEXKIT_CHECK(this->retention_map.contains(field_idx));
            if (this->retention_map[field_idx] != matcher->policy()) {
                return false;
            }
        }
        if (matcher->target_element_types()) {
            auto target_element_types = matcher->target_element_types();
            uint32_t target_flags = 0, matcher_flags = 0;

            for (auto element: target_annotation->elements) {
                auto field_idx = element->value->u.enum_value->orig_index;
                DEXKIT_CHECK(this->target_element_map.contains(field_idx));
                target_flags |= 1 << (uint8_t) this->target_element_map[field_idx];
            }

            for (int i = 0; i < target_element_types->types()->size(); ++i) {
                auto type = target_element_types->types()->Get(i);
                matcher_flags |= 1 << (uint8_t) type;
            }

            bool condition = false;
            switch (target_element_types->match_type()) {
                case schema::MatchType::Equal: condition = target_flags == matcher_flags; break;
                case schema::MatchType::Contains: condition = (target_flags & matcher_flags) == matcher_flags; break;
            }
            if (!condition) {
                return false;
            }
        }
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
    auto annotation_set_size = annotationSet == nullptr ? 0 : annotationSet->annotations.size();
    if (matcher->annotation_count()) {
        if (annotation_set_size < matcher->annotation_count()->min()
        || annotation_set_size > matcher->annotation_count()->max()) {
            return false;
        }
    }
    if (matcher->annotations()) {
        auto IsAnnotationMatched = [this](ir::Annotation *annotation, const schema::AnnotationMatcher *matcher) {
            return this->IsAnnotationMatched(annotation, matcher);
        };

        typedef std::vector<const schema::AnnotationMatcher *> AnnotationMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationMatcher>(POINT_CASE(matcher->annotations()));
        if (ptr == nullptr) {
            auto vec = AnnotationMatcher();
            for (auto annotation : *matcher->annotations()) {
                vec.push_back(annotation);
            }
            ptr = ThreadVariable::SetThreadVariable<AnnotationMatcher>(POINT_CASE(matcher->annotations()), vec);
        }

        auto annotation_matches = *ptr;
        if (annotation_matches.size() > annotation_set_size) {
            return false;
        }
        Hungarian<ir::Annotation *, const schema::AnnotationMatcher *> hungarian(annotationSet->annotations, annotation_matches, IsAnnotationMatched);
        auto count = hungarian.solve();
        if (count != annotation_matches.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != annotation_set_size) {
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
        case schema::AnnotationEncodeValueMatcher::MethodMatcher: return dex::kEncodedMethod;
        case schema::AnnotationEncodeValueMatcher::FieldMatcher: return dex::kEncodedEnum;
        case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: return dex::kEncodedArray;
        case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: return dex::kEncodedAnnotation;
        case schema::AnnotationEncodeValueMatcher::EncodeValueNull: return dex::kEncodedNull;
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

bool DexItem::IsAnnotationEncodeValueMatched(const ir::EncodedValue *encodedValue, schema::AnnotationEncodeValueMatcher type, const void *value) {
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
        case dex::kEncodedMethod: return IsMethodMatched(encodedValue->u.method_value->orig_index, NonNullCase<const dexkit::schema::MethodMatcher *>(value));
        case dex::kEncodedEnum: return IsFieldMatched(encodedValue->u.enum_value->orig_index, NonNullCase<const dexkit::schema::FieldMatcher *>(value));
        case dex::kEncodedArray: return IsAnnotationEncodeArrayMatcher(encodedValue->u.array_value->values, NonNullCase<const dexkit::schema::AnnotationEncodeArrayMatcher *>(value));
        case dex::kEncodedAnnotation: return IsAnnotationMatched(encodedValue->u.annotation_value, NonNullCase<const dexkit::schema::AnnotationMatcher *>(value));
        case dex::kEncodedNull: return true;
        case dex::kEncodedBoolean: return encodedValue->u.bool_value == NonNullCase<const dexkit::schema::EncodeValueBoolean *>(value)->value();
        default: abort();
    }
}

bool DexItem::IsAnnotationEncodeArrayMatcher(const std::vector<ir::EncodedValue *> &encodedValues, const dexkit::schema::AnnotationEncodeArrayMatcher *matcher) {
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
        auto IsAnnotationEncodeValueMatched = [this](const ir::EncodedValue *encodedValue, const std::pair<schema::AnnotationEncodeValueMatcher, const void *> &value) {
            return this->IsAnnotationEncodeValueMatched(encodedValue, value.first, value.second);
        };

        if (matcher->values()->size() > encodedValues.size()) {
            return false;
        }

        typedef std::vector<std::pair<schema::AnnotationEncodeValueMatcher, const void *>> AnnotationEncodeValueMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationEncodeValueMatcher>(POINT_CASE(matcher->values()));
        if (ptr == nullptr) {
            auto values = AnnotationEncodeValueMatcher();
            for (auto i = 0; i < matcher->values()->size(); ++i) {
                auto type = matcher->values_type()->Get(i);
                auto value = matcher->values()->GetAs<void>(i);
                values.emplace_back(type, value);
            }
            ptr = ThreadVariable::SetThreadVariable<AnnotationEncodeValueMatcher>(POINT_CASE(matcher), values);
        }

        auto values = *ptr;
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
        DEXKIT_CHECK(matcher->value_type() != schema::AnnotationEncodeValueMatcher::NONE);
        if (!IsAnnotationEncodeValueMatched(annotationElement->value, matcher->value_type(), matcher->value())) {
            return false;
        }
    }
    return true;
}

bool DexItem::IsAnnotationElementsMatched(const std::vector<ir::AnnotationElement *> &annotationElement, const schema::AnnotationElementsMatcher *matcher) {
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
        auto IsAnnotationElementMatched = [this](const ir::AnnotationElement *annotationElement, const schema::AnnotationElementMatcher *matcher) {
            return this->IsAnnotationElementMatched(annotationElement, matcher);
        };

        typedef std::vector<const schema::AnnotationElementMatcher *> AnnotationElementMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<AnnotationElementMatcher>(POINT_CASE(matcher->elements()));
        if (ptr == nullptr) {
            auto matchers = AnnotationElementMatcher();
            for (auto element : *matcher->elements()) {
                matchers.push_back(element);
            }
            ptr = ThreadVariable::SetThreadVariable<AnnotationElementMatcher>(POINT_CASE(matcher->elements()), matchers);
        }

        auto matchers = *ptr;
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

// NOLINTNEXTLINE
bool DexItem::IsClassMatched(uint32_t type_idx, const schema::ClassMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!type_def_flag[type_idx]) {
        // try matched in declared dex
        auto &type_name = type_names[type_idx];
        auto declared_info = dexkit->GetClassDeclaredPair(type_name);
        if (declared_info.first) {
            return declared_info.first->IsClassMatched(declared_info.second, matcher);
        }
    }
    if (!IsTypeNameMatched(type_idx, matcher->class_name())) {
        return false;
    }
    if (!IsClassSmaliSourceMatched(type_idx, matcher->smali_source())) {
        return false;
    }
    if (!IsClassAccessFlagsMatched(type_idx, matcher->access_flags())) {
        return false;
    }
    if (!IsSuperClassMatched(type_idx, matcher->super_class())) {
        return false;
    }
    if (!IsClassUsingStringsMatched(type_idx, matcher)) {
        return false;
    }
    if (!IsClassAnnotationMatched(type_idx, matcher->annotations())) {
        return false;
    }
    if (!IsInterfacesMatched(type_idx, matcher->interfaces())) {
        return false;
    }
    if (!IsFieldsMatched(type_idx, matcher->fields())) {
        return false;
    }
    if (!IsMethodsMatched(type_idx, matcher->methods())) {
        return false;
    }
    return true;
}

bool DexItem::IsTypeNameMatched(uint32_t type_idx, const schema::StringMatcher *matcher) {
    if (matcher == nullptr || matcher->value()->size() == 0) {
        return true;
    }
    auto type_array_count = this->type_name_array_count[type_idx];
    auto type_name = this->type_names[type_idx];
    auto component_type_name = type_name.substr(type_array_count);

    auto match_ptr = ThreadVariable::GetThreadVariable<std::pair<std::string_view, schema::StringMatchType>>(POINT_CASE(matcher));
    if (match_ptr == nullptr) {
        auto match_str = matcher->value()->string_view();
        auto match_type = matcher->match_type();
        ConvertSimilarRegex(match_str, match_type);
        auto match_pair = std::make_pair(match_str, match_type);
        match_ptr = ThreadVariable::SetThreadVariable<std::pair<std::string_view, schema::StringMatchType>>(POINT_CASE(matcher), match_pair);
    }
    auto match_str = match_ptr->first;
    auto match_type = match_ptr->second;

    typedef std::pair<std::string, uint8_t> MatchPair;
    auto ptr = ThreadVariable::GetThreadVariable<MatchPair>(POINT_CASE(matcher->value()));
    if (ptr == nullptr) {
        auto array_count = 0;
        auto find_index = match_str.find_first_of('[');
        if (find_index != std::string_view::npos) {
            array_count = (uint8_t) (match_str.size() - find_index) / 2;
        }
        auto match_name_type = match_str.substr(0, match_str.size() - array_count * 2);
        bool start_flag = match_type == schema::StringMatchType::StartWith || match_type == schema::StringMatchType::Equal;
        bool end_flag = match_type == schema::StringMatchType::EndWith || match_type == schema::StringMatchType::Equal;
        auto match_name = NameToDescriptor(match_name_type, start_flag, end_flag);
        ptr = ThreadVariable::SetThreadVariable<MatchPair>(POINT_CASE(matcher->value()), std::make_pair(match_name, array_count));
    }

    auto match_pair = *ptr;
    auto &match_type_name = match_pair.first;
    auto &match_array_count = match_pair.second;
    bool condition;
    switch (match_type) {
        case schema::StringMatchType::StartWith: {
            auto starts_with = kmp::starts_with(component_type_name, match_type_name, matcher->ignore_case());
            condition = starts_with && match_array_count <= type_array_count;
            break;
        }
        case schema::StringMatchType::EndWith: {
            auto ends_with = kmp::ends_with(component_type_name, match_type_name, matcher->ignore_case());
            condition = ends_with && match_array_count == type_array_count;
            break;
        }
        case schema::StringMatchType::Equal: {
            auto equals = kmp::equals(component_type_name, match_type_name, matcher->ignore_case());
            condition = equals && match_array_count == type_array_count;
            break;
        }
        case schema::StringMatchType::Contains: {
            auto index = kmp::FindIndex(component_type_name, match_type_name, matcher->ignore_case());
            condition = index != -1 && match_array_count <= type_array_count;
            break;
        }
        case schema::StringMatchType::SimilarRegex: abort();
    }
    return condition;
}

bool DexItem::IsClassAccessFlagsMatched(uint32_t type_idx, const schema::AccessFlagsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    auto access_flags = this->class_access_flags[type_idx];
    return IsAccessFlagsMatched(access_flags, matcher);
}

bool DexItem::IsClassSmaliSourceMatched(uint32_t type_idx, const schema::StringMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    auto smali_source = this->class_source_files[type_idx];
    return IsStringMatched(smali_source, matcher);
}

bool DexItem::IsClassUsingStringsMatched(uint32_t type_idx, const schema::ClassMatcher *matcher) {
    if (matcher->using_strings() == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }

    typedef acdat::AhoCorasickDoubleArrayTrie<std::string_view> AcTrie;
    typedef phmap::flat_hash_map<std::string_view, schema::StringMatchType> MatchTypeMap;
    typedef std::set<std::string_view> StringSet;
    std::shared_ptr<AcTrie> acTrie;
    std::shared_ptr<MatchTypeMap> match_type_map;
    std::shared_ptr<StringSet> real_keywords;

    typedef std::tuple<std::shared_ptr<AcTrie>, std::shared_ptr<MatchTypeMap>, std::shared_ptr<StringSet>> KeywordsTuple;
    std::vector<std::pair<std::string_view, bool>> keywords;
    auto ptr = ThreadVariable::GetThreadVariable<KeywordsTuple>(POINT_CASE(matcher->using_strings()));
    if (ptr == nullptr) {
        auto trie = std::make_shared<AcTrie>();
        auto map = std::make_shared<MatchTypeMap>();
        auto result = BuildBatchFindKeywordsMap(matcher->using_strings(), keywords, *map);
        auto string_set = std::make_shared<StringSet>(result);
        acdat::Builder<std::string_view>().Build(keywords, trie.get());
        auto tuple = std::make_tuple(trie, map, string_set);
        ptr = ThreadVariable::SetThreadVariable<KeywordsTuple>(POINT_CASE(matcher->using_strings()), tuple);
    }

    acTrie = std::get<0>(*ptr);
    match_type_map = std::get<1>(*ptr);
    real_keywords = std::get<2>(*ptr);

    auto using_empty_string_count = 0;
    std::set<std::string_view> search_set;
    for (auto method_idx: this->class_method_ids[type_idx]) {
        auto &using_strings = this->method_using_string_ids[method_idx];
        for (auto idx: using_strings) {
            if (idx == this->empty_string_id) ++using_empty_string_count;
            auto str = this->strings[idx];
            auto hits = acTrie->ParseText(str);
            for (auto &hit: hits) {
                auto match_type = match_type_map->find(hit.value)->second;
                bool match;
                switch (match_type) {
                    case schema::StringMatchType::Contains: match = true; break;
                    case schema::StringMatchType::StartWith: match = (hit.begin == 0); break;
                    case schema::StringMatchType::EndWith: match = (hit.end == str.size()); break;
                    case schema::StringMatchType::Equal: match = (hit.begin == 0 && hit.end == str.size()); break;
                    case schema::StringMatchType::SimilarRegex: abort();
                }
                if (match) {
                    search_set.insert(hit.value);
                }
            }
        }
    }
    if (match_type_map->contains("")) {
        DEXKIT_CHECK((*match_type_map)[""] == schema::StringMatchType::Equal);
        if (using_empty_string_count) {
            search_set.insert("");
        }
    }
    if (search_set.size() < real_keywords->size()) {
        return false;
    }
    std::vector<std::string_view> unique_vec;
    std::set_intersection(search_set.begin(), search_set.end(),
                          real_keywords->begin(), real_keywords->end(),
                          std::inserter(unique_vec, unique_vec.begin()));
    if (unique_vec.size() != real_keywords->size()) {
        return false;
    }
    return true;
}

// NOLINTNEXTLINE
bool DexItem::IsSuperClassMatched(uint32_t type_idx, const schema::ClassMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto super_class_idx = this->reader.ClassDefs()[this->type_def_idx[type_idx]].superclass_idx;
    return IsClassMatched(super_class_idx, matcher);
}

bool DexItem::IsInterfacesMatched(uint32_t type_idx, const schema::InterfacesMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    const auto &interfaces = this->class_interface_ids[type_idx];
    if (matcher->interface_count()) {
        if (interfaces.size() < matcher->interface_count()->min()
        || interfaces.size() > matcher->interface_count()->max()) {
            return false;
        }
    }
    if (matcher->interfaces()) {
        auto IsClassMatched = [this](uint32_t type_idx, const schema::ClassMatcher *matcher) {
            return this->IsClassMatched(type_idx, matcher);
        };

        typedef std::vector<const schema::ClassMatcher *> ClassMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<ClassMatcher>(POINT_CASE(matcher->interfaces()));
        if (ptr == nullptr) {
            auto vec = ClassMatcher();
            for (auto interface_matcher : *matcher->interfaces()) {
                vec.push_back(interface_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<ClassMatcher>(POINT_CASE(matcher->interfaces()), vec);
        }

        auto interface_matchers = *ptr;
        Hungarian<uint32_t, const schema::ClassMatcher *> hungarian(interfaces, interface_matchers, IsClassMatched);
        auto count = hungarian.solve();
        if (count != interface_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != interfaces.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsClassAnnotationMatched(uint32_t type_idx, const schema::AnnotationsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    if (!IsAnnotationsMatched(this->class_annotations[type_idx], matcher)) {
        return false;
    }
    return true;
}

bool DexItem::IsFieldsMatched(uint32_t type_idx, const schema::FieldsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    const auto &fields = this->class_field_ids[type_idx];
    if (matcher->field_count()) {
        if (fields.size() < matcher->field_count()->min()
        || fields.size() > matcher->field_count()->max()) {
            return false;
        }
    }
    if (matcher->fields()) {
        auto IsFieldMatched = [this](uint32_t field_idx, const schema::FieldMatcher *matcher) {
            return this->IsFieldMatched(field_idx, matcher);
        };

        typedef std::vector<const schema::FieldMatcher *> FieldMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<FieldMatcher>(POINT_CASE(matcher->fields()));
        if (ptr == nullptr) {
            auto vec = FieldMatcher();
            for (auto field_matcher : *matcher->fields()) {
                vec.push_back(field_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<FieldMatcher>(POINT_CASE(matcher->fields()), vec);
        }

        auto field_matchers = *ptr;
        Hungarian<uint32_t, const schema::FieldMatcher *> hungarian(fields, field_matchers, IsFieldMatched);
        auto count = hungarian.solve();
        if (count != field_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != fields.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsMethodsMatched(uint32_t type_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!this->type_def_flag[type_idx]) {
        return false;
    }
    const auto &methods = this->class_method_ids[type_idx];
    if (matcher->method_count()) {
        if (methods.size() < matcher->method_count()->min()
        || methods.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = MethodMatcher();
            for (auto method_matcher : *matcher->methods()) {
                vec.push_back(method_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()), vec);
        }

        auto method_matchers = *ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(methods, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != method_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != methods.size()) {
                return false;
            }
        }
    }
    return true;
}

// NOLINTNEXTLINE
bool DexItem::IsMethodMatched(uint32_t method_idx, const schema::MethodMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto &cross_info = this->method_cross_info[method_idx];
    if (cross_info.has_value()) {
        auto dex = dexkit->GetDexItem(cross_info->first);
        return dex->IsMethodMatched(cross_info->second, matcher);
    }
    auto &method_def = this->reader.MethodIds()[method_idx];
    auto method_name = this->strings[method_def.name_idx];
    if (!IsStringMatched(method_name, matcher->method_name())) {
        return false;
    }
    if (!IsAccessFlagsMatched(this->method_access_flags[method_idx], matcher->access_flags())) {
        return false;
    }
    if (!IsClassMatched(method_def.class_idx, matcher->declaring_class())) {
        return false;
    }
    if (!IsOpCodesMatched(method_idx, matcher->op_codes())) {
        return false;
    }
    if (!IsMethodUsingStringsMatched(method_idx, matcher)) {
        return false;
    }
    if (!IsMethodAnnotationMatched(method_idx, matcher->annotations())) {
        return false;
    }
    auto &proto_def = this->reader.ProtoIds()[method_def.proto_idx];
    if (!IsClassMatched(proto_def.return_type_idx, matcher->return_type())) {
        return false;
    }
    if (!IsParametersMatched(method_idx, matcher->parameters())) {
        return false;
    }
    if (!IsUsingFieldsMatched(method_idx, matcher)) {
        return false;
    }
    if (!IsInvokingMethodsMatched(method_idx, matcher->invoking_methods())) {
        return false;
    }
    if (!IsCallMethodsMatched(method_idx, matcher->method_callers())) {
        return false;
    }
    if (!IsUsingNumbersMatched(method_idx, matcher)) {
        return false;
    }
    return true;
}

bool DexItem::IsParametersMatched(uint32_t method_idx, const schema::ParametersMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto method_def = this->reader.MethodIds()[method_idx];
    const auto type_list = this->proto_type_list[method_def.proto_idx];
    auto type_list_size = type_list == nullptr ? 0 : type_list->size;
    if (matcher->parameter_count()) {
        if (type_list_size < matcher->parameter_count()->min()
        || type_list_size > matcher->parameter_count()->max()) {
            return false;
        }
    }
    if (matcher->parameters()) {
        if (type_list_size != matcher->parameters()->size()) {
            return false;
        }
        for (size_t i = 0; i < type_list_size; ++i) {
            auto parameter_matcher = matcher->parameters()->Get(i);
            if (!IsClassMatched(type_list->list[i].type_idx, parameter_matcher->parameter_type())) {
                return false;
            }
            if (parameter_matcher->annotations()) {
                if (!IsAnnotationsMatched(this->method_parameter_annotations[method_idx][i], parameter_matcher->annotations())) {
                    return false;
                }
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
    auto op_code_size = opt_opcodes.has_value() ? opt_opcodes->size() : 0;
    if (matcher->op_code_count()) {
        if (op_code_size < matcher->op_code_count()->min()
        || op_code_size > matcher->op_code_count()->max()) {
            return false;
        }
    }
    if (matcher->op_codes()) {
        if (matcher->op_codes()->size() > op_code_size) {
            return false;
        }

        typedef std::vector<std::optional<uint8_t>> OpCodeMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<OpCodeMatcher>(POINT_CASE(matcher->op_codes()));
        if (ptr == nullptr) {
            auto vec = OpCodeMatcher();
            for (auto opcode : *matcher->op_codes()) {
                if (opcode < 0) {
                    vec.emplace_back(std::nullopt);
                } else {
                    vec.emplace_back(opcode);
                }
            }
            ptr = ThreadVariable::SetThreadVariable<OpCodeMatcher>(POINT_CASE(matcher->op_codes()), vec);
        }

        auto matcher_opcodes = *ptr;
        if (matcher_opcodes.size() > op_code_size) {
            return false;
        }

        if (!matcher_opcodes.empty()) {
            auto index = kmp::FindIndex(opt_opcodes.value(), matcher_opcodes);
            if (index == -1) {
                return false;
            }
            bool condition = false;
            switch (matcher->match_type()) {
                case schema::OpCodeMatchType::Equal: condition = index == 0 && matcher_opcodes.size() == op_code_size; break;
                case schema::OpCodeMatchType::StartWith: condition = index == 0; break;
                case schema::OpCodeMatchType::EndWith: condition = index + matcher_opcodes.size() == op_code_size; break;
                case schema::OpCodeMatchType::Contains: condition = true; break;
            }
            if (!condition) {
                return false;
            }
        }

    }
    return true;
}

bool DexItem::IsMethodUsingStringsMatched(uint32_t method_idx, const schema::MethodMatcher *matcher) {
    if (matcher->using_strings() == nullptr) {
        return true;
    }

    typedef acdat::AhoCorasickDoubleArrayTrie<std::string_view> AcTrie;
    typedef phmap::flat_hash_map<std::string_view, schema::StringMatchType> MatchTypeMap;
    typedef std::set<std::string_view> StringSet;
    std::shared_ptr<AcTrie> acTrie;
    std::shared_ptr<MatchTypeMap> match_type_map;
    std::shared_ptr<StringSet> real_keywords;

    typedef std::tuple<std::shared_ptr<AcTrie>, std::shared_ptr<MatchTypeMap>, std::shared_ptr<StringSet>> KeywordsTuple;
    std::vector<std::pair<std::string_view, bool>> keywords;
    auto ptr = ThreadVariable::GetThreadVariable<KeywordsTuple>(POINT_CASE(matcher->using_strings()));
    if (ptr == nullptr) {
        auto trie = std::make_shared<AcTrie>();
        auto map = std::make_shared<MatchTypeMap>();
        auto result = BuildBatchFindKeywordsMap(matcher->using_strings(), keywords, *map);
        auto string_set = std::make_shared<StringSet>(result);
        acdat::Builder<std::string_view>().Build(keywords, trie.get());
        auto tuple = std::make_tuple(trie, map, string_set);
        ptr = ThreadVariable::SetThreadVariable<KeywordsTuple>(POINT_CASE(matcher->using_strings()), tuple);
    }

    acTrie = std::get<0>(*ptr);
    match_type_map = std::get<1>(*ptr);
    real_keywords = std::get<2>(*ptr);

    auto using_empty_string_count = 0;
    std::set<std::string_view> search_set;
    auto &using_strings = this->method_using_string_ids[method_idx];
    for (auto idx: using_strings) {
        if (idx == this->empty_string_id) ++using_empty_string_count;
        auto str = this->strings[idx];
        auto hits = acTrie->ParseText(str);
        for (auto &hit: hits) {
            auto match_type = match_type_map->find(hit.value)->second;
            bool match;
            switch (match_type) {
                case schema::StringMatchType::Contains: match = true; break;
                case schema::StringMatchType::StartWith: match = (hit.begin == 0); break;
                case schema::StringMatchType::EndWith: match = (hit.end == str.size()); break;
                case schema::StringMatchType::Equal: match = (hit.begin == 0 && hit.end == str.size()); break;
                case schema::StringMatchType::SimilarRegex: abort();
            }
            if (match) {
                search_set.insert(hit.value);
            }
        }
    }
    if (match_type_map->contains("")) {
        DEXKIT_CHECK((*match_type_map)[""] == schema::StringMatchType::Equal);
        if (using_empty_string_count) {
            search_set.insert("");
        }
    }
    if (search_set.size() < real_keywords->size()) {
        return false;
    }
    std::vector<std::string_view> unique_vec;
    std::set_intersection(search_set.begin(), search_set.end(),
                          real_keywords->begin(), real_keywords->end(),
                          std::inserter(unique_vec, unique_vec.begin()));
    if (unique_vec.size() != real_keywords->size()) {
        return false;
    }
    return true;
}

bool DexItem::IsMethodAnnotationMatched(uint32_t method_idx, const schema::AnnotationsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!IsAnnotationsMatched(this->method_annotations[method_idx], matcher)) {
        return false;
    }
    return true;
}

bool DexItem::IsUsingFieldsMatched(uint32_t method_idx, const schema::MethodMatcher *matcher) {
    if (matcher->using_fields() == nullptr) {
        return true;
    }
    auto IsUsingFieldMatched = [this](std::pair<uint32_t, bool> field, const schema::UsingFieldMatcher *matcher) {
        return this->IsUsingFieldMatched(field, matcher);
    };
    auto &using_fields = this->method_using_field_ids[method_idx];

    typedef std::vector<const schema::UsingFieldMatcher *> UsingFieldMatcher;
    auto ptr = ThreadVariable::GetThreadVariable<UsingFieldMatcher>(POINT_CASE(matcher->using_fields()));
    if (ptr == nullptr) {
        auto using_vec = UsingFieldMatcher();
        for (int i = 0; i < matcher->using_fields()->size(); ++i) {
            using_vec.push_back(matcher->using_fields()->Get(i));
        }
        ptr = ThreadVariable::SetThreadVariable<UsingFieldMatcher>(POINT_CASE(matcher->using_fields()), using_vec);
    }

    auto using_field_matchers = *ptr;
    Hungarian<std::pair<uint32_t, bool>, const schema::UsingFieldMatcher *> hungarian(using_fields, using_field_matchers, IsUsingFieldMatched);
    auto count = hungarian.solve();
    if (count != using_field_matchers.size()) {
        return false;
    }
    return true;
}

bool DexItem::IsUsingNumbersMatched(uint32_t method_idx, const schema::MethodMatcher *matcher) {
    if (matcher->using_numbers() == nullptr) {
        return true;
    }
    auto using_numbers = this->GetUsingNumberFromCode(method_idx);
    if (matcher->using_numbers()->size() > using_numbers.size()) {
        return false;
    }

    typedef std::vector<EncodeNumber> Numbers;
    auto ptr = ThreadVariable::GetThreadVariable<Numbers>(POINT_CASE(matcher->using_numbers()));
    if (ptr == nullptr) {
        auto numbers = Numbers();
        auto types = matcher->using_numbers_type();
        for (int i = 0; i < matcher->using_numbers()->size(); ++i) {
            auto NumberMatcher = matcher->using_numbers()->Get(i);
            auto type = types->Get(i);
            EncodeNumber number{};
            switch (type) {
                case schema::Number::EncodeValueByte: {
                    number = EncodeNumber{
                            .type = BYTE,
                            .value = {.L8 = matcher->using_numbers()->GetAs<schema::EncodeValueByte>(i)->value()}
                    };
                    break;
                }
                case schema::Number::EncodeValueShort: {
                    number = EncodeNumber{
                            .type = SHORT,
                            .value = {.L16 = matcher->using_numbers()->GetAs<schema::EncodeValueShort>(i)->value()}
                    };
                    break;
                }
                case schema::Number::EncodeValueInt: {
                    number = EncodeNumber{
                            .type = INT,
                            .value = {.L32 = {.int_value = matcher->using_numbers()->GetAs<schema::EncodeValueInt>(i)->value()}}
                    };
                    break;
                }
                case schema::Number::EncodeValueLong: {
                    number = EncodeNumber{
                            .type = LONG,
                            .value = {.L64 = {.long_value = matcher->using_numbers()->GetAs<schema::EncodeValueLong>(i)->value()}}
                    };
                    break;
                }
                case schema::Number::EncodeValueFloat: {
                    number = EncodeNumber{
                            .type = FLOAT,
                            .value = {.L32 = {.float_value = matcher->using_numbers()->GetAs<schema::EncodeValueFloat>(i)->value()}}
                    };
                    break;
                }
                case schema::Number::EncodeValueDouble: {
                    number = EncodeNumber{
                            .type = DOUBLE,
                            .value = {.L64 = {.double_value = matcher->using_numbers()->GetAs<schema::EncodeValueDouble>(i)->value()}}
                    };
                    break;
                }
                default: abort();
            }
            numbers.push_back(number);
        }
        ptr = ThreadVariable::SetThreadVariable<Numbers>(POINT_CASE(matcher->using_numbers()), numbers);
    }

    auto IsNumberMatched = [](EncodeNumber number, EncodeNumber matcher) {
        if (matcher.type >= FLOAT) {
            return abs(GetDoubleValue(number) - GetDoubleValue(matcher)) < EPS;
        }
        return GetLongValue(number) == GetLongValue(matcher);
    };

    auto numbers = *ptr;
    Hungarian<EncodeNumber, EncodeNumber> hungarian(using_numbers, numbers, IsNumberMatched);
    auto count = hungarian.solve();
    if (count != numbers.size()) {
        return false;
    }
    return true;
}

bool DexItem::IsInvokingMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto invoking_methods = this->method_invoking_ids[method_idx];
    if (matcher->method_count()) {
        if (invoking_methods.size() < matcher->method_count()->min()
        || invoking_methods.size() > matcher->method_count()->max()) {
            return false;
        }
    }

    if (matcher->methods()) {
        if (matcher->methods()->size() > invoking_methods.size()) {
            return false;
        }
        auto IsMethodMatched = [this](uint32_t method_idx, const schema::MethodMatcher *matcher) {
            return this->IsMethodMatched(method_idx, matcher);
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = MethodMatcher();
            for (auto method_matcher : *matcher->methods()) {
                vec.push_back(method_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()), vec);
        }

        auto method_matchers = *ptr;
        Hungarian<uint32_t, const schema::MethodMatcher *> hungarian(invoking_methods, method_matchers, IsMethodMatched);
        auto count = hungarian.solve();
        if (count != method_matchers.size()) {
            return false;
        }
        if (matcher->match_type() == schema::MatchType::Equal) {
            if (count != invoking_methods.size()) {
                return false;
            }
        }
    }
    return true;
}

bool DexItem::IsCallMethodsMatched(uint32_t method_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto &ids = this->method_caller_ids[method_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        auto IsMethodMatched = [this](std::pair<uint16_t, uint32_t> method_info, const schema::MethodMatcher *matcher) {
            if (method_info.first == this->dex_id) {
                return this->IsMethodMatched(method_info.second, matcher);
            } else {
                auto dex = dexkit->GetDexItem(method_info.first);
                return dex->IsMethodMatched(method_info.second, matcher);
            }
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = MethodMatcher();
            for (auto method_matcher : *matcher->methods()) {
                vec.push_back(method_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()), vec);
        }

        auto method_matchers = *ptr;
        Hungarian<std::pair<uint16_t, uint32_t>, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
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

bool DexItem::IsUsingFieldMatched(std::pair<uint32_t, bool> field, const schema::UsingFieldMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (matcher->field()) {
        auto type = field.second ? schema::UsingType::Get : schema::UsingType::Put;
        if (type != matcher->using_type() && matcher->using_type() != schema::UsingType::Any) {
            return false;
        }
        if (!IsFieldMatched(field.first, matcher->field())) {
            return false;
        }
    }
    return true;
}

// NOLINTNEXTLINE
bool DexItem::IsFieldMatched(uint32_t field_idx, const schema::FieldMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    auto &cross_info = this->field_cross_info[field_idx];
    if (cross_info.has_value()) {
        auto dex = dexkit->GetDexItem(cross_info->first);
        return dex->IsFieldMatched(cross_info->second, matcher);
    }
    auto &field_def = this->reader.FieldIds()[field_idx];
    auto field_name = this->strings[field_def.name_idx];
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
    if (!IsFieldAnnotationMatched(field_idx, matcher->annotations())) {
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

bool DexItem::IsFieldAnnotationMatched(uint32_t field_idx, const schema::AnnotationsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    if (!IsAnnotationsMatched(this->field_annotations[field_idx], matcher)) {
        return false;
    }
    return true;
}

bool DexItem::IsFieldGetMethodsMatched(uint32_t field_idx, const schema::MethodsMatcher *matcher) {
    if (matcher == nullptr) {
        return true;
    }
    const auto ids = this->field_get_method_ids[field_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        auto IsMethodMatched = [this](std::pair<uint16_t, uint32_t> method_idx, const schema::MethodMatcher *matcher) {
            if (method_idx.first == this->dex_id) {
                return this->IsMethodMatched(method_idx.second, matcher);
            } else {
                auto dex = dexkit->GetDexItem(method_idx.first);
                return dex->IsMethodMatched(method_idx.second, matcher);
            }
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = MethodMatcher();
            for (auto method_matcher : *matcher->methods()) {
                vec.push_back(method_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()), vec);
        }

        auto method_matchers = *ptr;
        Hungarian<std::pair<uint16_t, uint32_t>, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
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
    const auto ids = this->field_put_method_ids[field_idx];
    if (matcher->method_count()) {
        if (ids.size() < matcher->method_count()->min() || ids.size() > matcher->method_count()->max()) {
            return false;
        }
    }
    if (matcher->methods()) {
        if (matcher->methods()->size() > ids.size()) {
            return false;
        }
        auto IsMethodMatched = [this](std::pair<uint16_t, uint32_t> method_idx, const schema::MethodMatcher *matcher) {
            if (method_idx.first == this->dex_id) {
                return this->IsMethodMatched(method_idx.second, matcher);
            } else {
                auto dex = dexkit->GetDexItem(method_idx.first);
                return dex->IsMethodMatched(method_idx.second, matcher);
            }
        };

        typedef std::vector<const schema::MethodMatcher *> MethodMatcher;
        auto ptr = ThreadVariable::GetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()));
        if (ptr == nullptr) {
            auto vec = MethodMatcher();
            for (auto method_matcher : *matcher->methods()) {
                vec.push_back(method_matcher);
            }
            ptr = ThreadVariable::SetThreadVariable<MethodMatcher>(POINT_CASE(matcher->methods()), vec);
        }

        auto method_matchers = *ptr;
        Hungarian<std::pair<uint16_t, uint32_t>, const schema::MethodMatcher *> hungarian(ids, method_matchers, IsMethodMatched);
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