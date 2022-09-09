#include "dex_kit.h"
#include "file_helper.h"
#include "acdat/Builder.h"
#include "thread_pool.h"
#include "byte_code_util.h"
#include "opcode_util.h"
#include "code_format.h"
#include <algorithm>

namespace dexkit {

using namespace acdat;

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    auto map = MemMap(apk_path);
    if (!map.ok()) {
        return;
    }
    auto zip_file = ZipFile::Open(map);
    maps_.emplace_back(std::move(map));
    if (zip_file) {
        std::vector<std::pair<int, ZipLocalFile *>> dexs;
        for (int idx = 1;; ++idx) {
            auto entry_name = "classes" + (idx == 1 ? std::string() : std::to_string(idx)) + ".dex";
            auto entry = zip_file->Find(entry_name);
            if (!entry) {
                break;
            }
            dexs.emplace_back(idx, entry);
        }
        dex_images_.resize(dexs.size());
        maps_.resize(dexs.size() + 1);
        ThreadPool pool(unzip_thread_num == -1 ? thread_num_ : unzip_thread_num);
        for (auto &dex_pair: dexs) {
            pool.enqueue([&dex_pair, this]() {
                auto dex_image = dex_pair.second->uncompress();
                if (!dex_image.ok()) {
                    return;
                }
                dex_images_[dex_pair.first - 1] = std::make_pair(dex_image.addr(), dex_image.len());
                maps_[dex_pair.first] = std::move(dex_image);
            });
        }
    }
    InitImages();
}

DexKit::DexKit(std::vector<std::pair<const void *, size_t>> &dex_images) {
    dex_images_.resize(dex_images.size());
    for (int i = 0; i < dex_images.size(); ++i) {
        dex_images_[i] = dex_images[i];
    }
    InitImages();
}

std::map<std::string, std::vector<std::string>>
DexKit::LocationClasses(std::map<std::string, std::set<std::string>> &location_map, bool advanced_match) {
    auto acdat = AhoCorasickDoubleArrayTrie<std::string>();
    std::map<std::string, std::string> buildMap;
    std::map<std::string, uint8_t> flag_map;
    for (auto &[name, str_set]: location_map) {
        for (auto &str: str_set) {
            uint32_t l = 0, r = str.size();
            uint8_t flag = 0;
            if (advanced_match) {
                if (str[0] == '^') {
                    l = 1;
                    flag |= 1;
                }
                if (str[str.size() - 1] == '$') {
                    r = str.size() - 1;
                    flag |= 2;
                }
            }
            auto origin_str = str.substr(l, r - l);
            flag_map[origin_str] = flag;
            buildMap[origin_str] = origin_str;
        }
    }
    Builder<std::string>().build(buildMap, &acdat);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue([&acdat, &location_map, dex_idx, this, &flag_map]() {
            InitCached(dex_idx);
            auto &method_codes = method_codes_[dex_idx];
            auto &class_method_ids = class_method_ids_[dex_idx];
            auto &type_names = type_names_[dex_idx];
            auto &strings = strings_[dex_idx];

            std::map<std::string, std::vector<std::string>> result;

            std::map<dex::u4, std::string> string_map;
            for (int index = 0; index < strings.size(); ++index) {
                auto string = strings[index];
                std::function<void(int, int, std::string)> callback =
                        [&string_map, index, &flag_map, &string](int begin, int end, const std::string &value) -> void {
                            auto flag = flag_map[value];
                            if ((flag & 1 && begin != 0) ||
                                (flag >> 1 && end != string.size())) {
                                return;
                            }
                            string_map[index] = ((flag & 1) ? "^" : "") + value + ((flag >> 1) ? "$" : "");
                        };
                acdat.parseText(string.data(), callback);
            }

            if (string_map.empty()) {
                return std::map<std::string, std::vector<std::string>>();
            }
            for (int i = 0; i < type_names.size(); ++i) {
                if (class_method_ids[i].empty()) {
                    continue;
                }
                std::set<std::string> search_set;
                for (auto method_idx: class_method_ids[i]) {
                    auto &code = method_codes[method_idx];
                    if (code == nullptr) {
                        continue;
                    }
                    auto p = code->insns;
                    auto end_p = p + code->insns_size;
                    while (p < end_p) {
                        auto op = *p & 0xff;
                        auto ptr = p;
                        auto width = GetBytecodeWidth(ptr++);
                        switch (op) {
                            case 0x1a: {
                                auto index = ReadShort(ptr);
                                if (string_map.find(index) != string_map.end()) {
                                    search_set.emplace(string_map[index]);
                                }
                                break;
                            }
                            case 0x1b: {
                                auto index = ReadInt(ptr);
                                if (string_map.find(index) != string_map.end()) {
                                    search_set.emplace(string_map[index]);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        p += width;
                    }
                }
                if (search_set.empty()) continue;
                for (auto &[real_class, value_set]: location_map) {
                    std::vector<std::string> vec;
                    std::set_intersection(search_set.begin(), search_set.end(), value_set.begin(), value_set.end(),
                                          std::inserter(vec, vec.begin()));
                    if (vec.size() == value_set.size()) {
                        result[real_class].emplace_back(type_names[i]);
                    }
                }
            }
            return result;
        }));
    }
    std::map<std::string, std::vector<std::string>> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &[key, value]: r) {
            for (auto &cls_name: value) {
                result[key].emplace_back(cls_name);
            }
        }
    }
    for (auto &[key, value]: location_map) {
        if (result.find(key) == result.end()) {
            result[key] = std::vector<std::string>();
        }
    }
    return result;
}

