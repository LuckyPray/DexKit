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

#pragma once

#include "schema/querys_generated.h"
#include "schema/matchers_generated.h"

// max depth of analyze type.
// type name must in this dex->TypeIds()
// type class maybe in this dex->TypeIds() or other dex->TypeIds()
#define MAX_ANALYZE_TYPE_DEPTH 2

namespace dexkit {

// optional init
const uint32_t kClassAnnotation = 0x0010;
const uint32_t kFieldAnnotation = 0x0020;
const uint32_t kMethodAnnotation = 0x0040;
const uint32_t kParamAnnotation = 0x0080;

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