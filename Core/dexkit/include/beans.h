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

#include <variant>

#include "schema/enums_generated.h"
#include "schema/results_generated.h"

namespace dexkit {

class ClassBean {
public:
    uint32_t id = -1;
    uint32_t dex_id = -1;
    std::string_view source_file;
    uint32_t access_flags = 0;
    std::string_view dex_descriptor;
    uint32_t super_class_id = -1;
    std::vector<uint32_t> interface_ids;
    std::vector<uint32_t> method_ids;
    std::vector<uint32_t> field_ids;

public:
    flatbuffers::Offset<schema::ClassMeta>
    CreateClassMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class MethodBean {
public:
    uint32_t id = -1;
    uint32_t dex_id = -1;
    uint32_t class_id = -1;
    uint32_t access_flags = 0;
    std::string_view dex_descriptor;
    uint32_t return_type = -1;
    std::vector<uint32_t> parameter_types;

public:
    flatbuffers::Offset<schema::MethodMeta>
    CreateMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class FieldBean {
public:
    uint32_t id = -1;
    uint32_t dex_id = -1;
    uint32_t class_id = -1;
    uint32_t access_flags = 0;
    std::string_view dex_descriptor;
    uint32_t type_id = -1;

public:
    flatbuffers::Offset<schema::FieldMeta>
    CreateFieldMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationBean;
class AnnotationElementBean;
class AnnotationEncodeArrayBean;

using AnnotationEncodeValue = std::variant<
        int8_t /*byte_value*/,
        int16_t /*short_value, char_value*/,
        int32_t /*int_value*/,
        int64_t /*long_value*/,
        float /*float_value*/,
        double /*double_value*/,
        std::string_view /*string_value*/,
        std::unique_ptr<ClassBean> /*type_value*/,
        std::unique_ptr<MethodBean> /*method_value*/,
        std::unique_ptr<FieldBean> /*enum_value*/,
        std::unique_ptr<AnnotationEncodeArrayBean> /*array_value*/,
        std::unique_ptr<AnnotationBean> /*annotation_value*/,
        bool /*bool_value*/>;

class AnnotationEncodeValueBean {
public:
    schema::AnnotationEncodeValueType type;
    AnnotationEncodeValue value;

public:
    flatbuffers::Offset<schema::AnnotationEncodeValueMeta>
    CreateAnnotationEncodeValueMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationEncodeArrayBean {
public:
    std::vector<AnnotationEncodeValueBean> values;

public:
    flatbuffers::Offset<schema::AnnotationEncodeArray>
    CreateAnnotationEncodeArray(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationBean {
public:
    uint32_t dex_id;
    uint32_t type_id;
    std::string_view type_descriptor;
    schema::AnnotationVisibilityType visibility;
    std::vector<AnnotationElementBean> elements;

public:
    flatbuffers::Offset<schema::AnnotationMeta>
    CreateAnnotationMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class AnnotationElementBean {
public:
    std::string_view name;
    AnnotationEncodeValueBean value;

public:
    flatbuffers::Offset<schema::AnnotationElementMeta>
    CreateAnnotationElementMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindClassItemBean {
public:
    std::string_view union_key;
    std::vector<ClassBean> classes;

public:
    flatbuffers::Offset<schema::BatchClassMeta>
    CreateBatchClassMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

class BatchFindMethodItemBean {
public:
    std::string_view union_key;
    std::vector<MethodBean> methods;

public:
    flatbuffers::Offset<schema::BatchMethodMeta>
    CreateBatchMethodMeta(flatbuffers::FlatBufferBuilder &fbb) const;
};

}