std::map<std::string, std::vector<std::string>>
DexKit::LocationMethods(std::map<std::string, std::set<std::string>> &location_map, bool advanced_match) {
    auto acdat = AhoCorasickDoubleArrayTrie<std::string>();
    std::map<std::string, std::string> buildMap;
    std::map<std::string, uint8_t> flag_map;
    for (auto &[name, str_set]: location_map) {
        for (auto &str: str_set) {
            uint32_t l = 0, r = str.size();
            uint8_t flag = 0;
            if (advanced_match) {
                if (str[0] == '^') {
                    l = 1;
                    flag |= 1;
                }
                if (str[str.size() - 1] == '$') {
                    r = str.size() - 1;
                    flag |= 2;
                }
            }
            auto origin_str = str.substr(l, r - l);
            flag_map[origin_str] = flag;
            buildMap[origin_str] = origin_str;
        }
    }
    Builder<std::string>().build(buildMap, &acdat);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue([&acdat, &location_map, dex_idx, this, &flag_map]() {
            InitCached(dex_idx);
            auto &method_codes = method_codes_[dex_idx];
            auto &class_method_ids = class_method_ids_[dex_idx];
            auto &type_names = type_names_[dex_idx];
            auto &strings = strings_[dex_idx];

            std::map<std::string, std::vector<std::string>> result;

            std::map<dex::u4, std::string> string_map;
            for (int index = 0; index < strings.size(); ++index) {
                auto string = strings[index];
                std::function<void(int, int, std::string)> callback =
                        [&string_map, index, &flag_map, &string](int begin, int end, const std::string &value) -> void {
                            auto flag = flag_map[value];
                            if ((flag & 1 && begin != 0) ||
                                (flag >> 1 && end != string.size())) {
                                return;
                            }
                            string_map[index] = ((flag & 1) ? "^" : "") + value + ((flag >> 1) ? "$" : "");
                        };
                acdat.parseText(string.data(), callback);
            }

            if (string_map.empty()) {
                return std::map<std::string, std::vector<std::string>>();
            }
            for (int i = 0; i < type_names.size(); ++i) {
                if (class_method_ids[i].empty()) {
                    continue;
                }
                for (auto method_idx: class_method_ids[i]) {
                    std::set<std::string> search_set;
                    auto &code = method_codes[method_idx];
                    if (code == nullptr) {
                        continue;
                    }
                    auto p = code->insns;
                    auto end_p = p + code->insns_size;
                    while (p < end_p) {
                        auto op = *p & 0xff;
                        auto ptr = p;
                        auto width = GetBytecodeWidth(ptr++);
                        switch (op) {
                            case 0x1a: {
                                auto index = ReadShort(ptr);
                                if (string_map.find(index) != string_map.end()) {
                                    search_set.emplace(string_map[index]);
                                }
                                break;
                            }
                            case 0x1b: {
                                auto index = ReadInt(ptr);
                                if (string_map.find(index) != string_map.end()) {
                                    search_set.emplace(string_map[index]);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                        p += width;
                    }
                    if (search_set.empty()) continue;
                    for (auto &[real_method, value_set]: location_map) {
                        std::vector<std::string> vec;
                        std::set_intersection(search_set.begin(), search_set.end(), value_set.begin(), value_set.end(),
                                              std::inserter(vec, vec.begin()));
                        if (vec.size() == value_set.size()) {
                            result[real_method].emplace_back(GetMethodDescriptor(dex_idx, method_idx));
                        }
                    }
                }
            }
            return result;
        }));
    }
    std::map<std::string, std::vector<std::string>> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &[key, value]: r) {
            for (auto &cls_name: value) {
                result[key].emplace_back(cls_name);
            }
        }
    }
    for (auto &[key, value]: location_map) {
        if (result.find(key) == result.end()) {
            result[key] = std::vector<std::string>();
        }
    }
    return result;
}

