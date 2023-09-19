#include "analyze.h"

namespace dexkit {

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
        // class 的注解必定存在于本 dex 中
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->fields()) {
        ret.need_flags |= kClassField;
        auto result = Analyze(matcher->fields(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->methods()) {
        ret.need_flags |= kClassMethod;
        auto result = Analyze(matcher->methods(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->using_strings()) {
        ret.need_flags |= kUsingString;
    }
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
        // method 的注解必定存在于本 dex 中
        auto result = Analyze(matcher->annotations(), dex_depth);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
    }
    if (matcher->using_fields()) {
        ret.need_flags |= kMethodUsingField;
        for (auto i = 0; i < matcher->using_fields()->size(); ++i) {
            // 使用的 field 可能定义在其它 dex 中
            auto result = Analyze(matcher->using_fields()->Get(i)->field(), dex_depth + 1);
            ret.need_flags |= result.need_flags;
            ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
        }
    }
    if (matcher->invoking_methods()) {
        ret.need_flags |= kMethodInvoking;
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
    return ret;
}

AnalyzeRet Analyze(const schema::AnnotationEncodeArrayMatcher *matcher, int dex_depth) {
    if (!matcher) return {};
    AnalyzeRet ret{.need_flags = kAnnotation};
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
    AnalyzeRet ret{.need_flags = kAnnotation};
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
    AnalyzeRet ret{.need_flags = kAnnotation};
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
    AnalyzeRet ret{.need_flags = kAnnotation};
    if (matcher->type()) {
        auto result = Analyze(matcher->type(), dex_depth + 1);
        ret.need_flags |= result.need_flags;
        ret.declare_class.insert(ret.declare_class.end(), result.declare_class.begin(), result.declare_class.end());
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
    AnalyzeRet ret{.need_flags = kAnnotation};
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
    AnalyzeRet ret{.need_flags = kInterface};
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
