/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include "common.h"
#include "dex_format.h"
#include "dex_ir.h"

#include <cassert>
#include <cstdlib>
#include <map>
#include <memory>

namespace dex {

// Provides both a low level iteration over the .dex
// structures and incremental .dex IR creation.
//
// NOTES:
// - only little-endian .dex files and host machines are supported
// - aggresive structure validation & minimal semantic validation
//
class Reader {
public:
    Reader(const dex::u1 *image, size_t size);

    ~Reader() = default;

    Reader(Reader &&) = default;

    Reader &operator=(Reader &&) = default;

    // No copy semantics
    Reader(const Reader &) = delete;

    Reader &operator=(const Reader &) = delete;

public:
    // Low level dex format interface
    [[nodiscard]] const dex::Header *Header() const { return header_; }

    [[nodiscard]] const char *GetStringMUTF8(dex::u4 index) const;

    [[nodiscard]] slicer::ArrayView<const dex::ClassDef> ClassDefs() const;

    [[nodiscard]] slicer::ArrayView<const dex::StringId> StringIds() const;

    [[nodiscard]] slicer::ArrayView<const dex::TypeId> TypeIds() const;

    [[nodiscard]] slicer::ArrayView<const dex::FieldId> FieldIds() const;

    [[nodiscard]] slicer::ArrayView<const dex::MethodId> MethodIds() const;

    [[nodiscard]] slicer::ArrayView<const dex::ProtoId> ProtoIds() const;

    [[nodiscard]] const dex::MapList *DexMapList() const;

    // IR creation interface
    [[nodiscard]] std::shared_ptr<ir::DexFile> GetIr() const { return dex_ir_; }

    void CreateFullIr();

    void CreateClassIr(dex::u4 index);

    dex::u4 FindClassIndex(const char *class_descriptor) const;

    // Convert a file pointer (absolute offset) to an in-memory pointer
    template<class T>
    const T *ptr(u4 offset) const {
        SLICER_CHECK_GE(offset, 0 && offset + sizeof(T) <= size_);
        return reinterpret_cast<const T *>(image_ + offset);
    }

    // Convert a data section file pointer (absolute offset) to an in-memory pointer
    // (offset should be inside the data section)
    template<class T>
    [[nodiscard]]  const T *dataPtr(size_t offset) const {
        SLICER_CHECK_GE(offset, header_->data_off && offset + sizeof(T) <= size_);
        return reinterpret_cast<const T *>(image_ + offset);
    }

private:
    // Internal access to IR nodes for indexed .dex structures
    ir::Class *GetClass(dex::u4 index);

    ir::Type *GetType(dex::u4 index);

    ir::FieldDecl *GetFieldDecl(dex::u4 index);

    ir::MethodDecl *GetMethodDecl(dex::u4 index);

    ir::Proto *GetProto(dex::u4 index);

    ir::String *GetString(dex::u4 index);

    // Parsing annotations
    ir::AnnotationsDirectory *ExtractAnnotations(dex::u4 offset);

    ir::Annotation *ExtractAnnotationItem(dex::u4 offset);

    ir::AnnotationSet *ExtractAnnotationSet(dex::u4 offset);

    ir::AnnotationSetRefList *ExtractAnnotationSetRefList(dex::u4 offset);

    ir::FieldAnnotation *ParseFieldAnnotation(const dex::u1 **pptr);

    ir::MethodAnnotation *ParseMethodAnnotation(const dex::u1 **pptr);

    ir::ParamAnnotation *ParseParamAnnotation(const dex::u1 **pptr);

    ir::EncodedField *ParseEncodedField(const dex::u1 **pptr, dex::u4 *baseIndex);

    ir::Annotation *ParseAnnotation(const dex::u1 **pptr);

    // Parse encoded values and arrays
    ir::EncodedValue *ParseEncodedValue(const dex::u1 **pptr);

    ir::EncodedArray *ParseEncodedArray(const dex::u1 **pptr);

    ir::EncodedArray *ExtractEncodedArray(dex::u4 offset);

    // Parse root .dex structures
    ir::Class *ParseClass(dex::u4 index);

    ir::EncodedMethod *ParseEncodedMethod(const dex::u1 **pptr, dex::u4 *baseIndex);

    ir::Type *ParseType(dex::u4 index);

    ir::FieldDecl *ParseFieldDecl(dex::u4 index);

    ir::MethodDecl *ParseMethodDecl(dex::u4 index);

    ir::TypeList *ExtractTypeList(dex::u4 offset);

    ir::Proto *ParseProto(dex::u4 index);

    ir::String *ParseString(dex::u4 index);

    // Parse code and debug information
    ir::DebugInfo *ExtractDebugInfo(dex::u4 offset);

    ir::Code *ExtractCode(dex::u4 offset);

    void ParseInstructions(slicer::ArrayView<const dex::u2> code);

    // Map an indexed section to an ArrayView<T>
    template<class T>
    slicer::ArrayView<const T> section(int offset, int count) const {
        return slicer::ArrayView<const T>(ptr<T>(offset), count);
    }

    // Simple accessor for a MUTF8 string data
    [[nodiscard]] const dex::u1 *GetStringData(dex::u4 index) const {
        auto &stringId = StringIds()[index];
        return dataPtr<dex::u1>(stringId.string_data_off);
    }

    void ValidateHeader();

private:
    // the in-memory .dex image
    const dex::u1 *image_;
    size_t size_;

    // .dex image header
    const dex::Header *header_;

    // .dex IR associated with the reader
    std::shared_ptr<ir::DexFile> dex_ir_;

    // maps for de-duplicating items identified by file pointers
    std::map<dex::u4, ir::TypeList *> type_lists_;
    std::map<dex::u4, ir::Annotation *> annotations_;
    std::map<dex::u4, ir::AnnotationSet *> annotation_sets_;
    std::map<dex::u4, ir::AnnotationsDirectory *> annotations_directories_;
    std::map<dex::u4, ir::EncodedArray *> encoded_arrays_;
};

}  // namespace dex