std::vector<std::string>
DexKit::FindMethodInvoked(std::string method_descriptor,
                          std::string class_decl_name,
                          std::string method_name,
                          std::string result_class_decl,
                          const std::vector<std::string> &param_class_decls,
                          const std::vector<size_t> &dex_priority,
                          bool match_any_param_if_param_vector_empty) {
    std::string class_desc, return_desc;
    std::vector<std::string> param_descs;
    if (!method_descriptor.empty()) {
        auto extract_tuple = ExtractMethodDescriptor(method_descriptor);
        class_desc = std::get<0>(extract_tuple);
        method_name = std::get<1>(extract_tuple);
        return_desc = std::get<2>(extract_tuple);
        param_descs = std::get<3>(extract_tuple);
    } else {
        class_desc = GetClassDescriptor(class_decl_name);
        return_desc = DeclToMatchDescriptor(result_class_decl);
        for (auto &param_decl: param_class_decls) {
            param_descs.emplace_back(DeclToMatchDescriptor(param_decl));
        }
    }
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param_if_param_vector_empty]() {
                    InitCached(dex_idx);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto class_idx = dex::kNoIndex;
                    if (!class_desc.empty()) {
                        class_idx = FindTypeIdx(dex_idx, class_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!return_desc.empty()) {
                        return_type = FindTypeIdx(dex_idx, return_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: param_descs) {
                        uint32_t type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        param_types.emplace_back(type);
                    }
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            auto &code = method_codes[method_idx];
                            if (code == nullptr) {
                                continue;
                            }
                            auto p = code->insns;
                            auto end_p = p + code->insns_size;
                            while (p < end_p) {
                                auto op = *p & 0xff;
                                auto ptr = p;
                                auto width = GetBytecodeWidth(ptr++);
                                switch (op) {
                                    case 0x6e:
                                    case 0x6f:
                                    case 0x70:
                                    case 0x71:
                                    case 0x72: {
                                        auto index = ReadShort(ptr);
                                        if (IsMethodMatch(dex_idx, index, class_idx, match_shorty, method_name,
                                                          return_type, param_types,
                                                          match_any_param_if_param_vector_empty)) {
                                            auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                            result.emplace_back(descriptor);
                                            goto label;
                                        }
                                        break;
                                    }
                                    default:
                                        break;
                                }
                                p += width;
                            }
                            label:;
                        }
                    }
                    return result;
                })
        );
    }
    std::vector<std::string> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &desc: r) {
            result.emplace_back(desc);
        }
    }
    return result;
}

