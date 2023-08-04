#pragma once

#include <string_view>
#include <vector>

#include "phmap.h"
#include "slicer/reader.h"

#include "beans.h"
#include "byte_code_util.h"
#include "constant.h"
#include "error.h"
#include "file_helper.h"

#include "querys_generated.h"
#include "results_generated.h"

namespace dexkit {

class DexItem {
public:

    explicit DexItem(uint8_t *data, size_t size);
    explicit DexItem(std::unique_ptr<MemMap> mmap);

    ~DexItem() = default;

    DexItem(DexItem &&) = default;
    DexItem &operator=(DexItem &&) = default;

    DexItem(const DexItem&) = delete;
    DexItem& operator=(const DexItem&) = delete;

    [[nodiscard]] MemMap *GetImage() const {
        return _image.get();
    }

    std::vector<ClassBean> FindClass(const schema::FindClass *query);
    std::vector<MethodBean> FindMethod(const schema::FindMethod *query);
    std::vector<FieldBean> FindField(const schema::FindField *query);
    std::vector<ClassBean> BatchFindClassUsingStrings(const schema::BatchFindClassUsingStrings *query);
    std::vector<MethodBean> BatchFindMethodUsingStrings(const schema::BatchFindMethodUsingStrings *query);

private:

    int InitCache();

private:
    std::unique_ptr<MemMap> _image;
    dex::Reader reader;

    uint32_t dex_flag = 0;

    // string constants, sorted by string value
    std::vector<std::string_view> strings;
    std::vector<std::string_view> type_names;
    phmap::flat_hash_map<std::string_view /*type_name*/, uint32_t /*type_id*/> type_ids_map;
    // dex declared types flag
    std::vector<bool /*declared_flag*/> type_declared_flag;
    // class source file name, eg. "HelloWorld.java", maybe obfuscated
    std::vector<std::string_view> class_source_files;
    std::vector<uint32_t /*access_flag*/> class_access_flags;
    std::vector<std::vector<uint32_t /*method_id*/>> class_method_ids;
    std::vector<uint32_t /*access_flag*/> method_access_flags;
    std::vector<std::vector<uint32_t /*field_id*/>> class_field_ids;
    std::vector<uint32_t /*access_flag*/> field_access_flags;
    std::vector<const dex::Code *> method_codes;
    std::vector<const dex::TypeList *> proto_type_list;
    std::vector<std::optional<std::vector<uint8_t /*opcode*/>>> method_opcode_seq;
    std::vector<const ir::AnnotationsDirectory *> class_annotations;
};

} // namespace dexkit