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

#include "analyze.h"

namespace dexkit {

namespace {

template<typename T>
bool HasEntries(const T *vector) {
    return vector != nullptr && vector->size() > 0;
}

template<typename T>
bool HasCompositeVector(const flatbuffers::Vector<flatbuffers::Offset<T>> *matchers);

bool HasCompositeInternal(const schema::StringMatcher *matcher) {
    return false;
}

bool HasCompositeInternal(const schema::ClassMatcher *matcher);
bool HasCompositeInternal(const schema::FieldMatcher *matcher);
bool HasCompositeInternal(const schema::MethodMatcher *matcher);
bool HasCompositeInternal(const schema::AnnotationEncodeArrayMatcher *matcher);
bool HasCompositeInternal(const schema::AnnotationElementMatcher *matcher);
bool HasCompositeInternal(const schema::AnnotationElementsMatcher *matcher);
bool HasCompositeInternal(const schema::AnnotationMatcher *matcher);
bool HasCompositeInternal(const schema::AnnotationsMatcher *matcher);
bool HasCompositeInternal(const schema::InterfacesMatcher *matcher);
bool HasCompositeInternal(const schema::FieldsMatcher *matcher);
bool HasCompositeInternal(const schema::MethodsMatcher *matcher);
bool HasCompositeInternal(const schema::ParameterMatcher *matcher);
bool HasCompositeInternal(const schema::ParametersMatcher *matcher);
bool HasCompositeInternal(const schema::UsingFieldMatcher *matcher);

template<typename T>
bool HasCompositeVector(const flatbuffers::Vector<flatbuffers::Offset<T>> *matchers) {
    if (!matchers) return false;
    for (int i = 0; i < matchers->size(); ++i) {
        if (HasCompositeInternal(matchers->Get(i))) {
            return true;
        }
    }
    return false;
}

static void MergeAnalyzeRet(AnalyzeRet &target, const AnalyzeRet &source) {
    target.need_flags |= source.need_flags;
    target.declare_class.insert(target.declare_class.end(), source.declare_class.begin(), source.declare_class.end());
}

template<typename T>
static void MergeAnalyzeVector(
        AnalyzeRet &target,
        const flatbuffers::Vector<flatbuffers::Offset<T>> *matchers,
        int dex_depth
) {
    if (!matchers) return;
    for (int i = 0; i < matchers->size(); ++i) {
        MergeAnalyzeRet(target, Analyze(matchers->Get(i), dex_depth));
    }
}

bool HasCompositeInternal(const schema::ClassMatcher *matcher) {
    if (!matcher) return false;
    return HasEntries(matcher->all_of())
           || HasEntries(matcher->any_of())
           || HasEntries(matcher->none_of())
           || HasCompositeInternal(matcher->smali_source())
           || HasCompositeInternal(matcher->class_name())
           || HasCompositeInternal(matcher->super_class())
           || HasCompositeInternal(matcher->interfaces())
           || HasCompositeInternal(matcher->annotations())
           || HasCompositeInternal(matcher->fields())
           || HasCompositeInternal(matcher->methods())
           || HasCompositeVector(matcher->using_strings());
}

bool HasCompositeInternal(const schema::FieldMatcher *matcher) {
    if (!matcher) return false;
    return HasEntries(matcher->all_of())
           || HasEntries(matcher->any_of())
           || HasEntries(matcher->none_of())
           || HasCompositeInternal(matcher->field_name())
           || HasCompositeInternal(matcher->declaring_class())
           || HasCompositeInternal(matcher->type_class())
           || HasCompositeInternal(matcher->annotations())
           || HasCompositeInternal(matcher->get_methods())
           || HasCompositeInternal(matcher->put_methods());
}

bool HasCompositeInternal(const schema::MethodMatcher *matcher) {
    if (!matcher) return false;
    return HasEntries(matcher->all_of())
           || HasEntries(matcher->any_of())
           || HasEntries(matcher->none_of())
           || HasCompositeInternal(matcher->method_name())
           || HasCompositeInternal(matcher->declaring_class())
           || HasCompositeInternal(matcher->return_type())
           || HasCompositeInternal(matcher->parameters())
           || HasCompositeInternal(matcher->annotations())
           || HasCompositeVector(matcher->using_strings())
           || HasCompositeVector(matcher->using_fields())
           || HasCompositeInternal(matcher->invoking_methods())
           || HasCompositeInternal(matcher->method_callers());
}

bool HasCompositeInternal(const schema::AnnotationEncodeArrayMatcher *matcher) {
    if (!matcher || !matcher->values()) return false;
    for (int i = 0; i < matcher->values()->size(); ++i) {
        switch (matcher->values_type()->Get(i)) {
            case schema::AnnotationEncodeValueMatcher::StringMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::StringMatcher>(i))) return true;
                break;
            case schema::AnnotationEncodeValueMatcher::ClassMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::ClassMatcher>(i))) return true;
                break;
            case schema::AnnotationEncodeValueMatcher::MethodMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::MethodMatcher>(i))) return true;
                break;
            case schema::AnnotationEncodeValueMatcher::FieldMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::FieldMatcher>(i))) return true;
                break;
            case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::AnnotationEncodeArrayMatcher>(i))) return true;
                break;
            case schema::AnnotationEncodeValueMatcher::AnnotationMatcher:
                if (HasCompositeInternal(matcher->values()->GetAs<schema::AnnotationMatcher>(i))) return true;
                break;
            default:
                break;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::AnnotationElementMatcher *matcher) {
    if (!matcher) return false;
    if (HasCompositeInternal(matcher->name())) return true;
    switch (matcher->value_type()) {
        case schema::AnnotationEncodeValueMatcher::StringMatcher:
            return HasCompositeInternal(matcher->value_as_StringMatcher());
        case schema::AnnotationEncodeValueMatcher::ClassMatcher:
            return HasCompositeInternal(matcher->value_as_ClassMatcher());
        case schema::AnnotationEncodeValueMatcher::MethodMatcher:
            return HasCompositeInternal(matcher->value_as_MethodMatcher());
        case schema::AnnotationEncodeValueMatcher::FieldMatcher:
            return HasCompositeInternal(matcher->value_as_FieldMatcher());
        case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher:
            return HasCompositeInternal(matcher->value_as_AnnotationEncodeArrayMatcher());
        case schema::AnnotationEncodeValueMatcher::AnnotationMatcher:
            return HasCompositeInternal(matcher->value_as_AnnotationMatcher());
        default:
            return false;
    }
}