std::vector<std::string>
DexKit::FindMethodUsedString(std::string str,
                             std::string class_decl_name,
                             std::string method_name,
                             std::string result_class_decl,
                             const std::vector<std::string> &param_class_decls,
                             const std::vector<size_t> &dex_priority,
                             bool match_any_param_if_param_vector_empty,
                             bool advanced_match) {
    std::string class_desc, return_desc;
    std::vector<std::string> param_descs;
    class_desc = GetClassDescriptor(class_decl_name);
    return_desc = DeclToMatchDescriptor(result_class_decl);
    for (auto &param_decl: param_class_decls) {
        param_descs.emplace_back(DeclToMatchDescriptor(param_decl));
    }
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param_if_param_vector_empty, &advanced_match, &str]() {
                    InitCached(dex_idx);
                    auto &strings = strings_[dex_idx];
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    uint8_t flag = 0;
                    if (advanced_match) {
                        if (str[0] == '^') {
                            flag |= 1;
                        }
                        if (str[str.size() - 1] == '$') {
                            flag |= 2;
                        }
                    }
                    auto real_str = str.substr(flag & 1, str.size() - ((flag >> 1) + (flag & 1)));
                    std::set<uint32_t> matched_strings;
                    for (int str_idx = 0; str_idx < strings.size(); ++str_idx) {
                        auto &string = strings[str_idx];
                        auto find_idx = string.find(real_str);
                        if (find_idx == std::string::npos ||
                            (flag & 1 && find_idx != 0) ||
                            (flag & 2 && find_idx != string.size() - real_str.size())) {
                            continue;
                        }
                        matched_strings.emplace(str_idx);
                    }
                    if (matched_strings.empty()) {
                        return std::vector<std::string>();
                    }

                    auto class_idx = dex::kNoIndex;
                    if (!class_desc.empty()) {
                        class_idx = FindTypeIdx(dex_idx, class_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!return_desc.empty()) {
                        return_type = FindTypeIdx(dex_idx, return_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: param_descs) {
                        uint32_t type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        param_types.emplace_back(type);
                    }
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx, method_idx, class_idx, match_shorty, method_name,
                                               return_type, param_types, match_any_param_if_param_vector_empty)) {
                                continue;
                            }
                            auto &code = method_codes[method_idx];
                            if (code == nullptr) {
                                continue;
                            }
                            auto p = code->insns;
                            auto end_p = p + code->insns_size;

                            while (p < end_p) {
                                auto op = *p & 0xff;
                                auto ptr = p;
                                auto width = GetBytecodeWidth(ptr++);
                                switch (op) {
                                    case 0x1a: {
                                        auto index = ReadShort(ptr);
                                        if (matched_strings.find(index) != matched_strings.end()) {
                                            auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                            result.emplace_back(descriptor);
                                            goto label;
                                        }
                                        break;
                                    }
                                    case 0x1b: {
                                        auto index = ReadInt(ptr);
                                        if (matched_strings.find(index) != matched_strings.end()) {
                                            auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                            result.emplace_back(descriptor);
                                            goto label;
                                        }
                                        break;
                                    }
                                    default:
                                        break;
                                }
                                p += width;
                            }
                            label:;
                        }
                    }
                    return result;
                })
        );
    }
    std::vector<std::string> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &desc: r) {
            result.emplace_back(desc);
        }
    }
    return result;
}

