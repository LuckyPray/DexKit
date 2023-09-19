#pragma once

#include "schema/querys_generated.h"
#include "schema/matchers_generated.h"

// max depth of analyze type.
// type name must in this dex->TypeIds()
// type class maybe in this dex->TypeIds() or other dex->TypeIds()
#define MAX_ANALYZE_TYPE_DEPTH 2

namespace dexkit {

// must init before use
const uint32_t kString = 0x0001;
const uint32_t kClass = 0x0002;
const uint32_t kField = 0x0004;
const uint32_t kMethod = 0x0008;

const uint32_t kInterface = 0x0010;
const uint32_t kClassField = 0x0020;
const uint32_t kClassMethod = 0x0040;

// optional init
const uint32_t kAnnotation = 0x0080;

// code
const uint32_t kUsingString = 0x0100;
const uint32_t kMethodInvoking = 0x0200;
const uint32_t kCallerMethod = 0x0400; // cross
const uint32_t kMethodUsingField = 0x0800;
const uint32_t kRwFieldMethod = 0x1000; // cross
const uint32_t kOpSequence = 0x2000;
const uint32_t kUsingNumber = 0x4000;

struct AnalyzeRet {
    uint32_t need_flags = 0;
    std::vector<std::string_view> declare_class;
};

AnalyzeRet Analyze(const schema::ClassMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::FieldMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::MethodMatcher *matcher, int dex_depth);

AnalyzeRet Analyze(const schema::AnnotationEncodeArrayMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::AnnotationElementMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::AnnotationElementsMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::AnnotationMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::AnnotationsMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::InterfacesMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::FieldsMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::MethodsMatcher *matcher, int dex_depth);

AnalyzeRet Analyze(const schema::ParameterMatcher *matcher, int dex_depth);
AnalyzeRet Analyze(const schema::ParametersMatcher *matcher, int dex_depth);

} // namespace dexkit