bool HasCompositeInternal(const schema::AnnotationElementsMatcher *matcher) {
    if (!matcher || !matcher->elements()) return false;
    for (int i = 0; i < matcher->elements()->size(); ++i) {
        if (HasCompositeInternal(matcher->elements()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::AnnotationMatcher *matcher) {
    if (!matcher) return false;
    return HasCompositeInternal(matcher->type())
           || HasCompositeInternal(matcher->elements())
           || HasCompositeVector(matcher->using_strings());
}

bool HasCompositeInternal(const schema::AnnotationsMatcher *matcher) {
    if (!matcher || !matcher->annotations()) return false;
    for (int i = 0; i < matcher->annotations()->size(); ++i) {
        if (HasCompositeInternal(matcher->annotations()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::InterfacesMatcher *matcher) {
    if (!matcher || !matcher->interfaces()) return false;
    for (int i = 0; i < matcher->interfaces()->size(); ++i) {
        if (HasCompositeInternal(matcher->interfaces()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::FieldsMatcher *matcher) {
    if (!matcher || !matcher->fields()) return false;
    for (int i = 0; i < matcher->fields()->size(); ++i) {
        if (HasCompositeInternal(matcher->fields()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::MethodsMatcher *matcher) {
    if (!matcher || !matcher->methods()) return false;
    for (int i = 0; i < matcher->methods()->size(); ++i) {
        if (HasCompositeInternal(matcher->methods()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::ParameterMatcher *matcher) {
    if (!matcher) return false;
    return HasCompositeInternal(matcher->parameter_type())
           || HasCompositeInternal(matcher->annotations());
}

bool HasCompositeInternal(const schema::ParametersMatcher *matcher) {
    if (!matcher || !matcher->parameters()) return false;
    for (int i = 0; i < matcher->parameters()->size(); ++i) {
        if (HasCompositeInternal(matcher->parameters()->Get(i))) {
            return true;
        }
    }
    return false;
}

bool HasCompositeInternal(const schema::UsingFieldMatcher *matcher) {
    if (!matcher) return false;
    return HasCompositeInternal(matcher->field());
}

} // namespace

bool HasComposite(const schema::StringMatcher *matcher) {
    return HasCompositeInternal(matcher);
}

bool HasComposite(const schema::ClassMatcher *matcher) {
    return HasCompositeInternal(matcher);
}

bool HasComposite(const schema::FieldMatcher *matcher) {
    return HasCompositeInternal(matcher);
}

bool HasComposite(const schema::MethodMatcher *matcher) {
    return HasCompositeInternal(matcher);
}

AnalyzeRet Analyze(const schema::ClassMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->class_name()) {
        auto string_matcher = matcher->class_name();
        if (dex_depth <= MAX_ANALYZE_TYPE_DEPTH
            && !string_matcher->ignore_case()
            && string_matcher->match_type() == schema::StringMatchType::Equal) {
            ret.declare_class.emplace_back(string_matcher->value()->string_view());
        }
    }
    if (matcher->super_class()) {
        // 父类可能定义在其它 dex 中
        auto result = Analyze(matcher->super_class(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->interfaces()) {
        // 接口可能定义在其它 dex 中
        auto result = Analyze(matcher->interfaces(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->annotations()) {
        ret.need_flags |= kClassAnnotation;
        // class 的注解必定存在于本 dex 中
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->fields()) {
        auto result = Analyze(matcher->fields(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->methods()) {
        auto result = Analyze(matcher->methods(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->using_strings()) {
        ret.need_flags |= kUsingString;
    }
    MergeAnalyzeVector(ret, matcher->all_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->any_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->none_of(), dex_depth);
    return ret;
}

AnalyzeRet Analyze(const schema::FieldMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->declaring_class()) {
        // field 定义的类必定存在于本 dex 中
        auto result = Analyze(matcher->declaring_class(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->type_class()) {
        // field 的 type 类型可能定义在其它 dex 中
        auto result = Analyze(matcher->type_class(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->annotations()) {
        ret.need_flags |= kFieldAnnotation;
        // field 的注解必定存在于本 dex 中
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->get_methods()) {
        // 需建立 using_field 的交叉引用
        ret.need_flags |= kRwFieldMethod | kMethodUsingField;
        auto result = Analyze(matcher->get_methods(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        // 交叉引用的类必定存在于其它 dex 中
        if (false) {
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    if (matcher->put_methods()) {
        // 需建立 using_field 的交叉引用
        ret.need_flags |= kRwFieldMethod | kMethodUsingField;
        auto result = Analyze(matcher->put_methods(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        // 交叉引用的类必定存在于其它 dex 中
        if (false) {
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    MergeAnalyzeVector(ret, matcher->all_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->any_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->none_of(), dex_depth);
    return ret;
}

AnalyzeRet Analyze(const schema::MethodMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->declaring_class()) {
        // method 定义的类必定存在于本 dex 中
        auto result = Analyze(matcher->declaring_class(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->return_type()) {
        // method 的 return type 类型可能定义在其它 dex 中
        auto result = Analyze(matcher->return_type(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->parameters()) {
        // method 的参数类型可能定义在其它 dex 中
        auto result = Analyze(matcher->parameters(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->annotations()) {
        ret.need_flags |= kMethodAnnotation;
        // method 的注解必定存在于本 dex 中
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->using_fields()) {
        // 不初始化 kRwFieldMethod 可能会导致复杂查询无法跳转至对应的 dexItem 执行
        ret.need_flags |= kMethodUsingField | kRwFieldMethod;
        for (auto i = 0; i < matcher->using_fields()->size(); ++i) {
            // 使用的 field 可能定义在其它 dex 中
            auto result = Analyze(matcher->using_fields()->Get(i)->field(), dex_depth + 1);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    if (matcher->invoking_methods()) {
        // 不初始化 kCallerMethod 可能会导致复杂查询无法跳转至对应的 dexItem 执行
        ret.need_flags |= kCallerMethod | kMethodInvoking;
        // invoke 的方法可能定义在其它 dex 中
        auto result = Analyze(matcher->invoking_methods(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->method_callers()) {
        // 需建立 invoking_methods 的交叉引用
        ret.need_flags |= kCallerMethod | kMethodInvoking;
        auto result = Analyze(matcher->method_callers(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        // 交叉引用的类必定存在于其它 dex 中
        if (false) {
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    if (matcher->using_strings()) {
        ret.need_flags |= kUsingString;
    }
    if (matcher->op_codes()) {
        ret.need_flags |= kOpSequence;
    }
    MergeAnalyzeVector(ret, matcher->all_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->any_of(), dex_depth);
    MergeAnalyzeVector(ret, matcher->none_of(), dex_depth);
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationEncodeArrayMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->values()) {
        for (auto i = 0; i < matcher->values()->size(); ++i) {
            auto type = matcher->values_type()->Get(i);
            switch (type) {
                case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
                    auto result = Analyze(matcher->values()->GetAs<schema::ClassMatcher>(i), dex_depth + 1);
                    ret.need_flags |= result.need_flags;
                    ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                    break;
                }
                case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
                    auto result = Analyze(matcher->values()->GetAs<schema::FieldMatcher>(i), dex_depth + 1);
                    ret.need_flags |= result.need_flags;
                    ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                    break;
                }
                case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
                    auto result = Analyze(matcher->values()->GetAs<schema::AnnotationEncodeArrayMatcher>(i), dex_depth);
                    ret.need_flags |= result.need_flags;
                    ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                    break;
                }
                case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
                    auto result = Analyze(matcher->values()->GetAs<schema::AnnotationMatcher>(i), dex_depth);
                    ret.need_flags |= result.need_flags;
                    ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                    break;
                }
                default:
                    break;
            }
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationElementMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->value()) {
        switch (matcher->value_type()) {
            case schema::AnnotationEncodeValueMatcher::ClassMatcher: {
                auto result = Analyze(matcher->value_as_ClassMatcher(), dex_depth + 1);
                ret.need_flags |= result.need_flags;
                ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::FieldMatcher: {
                auto result = Analyze(matcher->value_as_FieldMatcher(), dex_depth + 1);
                ret.need_flags |= result.need_flags;
                ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationEncodeArrayMatcher: {
                auto result = Analyze(matcher->value_as_AnnotationEncodeArrayMatcher(), dex_depth);
                ret.need_flags |= result.need_flags;
                ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                break;
            }
            case schema::AnnotationEncodeValueMatcher::AnnotationMatcher: {
                auto result = Analyze(matcher->value_as_AnnotationMatcher(), dex_depth);
                ret.need_flags |= result.need_flags;
                ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
                break;
            }
            default:
                break;
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationElementsMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->elements()) {
        for (auto i = 0; i < matcher->elements()->size(); ++i) {
            auto result = Analyze(matcher->elements()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->type()) {
        auto result = Analyze(matcher->type(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->target_element_types()) {
        ret.need_flags |= kClassAnnotation;
    }
    if (matcher->policy() != schema::RetentionPolicyType::Any) {
        ret.need_flags |= kClassAnnotation;
    }
    if (matcher->elements()) {
        auto result = Analyze(matcher->elements(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationsMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->annotations()) {
        for (auto i = 0; i < matcher->annotations()->size(); ++i) {
            auto result = Analyze(matcher->annotations()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::InterfacesMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->interfaces()) {
        for (auto i = 0; i < matcher->interfaces()->size(); ++i) {
            auto result = Analyze(matcher->interfaces()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::FieldsMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    // using_fields, class_fields
    AnalyzeRet ret{};
    if (matcher->fields()) {
        for (auto i = 0; i < matcher->fields()->size(); ++i) {
            auto result = Analyze(matcher->fields()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::MethodsMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    // rw_methods, invoking_methods, method_callers, class_methods
    AnalyzeRet ret{};
    if (matcher->methods()) {
        for (auto i = 0; i < matcher->methods()->size(); ++i) {
            auto result = Analyze(matcher->methods()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

AnalyzeRet Analyze(const schema::ParameterMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->parameter_type()) {
        auto result = Analyze(matcher->parameter_type(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->annotations()) {
        ret.need_flags |= kParamAnnotation;
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    return ret;
}

AnalyzeRet Analyze(const schema::ParametersMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{};
    if (matcher->parameters()) {
        for (auto i = 0; i < matcher->parameters()->size(); ++i) {
            auto result = Analyze(matcher->parameters()->Get(i), dex_depth);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    return ret;
}

} // namespace dexkit