std::vector<std::string>
DexKit::FindMethod(std::string class_decl_name,
                   std::string method_name,
                   std::string result_class_decl,
                   const std::vector<std::string> &param_class_decls,
                   const std::vector<size_t> &dex_priority,
                   bool match_any_param_if_param_vector_empty) {
    std::string class_desc, return_desc;
    std::vector<std::string> param_descs;
    class_desc = GetClassDescriptor(class_decl_name);
    return_desc = DeclToMatchDescriptor(result_class_decl);
    for (auto &param_decl: param_class_decls) {
        param_descs.emplace_back(DeclToMatchDescriptor(param_decl));
    }
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param_if_param_vector_empty]() {
                    InitCached(dex_idx);
                    auto &strings = strings_[dex_idx];
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto class_idx = dex::kNoIndex;
                    if (!class_desc.empty()) {
                        class_idx = FindTypeIdx(dex_idx, class_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!return_desc.empty()) {
                        return_type = FindTypeIdx(dex_idx, return_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: param_descs) {
                        uint32_t type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        param_types.emplace_back(type);
                    }
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (IsMethodMatch(dex_idx, method_idx, class_idx, match_shorty, method_name,
                                              return_type, param_types, match_any_param_if_param_vector_empty)) {
                                result.emplace_back(GetMethodDescriptor(dex_idx, method_idx));
                            }
                        }
                    }
                    return result;
                })
        );
    }
    std::vector<std::string> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &desc: r) {
            result.emplace_back(desc);
        }
    }
    return result;
}

std::vector<std::string>
DexKit::FindSubClasses(std::string class_name) {
    auto class_descriptor = GetClassDescriptor(class_name);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_descriptor]() {
                    InitCached(dex_idx);
                    auto &reader = readers_[dex_idx];
                    auto &type_names = type_names_[dex_idx];

                    auto class_type_idx = dex::kNoIndex;
                    for (int i = 0; i < type_names.size(); ++i) {
                        if (type_names[i] == class_descriptor) {
                            class_type_idx = i;
                            break;
                        }
                    }
                    if (class_type_idx == dex::kNoIndex) {
                        return std::vector<std::string>();
                    }
                    std::vector<std::string> result;
                    for (auto &class_def: reader.ClassDefs()) {
                        if (class_def.interfaces_off > 0) {
                            auto types = reader.dataPtr<dex::TypeList>(class_def.interfaces_off);
                            for (int i = 0; i < types->size; ++i) {
                                auto type_idx = types->list[i].type_idx;
                                if (type_idx == class_type_idx) {
                                    result.emplace_back(type_names[class_def.class_idx]);
                                    break;
                                }
                            }
                        }
                        if (class_def.superclass_idx == class_type_idx) {
                            result.emplace_back(type_names[class_def.class_idx]);
                        }
                    }
                    return result;
                })
        );
    }
    std::vector<std::string> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &desc: r) {
            result.emplace_back(desc);
        }
    }
    return result;
}

std::vector<std::string>
DexKit::FindMethodOpPrefixSeq(const std::vector<uint8_t> &op_prefix_seq,
                              std::string class_decl_name,
                              std::string method_name,
                              std::string result_class_decl,
                              const std::vector<std::string> &param_class_decls,
                              const std::vector<size_t> &dex_priority,
                              bool match_any_param_if_param_vector_empty) {
    std::string class_desc, return_desc;
    std::vector<std::string> param_descs;
    class_desc = GetClassDescriptor(class_decl_name);
    return_desc = DeclToMatchDescriptor(result_class_decl);
    for (auto &param_decl: param_class_decls) {
        param_descs.emplace_back(DeclToMatchDescriptor(param_decl));
    }
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param_if_param_vector_empty, &op_prefix_seq]() {
                    InitCached(dex_idx);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];

                    auto class_idx = dex::kNoIndex;
                    if (!class_desc.empty()) {
                        class_idx = FindTypeIdx(dex_idx, class_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!return_desc.empty()) {
                        return_type = FindTypeIdx(dex_idx, return_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: param_descs) {
                        uint32_t type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        param_types.emplace_back(type);
                    }
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx, method_idx, class_idx, match_shorty, method_name,
                                               return_type, param_types, match_any_param_if_param_vector_empty)) {
                                continue;
                            }
                            auto &code = method_codes[method_idx];
                            if (code == nullptr) {
                                continue;
                            }
                            auto p = code->insns;
                            auto end_p = p + code->insns_size;
                            int op_index = 0;
                            while (p < end_p) {
                                auto op = *p & 0xff;
                                auto ptr = p;
                                auto width = GetBytecodeWidth(ptr++);
                                if (op_prefix_seq[op_index++] != op) {
                                    break;
                                }
                                if (op_prefix_seq.size() == op_index) {
                                    auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                    result.emplace_back(descriptor);
                                    break;
                                }
                                p += width;
                            }
                        }
                    }
                    return result;
                })
        );
    }
    std::vector<std::string> result;
    for (auto &f: futures) {
        auto r = f.get();
        for (auto &desc: r) {
            result.emplace_back(desc);
        }
    }
    return result;
}


void DexKit::InitImages() {
    for (auto &[image, size]: dex_images_) {
        auto reader = dex::Reader((const dex::u1 *) image, size);
        readers_.emplace_back(std::move(reader));
    }
    strings_.resize(readers_.size());
    type_names_.resize(readers_.size());
    class_method_ids_.resize(readers_.size());
    method_codes_.resize(readers_.size());
    proto_type_list_.resize(readers_.size());

    init_flags_.resize(readers_.size(), false);
}

std::string DexKit::GetClassDescriptor(std::string &class_name) {
    std::replace(class_name.begin(), class_name.end(), '.', '/');
    if (class_name.length() > 0 && class_name[0] != 'L') {
        class_name = "L" + class_name + ";";
    }
    return class_name;
}

void DexKit::InitCached(size_t dex_idx) {
    if (init_flags_[dex_idx]) return;
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &type_names = type_names_[dex_idx];
    auto &class_method_ids = class_method_ids_[dex_idx];
    auto &method_codes = method_codes_[dex_idx];
    auto &proto_type_list = proto_type_list_[dex_idx];

    strings.resize(reader.StringIds().size());
    type_names.resize(reader.TypeIds().size());
    class_method_ids.resize(reader.TypeIds().size());
    method_codes.resize(reader.MethodIds().size(), nullptr);
    proto_type_list.resize(reader.ProtoIds().size());

    auto strings_it = strings.begin();
    for (auto &str: reader.StringIds()) {
        auto *str_ptr = reader.dataPtr<dex::u1>(str.string_data_off);
        ReadULeb128(&str_ptr);
        *strings_it++ = reinterpret_cast<const char *>(str_ptr);
    }

    auto type_names_it = type_names.begin();
    for (auto &type_id: reader.TypeIds()) {
        *type_names_it++ = strings[type_id.descriptor_idx];
    }

    auto proto_it = proto_type_list.begin();
    for (auto &proto: reader.ProtoIds()) {
        if (proto.parameters_off != 0) {
            auto *type_list_ptr = reader.dataPtr<dex::TypeList>(proto.parameters_off);
            *proto_it = type_list_ptr;
        }
        ++proto_it;
    }

    for (auto &class_def: reader.ClassDefs()) {
        if (class_def.class_data_off == 0) {
            continue;
        }
        const auto *class_data = reader.dataPtr<dex::u1>(class_def.class_data_off);
        dex::u4 static_fields_size = ReadULeb128(&class_data);
        dex::u4 instance_fields_count = ReadULeb128(&class_data);
        dex::u4 direct_methods_count = ReadULeb128(&class_data);
        dex::u4 virtual_methods_count = ReadULeb128(&class_data);

        auto &methods = class_method_ids[class_def.class_idx];
        methods.resize(direct_methods_count + virtual_methods_count);

        for (int i = 0; i < static_fields_size; ++i) {
            ReadULeb128(&class_data);
            ReadULeb128(&class_data);
        }

        for (int i = 0; i < instance_fields_count; ++i) {
            ReadULeb128(&class_data);
            ReadULeb128(&class_data);
        }

        for (dex::u4 i = 0, method_idx = 0; i < direct_methods_count; ++i) {
            method_idx += ReadULeb128(&class_data);
            ReadULeb128(&class_data);
            dex::u4 code_off = ReadULeb128(&class_data);
            if (code_off == 0) {
                continue;
            }
            method_codes[method_idx] = reader.dataPtr<const dex::Code>(code_off);
            methods.emplace_back(method_idx);
        }
        for (dex::u4 i = 0, method_idx = 0; i < virtual_methods_count; ++i) {
            method_idx += ReadULeb128(&class_data);
            ReadULeb128(&class_data);
            dex::u4 code_off = ReadULeb128(&class_data);
            if (code_off == 0) {
                continue;
            }
            method_codes[method_idx] = reader.dataPtr<const dex::Code>(code_off);
            methods.emplace_back(method_idx);
        }
    }
    init_flags_[dex_idx] = true;
}

std::tuple<std::string, std::string, std::vector<std::string>>
DexKit::ConvertDescriptors(std::string &return_decl, std::vector<std::string> &param_decls) {
    std::string return_type = DeclToMatchDescriptor(return_decl);
    std::vector<std::string> param_types;
    for (auto &param_decl: param_decls) {
        param_types.emplace_back(DeclToMatchDescriptor(param_decl));
    }
    std::string shorty = DescriptorToMatchShorty(return_type, param_types);
    return std::make_tuple(shorty, return_type, param_types);
}

bool DexKit::IsMethodMatch(size_t dex_idx, uint32_t method_idx, uint32_t decl_class,
                           const std::string &shorty_match, const std::string &method_name,
                           uint32_t return_type, const std::vector<uint32_t> &param_types,
                           bool match_any_param_if_param_vector_empty) {
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &method_id = reader.MethodIds()[method_idx];
    auto &proto_id = reader.ProtoIds()[method_id.proto_idx];
    auto &shorty = strings[proto_id.shorty_idx];
    if (!match_any_param_if_param_vector_empty && !shorty.empty() && !ShortyDescriptorMatch(shorty_match, shorty)) {
        return false;
    }
    if (decl_class != dex::kNoIndex && decl_class != method_id.class_idx) {
        return false;
    }
    if (return_type != dex::kNoIndex && return_type != proto_id.return_type_idx) {
        return false;
    }
    if (!method_name.empty() && method_name != strings[method_id.name_idx]) {
        return false;
    }
    auto &type_list = proto_type_list_[dex_idx][method_id.proto_idx];
    if (match_any_param_if_param_vector_empty) {
        return true;
    }
    if (param_types.size() != type_list->size) {
        return false;
    }
    auto len = type_list ? type_list->size : 0;
    for (int i = 0; i < len; ++i) {
        if (param_types[i] != dex::kNoIndex && type_list->list[i].type_idx != param_types[i]) {
            return false;
        }
    }
    return true;
}

std::string DexKit::GetMethodDescriptor(size_t dex_idx, uint32_t method_idx) {
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &type_names = type_names_[dex_idx];
    auto &method_id = reader.MethodIds()[method_idx];
    auto &proto_id = reader.ProtoIds()[method_id.proto_idx];
    auto &type_list = proto_type_list_[dex_idx][method_id.proto_idx];
    std::string descriptor(type_names[method_id.class_idx]);
    descriptor += "->";
    descriptor += strings[method_id.name_idx];
    descriptor += '(';
    auto len = type_list ? type_list->size : 0;
    for (int i = 0; i < len; ++i) {
        descriptor += strings[reader.TypeIds()[type_list->list[i].type_idx].descriptor_idx];
    }
    descriptor += ')';
    descriptor += strings[reader.TypeIds()[proto_id.return_type_idx].descriptor_idx];
    return descriptor;
}

uint32_t DexKit::FindTypeIdx(size_t dex_idx, std::string &type_desc) {
    auto &type_names = type_names_[dex_idx];
    for (int i = 0; i < type_names.size(); ++i) {
        if (type_desc == type_names[i]) {
            return i;
        }
    }
    return dex::kNoIndex;
}

std::vector<size_t> DexKit::GetDexPriority(const std::vector<size_t> &dex_priority) {
    std::vector<size_t> res;
    if (dex_priority.empty()) {
        for (int i = 0; i < readers_.size(); ++i) {
            res.emplace_back(i);
        }
    } else {
        for (auto &dex_idx: dex_priority) {
            res.emplace_back(dex_idx);
        }
    }
    return res;
}

}