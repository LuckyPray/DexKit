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
DexKit::BatchFindClassesUsedStrings(std::map<std::string, std::set<std::string>> &location_map,
                                    bool advanced_match,
                                    const std::vector<size_t> &dex_priority) {
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
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue([this, dex_idx, &acdat, &location_map, &flag_map]() {
            InitCached(dex_idx, fDefault);
            const auto &method_codes = method_codes_[dex_idx];
            const auto &class_method_ids = class_method_ids_[dex_idx];
            const auto &type_names = type_names_[dex_idx];
            const auto &strings = strings_[dex_idx];

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
DexKit::BatchFindMethodsUsedStrings(std::map<std::string, std::set<std::string>> &location_map,
                                    bool advanced_match,
                                    const std::vector<size_t> &dex_priority) {
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
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue([this, dex_idx, &acdat, &location_map, &flag_map]() {
            InitCached(dex_idx, fDefault);
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
DexKit::FindMethodBeInvoked(const std::string &method_descriptor,
                            const std::string &method_declare_class,
                            const std::string &method_declare_name,
                            const std::string &method_return_type,
                            const std::optional<std::vector<std::string>> &method_param_types,
                            const std::string &caller_method_declare_class,
                            const std::string &caller_method_declare_name,
                            const std::string &caller_method_return_type,
                            const std::optional<std::vector<std::string>> &caller_method_param_types,
                            const std::vector<size_t> &dex_priority) {
    // be invoked method
    auto extract_tuple = ExtractMethodDescriptor(method_descriptor, method_declare_class, method_declare_name,
                                                 method_return_type, method_param_types);
    std::string class_desc = std::get<0>(extract_tuple);
    std::string method_name = std::get<1>(extract_tuple);
    std::string return_desc = std::get<2>(extract_tuple);
    std::vector<std::string> param_descs = std::get<3>(extract_tuple);
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);
    bool match_any_param = method_param_types == null_param;

    // caller method
    auto caller_extract_tuple = ExtractMethodDescriptor({}, caller_method_declare_class,
                                                        caller_method_declare_name, caller_method_return_type,
                                                        caller_method_param_types);
    std::string caller_class_desc = std::get<0>(caller_extract_tuple);
    std::string caller_method_name = std::get<1>(caller_extract_tuple);
    std::string caller_return_desc = std::get<2>(caller_extract_tuple);
    std::vector<std::string> caller_param_descs = std::get<3>(caller_extract_tuple);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_return_desc, caller_param_descs);
    bool caller_match_any_param = caller_method_param_types == null_param;
    bool need_caller_match = NeedMethodMatch({}, caller_method_declare_class, caller_method_name,
                                             caller_method_return_type, caller_method_param_types);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &method_name, &return_desc, &param_descs, &match_shorty, &match_any_param,
                        &caller_class_desc, &caller_method_name, &caller_return_desc, &caller_param_descs, &caller_match_shorty,
                        &caller_match_any_param, &need_caller_match]() {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // be invoked method
                    auto class_idx = dex::kNoIndex;
                    auto return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!class_desc.empty()) {
                        class_idx = FindTypeIdx(dex_idx, class_desc);
                        if (class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    if (!return_desc.empty()) {
                        return_type = FindTypeIdx(dex_idx, return_desc);
                        if (return_type == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: param_descs) {
                        auto type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        param_types.emplace_back(type);
                    }

                    // caller method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_class_desc.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_class_desc);
                        if (caller_class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_return_desc.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_return_desc);
                        if (caller_return_type == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: caller_param_descs) {
                        auto type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        caller_param_types.emplace_back(type);
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (need_caller_match) {
                                if (!IsMethodMatch(dex_idx, method_idx, caller_class_idx, caller_match_shorty,
                                                   caller_method_name, caller_return_type, caller_param_types,
                                                   caller_match_any_param)) {
                                    continue;
                                }
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
                                    case 0x6e: // invoke-virtual
                                    case 0x6f: // invoke-super
                                    case 0x70: // invoke-direct
                                    case 0x71: // invoke-static
                                    case 0x72: // invoke-interface
                                    case 0x74: // invoke-virtual/range
                                    case 0x75: // invoke-super/range
                                    case 0x76: // invoke-direct/range
                                    case 0x77: // invoke-static/range
                                    case 0x78: { // invoke-interface/range
                                        auto index = ReadShort(ptr);
                                        if (IsMethodMatch(dex_idx, index, class_idx, match_shorty, method_name,
                                                          return_type, param_types, match_any_param)) {
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

std::map<std::string, std::vector<std::string>>
DexKit::FindMethodInvoking(const std::string &method_descriptor,
                           const std::string &method_declare_class,
                           const std::string &method_declare_name,
                           const std::string &method_return_type,
                           const std::optional<std::vector<std::string>> &method_param_types,
                           const std::string &be_called_method_declare_class,
                           const std::string &be_called_method_declare_name,
                           const std::string &be_called_method_return_type,
                           const std::optional<std::vector<std::string>> &invoking_method_param_types,
                           const std::vector<size_t> &dex_priority) {

    // caller method
    auto extract_tuple = ExtractMethodDescriptor(method_descriptor, method_declare_class, method_declare_name,
                                                 method_return_type, method_param_types);
    std::string caller_class_desc = std::get<0>(extract_tuple);
    std::string caller_method_name = std::get<1>(extract_tuple);
    std::string caller_return_desc = std::get<2>(extract_tuple);
    std::vector<std::string> caller_param_descs = std::get<3>(extract_tuple);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_return_desc, caller_param_descs);
    bool caller_match_any_param = method_param_types == null_param;

    // be called method
    auto be_called_extract_tuple = ExtractMethodDescriptor({}, be_called_method_declare_class,
                                                           be_called_method_declare_name,
                                                           be_called_method_return_type,
                                                           invoking_method_param_types);
    std::string be_called_class_desc = std::get<0>(be_called_extract_tuple);
    std::string be_called_method_name = std::get<1>(be_called_extract_tuple);
    std::string be_called_return_desc = std::get<2>(be_called_extract_tuple);
    std::vector<std::string> be_called_param_descs = std::get<3>(be_called_extract_tuple);
    std::string be_called_match_shorty = DescriptorToMatchShorty(be_called_return_desc, be_called_param_descs);
    bool be_called_match_any_param = invoking_method_param_types == null_param;

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &caller_class_desc, &caller_method_name, &caller_return_desc, &caller_param_descs, &caller_match_shorty, &caller_match_any_param,
                        &be_called_class_desc, &be_called_method_name, &be_called_return_desc, &be_called_param_descs,
                        &be_called_match_shorty, &be_called_match_any_param]() {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // caller method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_class_desc.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_class_desc);
                        if (caller_class_idx == dex::kNoIndex) {
                            return std::map<std::string, std::vector<std::string>>();
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_return_desc.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_return_desc);
                        if (caller_return_type == dex::kNoIndex) {
                            return std::map<std::string, std::vector<std::string>>();
                        }
                    }
                    for (auto &v: caller_param_descs) {
                        auto type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::map<std::string, std::vector<std::string>>();
                            }
                        }
                        caller_param_types.emplace_back(type);
                    }

                    // be called method
                    auto be_called_class_idx = dex::kNoIndex;
                    auto be_called_return_type = dex::kNoIndex;
                    std::vector<uint32_t> be_called_param_types;
                    if (!be_called_class_desc.empty()) {
                        be_called_class_idx = FindTypeIdx(dex_idx, be_called_class_desc);
                        if (be_called_class_idx == dex::kNoIndex) {
                            return std::map<std::string, std::vector<std::string>>();
                        }
                    }
                    if (!be_called_return_desc.empty()) {
                        be_called_return_type = FindTypeIdx(dex_idx, be_called_return_desc);
                        if (be_called_return_type == dex::kNoIndex) {
                            return std::map<std::string, std::vector<std::string>>();
                        }
                    }
                    for (auto &v: be_called_param_descs) {
                        auto type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::map<std::string, std::vector<std::string>>();
                            }
                        }
                        be_called_param_types.emplace_back(type);
                    }

                    std::map<dex::u2, std::set<dex::u2>> index_map;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx, method_idx, caller_class_idx, caller_match_shorty,
                                               caller_method_name, caller_return_type, caller_param_types,
                                               caller_match_any_param)) {
                                continue;
                            }
                            auto caller_descriptor = GetMethodDescriptor(dex_idx, method_idx);
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
                                    case 0x6e: // invoke-virtual
                                    case 0x6f: // invoke-super
                                    case 0x70: // invoke-direct
                                    case 0x71: // invoke-static
                                    case 0x72: // invoke-interface
                                    case 0x74: // invoke-virtual/range
                                    case 0x75: // invoke-super/range
                                    case 0x76: // invoke-direct/range
                                    case 0x77: // invoke-static/range
                                    case 0x78: { // invoke-interface/range
                                        auto index = ReadShort(ptr);
                                        if (IsMethodMatch(dex_idx, index, be_called_class_idx, be_called_match_shorty,
                                                          be_called_method_name,
                                                          be_called_return_type, be_called_param_types,
                                                          be_called_match_any_param)) {
                                            index_map[method_idx].emplace(index);
                                        }
                                        break;
                                    }
                                    default:
                                        break;
                                }
                                p += width;
                            }
                        }
                    }
                    std::map<std::string, std::vector<std::string>> result;
                    for (auto &[key, value]: index_map) {
                        auto caller_descriptor = GetMethodDescriptor(dex_idx, key);
                        std::vector<std::string> be_called_descriptors;
                        for (auto v: value) {
                            auto be_called_descriptor = GetMethodDescriptor(dex_idx, v);
                            be_called_descriptors.emplace_back(be_called_descriptor);
                        }
                        result.emplace(caller_descriptor, be_called_descriptors);
                    }
                    return result;
                }
        ));
    }
    std::map<std::string, std::vector<std::string>> result;
    for (auto &f: futures) {
        auto r = f.get();
        result.insert(r.begin(), r.end());
    }
    return result;
}

std::vector<std::string>
DexKit::FindFieldBeUsed(const std::string &field_descriptor,
                        const std::string &field_declare_class,
                        const std::string &field_declare_name,
                        const std::string &field_type,
                        uint32_t be_used_flags,
                        const std::string &caller_method_declare_class,
                        const std::string &caller_method_declare_name,
                        const std::string &caller_method_return_type,
                        const std::optional<std::vector<std::string>> &caller_method_param_types,
                        const std::vector<size_t> &dex_priority) {
    // be getter field
    auto extract_tuple = ExtractFieldDescriptor(field_descriptor, field_declare_class, field_declare_name, field_type);
    std::string field_declare_class_desc = std::get<0>(extract_tuple);
    std::string field_name = std::get<1>(extract_tuple);
    std::string field_type_desc = std::get<2>(extract_tuple);
    if (be_used_flags == 0) be_used_flags = fGetting | fSetting;

    // caller method
    auto caller_extract_tuple = ExtractMethodDescriptor({}, caller_method_declare_class,
                                                        caller_method_declare_name, caller_method_return_type,
                                                        caller_method_param_types);
    std::string caller_class_desc = std::get<0>(caller_extract_tuple);
    std::string caller_method_name = std::get<1>(caller_extract_tuple);
    std::string caller_return_desc = std::get<2>(caller_extract_tuple);
    std::vector<std::string> caller_param_descs = std::get<3>(caller_extract_tuple);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_return_desc, caller_param_descs);
    bool caller_match_any_param = caller_method_param_types == null_param;
    bool need_caller_match = NeedMethodMatch({}, caller_method_declare_class, caller_method_declare_name,
                                             caller_method_return_type, caller_method_param_types);

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &field_declare_class_desc, &field_name, &field_type_desc, &be_used_flags,
                        &caller_class_desc, &caller_method_name, &caller_return_desc, &caller_param_descs, &caller_match_shorty,
                        &caller_match_any_param, &need_caller_match]() {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // be getter field
                    auto declared_class_idx = dex::kNoIndex;
                    auto field_type_idx = dex::kNoIndex;
                    if (!field_declare_class_desc.empty()) {
                        declared_class_idx = FindTypeIdx(dex_idx, field_declare_class_desc);
                        if (declared_class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    if (!field_type_desc.empty()) {
                        field_type_idx = FindTypeIdx(dex_idx, field_type_desc);
                        if (field_type_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }

                    // caller method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_class_desc.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_class_desc);
                        if (caller_class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_return_desc.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_return_desc);
                        if (caller_return_type == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: caller_param_descs) {
                        auto type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        caller_param_types.emplace_back(type);
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (need_caller_match) {
                                if (!IsMethodMatch(dex_idx, method_idx, caller_class_idx, caller_match_shorty,
                                                   caller_method_name, caller_return_type, caller_param_types,
                                                   caller_match_any_param)) {
                                    continue;
                                }
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
                                // iget, iget-wide, iget-object, iget-boolean, iget-byte, iget-char, iget-short
                                // sget, sget-wide, sget-object, sget-boolean, sget-byte, sget-char, sget-short
                                auto is_getter = ((op >= 0x52 && op <= 0x58) || (op >= 0x60 && op <= 0x66));
                                // iput, iput-wide, iput-object, iput-boolean, iput-byte, iput-char, iput-short
                                // sput, sput-wide, sput-object, sput-boolean, sput-byte, sput-char, sput-short
                                auto is_setter = ((op >= 0x59 && op <= 0x5f) || (op >= 0x67 && op <= 0x6d));
                                if ((is_getter && (be_used_flags & fGetting)) ||
                                    (is_setter && (be_used_flags & fSetting))) {
                                    auto index = ReadShort(ptr);
                                    if (IsFieldMatch(dex_idx, index, declared_class_idx, field_name,
                                                     field_type_idx)) {
                                        auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                        result.emplace_back(descriptor);
                                        goto label;
                                    }
                                }
                                p += width;
                            }
                            label:;
                        }
                    }
                    return result;
                }
        ));
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
DexKit::FindMethodUsedString(const std::string &used_utf8_string,
                             bool advanced_match,
                             const std::string &method_declare_class,
                             const std::string &method_declare_name,
                             const std::string &method_return_type,
                             const std::optional<std::vector<std::string>> &method_param_types,
                             const std::vector<size_t> &dex_priority) {
    // caller method
    auto extract_tuple = ExtractMethodDescriptor({}, method_declare_class, method_declare_name,
                                                 method_return_type, method_param_types);
    std::string caller_class_desc = std::get<0>(extract_tuple);
    std::string caller_method_name = std::get<1>(extract_tuple);
    std::string caller_return_desc = std::get<2>(extract_tuple);
    std::vector<std::string> caller_param_descs = std::get<3>(extract_tuple);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_return_desc, caller_param_descs);
    bool caller_match_any_param = method_param_types == null_param;

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &used_utf8_string, &advanced_match,
                        &caller_class_desc, &caller_method_name, &caller_return_desc, &caller_param_descs, &caller_match_shorty, caller_match_any_param]() {
                    InitCached(dex_idx, fDefault);
                    auto &strings = strings_[dex_idx];
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    uint8_t flag = 0;
                    if (advanced_match) {
                        if (used_utf8_string[0] == '^') {
                            flag |= 1;
                        }
                        if (used_utf8_string[used_utf8_string.size() - 1] == '$') {
                            flag |= 2;
                        }
                    }
                    auto real_str = used_utf8_string.substr(flag & 1,
                                                            used_utf8_string.size() - ((flag >> 1) + (flag & 1)));
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

                    auto caller_class_idx = dex::kNoIndex;
                    uint32_t caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_class_desc.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_class_desc);
                        if (caller_class_idx == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    if (caller_class_idx != dex::kNoIndex) {
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_return_desc.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_return_desc);
                        if (caller_return_type == dex::kNoIndex) {
                            return std::vector<std::string>();
                        }
                    }
                    for (auto &v: caller_param_descs) {
                        uint32_t type = dex::kNoIndex;
                        if (!v.empty()) {
                            type = FindTypeIdx(dex_idx, v);
                            if (type == dex::kNoIndex) {
                                return std::vector<std::string>();
                            }
                        }
                        caller_param_types.emplace_back(type);
                    }
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx, method_idx, caller_class_idx, caller_match_shorty,
                                               caller_method_name,
                                               caller_return_type, caller_param_types, caller_match_any_param)) {
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
                                    case 0x1a:
                                    case 0x1b: {
                                        dex::u4 index;
                                        if (op == 0x1a) {
                                            index = ReadShort(ptr);
                                        } else {
                                            index = ReadInt(ptr);
                                        }
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
DexKit::FindMethod(const std::string &method_declare_class,
                   const std::string &method_declare_name,
                   const std::string &method_return_type,
                   const std::optional<std::vector<std::string>> &method_param_types,
                   const std::vector<size_t> &dex_priority) {

    auto extract_tuple = ExtractMethodDescriptor({}, method_declare_class, method_declare_name,
                                                 method_return_type, method_param_types);
    std::string class_desc = std::get<0>(extract_tuple);
    std::string method_name = std::get<1>(extract_tuple);
    std::string return_desc = std::get<2>(extract_tuple);
    std::vector<std::string> param_descs = std::get<3>(extract_tuple);
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);
    bool match_any_param = method_param_types == null_param;

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param]() {
                    InitCached(dex_idx, fDefault);
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
                        if (return_type == dex::kNoIndex) {
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
                                              return_type, param_types, match_any_param)) {
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
DexKit::FindSubClasses(const std::string &parent_class,
                       const std::vector<size_t> &dex_priority) {
    auto class_descriptor = GetClassDescriptor(parent_class);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &class_descriptor]() {
                    InitCached(dex_idx, fDefault);
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
                              const std::string &method_declare_class,
                              const std::string &method_declare_name,
                              const std::string &method_return_type,
                              const std::optional<std::vector<std::string>> &method_param_types,
                              const std::vector<size_t> &dex_priority) {

    auto extract_tuple = ExtractMethodDescriptor({}, method_declare_class, method_declare_name,
                                                 method_return_type, method_param_types);
    std::string class_desc = std::get<0>(extract_tuple);
    std::string method_name = std::get<1>(extract_tuple);
    std::string return_desc = std::get<2>(extract_tuple);
    std::vector<std::string> param_descs = std::get<3>(extract_tuple);
    std::string match_shorty = DescriptorToMatchShorty(return_desc, param_descs);
    bool match_any_param = method_param_types == null_param;

    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &op_prefix_seq, &class_desc, &return_desc, &param_descs, &match_shorty, &method_name, &match_any_param]() {
                    InitCached(dex_idx, fDefault);
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
                        if (return_type == dex::kNoIndex) {
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
                                               return_type, param_types, match_any_param)) {
                                continue;
                            }
                            auto &code = method_codes[method_idx];
                            if (code == nullptr) {
                                continue;
                            }
                            auto p = code->insns;
                            auto end_p = p + code->insns_size;
                            uint32_t op_index = 0, prefix_seq_size = op_prefix_seq.size();
                            while (p < end_p && op_index < prefix_seq_size) {
                                auto op = *p & 0xff;
                                auto ptr = p;
                                auto width = GetBytecodeWidth(ptr++);
                                if (op_prefix_seq[op_index++] != op) {
                                    break;
                                }
                                if (op_index == prefix_seq_size) {
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
    strings_.resize(dex_images_.size());
    type_names_.resize(dex_images_.size());
    class_method_ids_.resize(dex_images_.size());
    class_field_ids_.resize(dex_images_.size());
    method_codes_.resize(dex_images_.size());
    proto_type_list_.resize(dex_images_.size());
    method_op_code_seq_.resize(dex_images_.size());

    init_flags_.resize(dex_images_.size(), fHeader);
}

void DexKit::InitCached(size_t dex_idx, dex::u4 flag) {
    auto &dex_flag = init_flags_[dex_idx];
    if (((dex_flag & flag) ^ flag) == 0) return;

    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &type_names = type_names_[dex_idx];
    auto &class_method_ids = class_method_ids_[dex_idx];
    auto &class_field_ids = class_field_ids_[dex_idx];
    auto &method_codes = method_codes_[dex_idx];
    auto &proto_type_list = proto_type_list_[dex_idx];
    auto &method_op_code_seq = method_op_code_seq_[dex_idx];

    if (dex_flag ^ (dex_flag & fString)) {
        strings.resize(reader.StringIds().size());
        auto strings_it = strings.begin();
        for (auto &str: reader.StringIds()) {
            auto *str_ptr = reader.dataPtr<dex::u1>(str.string_data_off);
            ReadULeb128(&str_ptr);
            *strings_it++ = reinterpret_cast<const char *>(str_ptr);
        }
        dex_flag |= fString;
    }

    if (dex_flag ^ (dex_flag & fType)) {
        type_names.resize(reader.TypeIds().size());
        auto type_names_it = type_names.begin();
        for (auto &type_id: reader.TypeIds()) {
            *type_names_it++ = strings[type_id.descriptor_idx];
        }
        dex_flag |= fType;
    }

    if (dex_flag ^ (dex_flag & fProto)) {
        proto_type_list.resize(reader.ProtoIds().size());
        auto proto_it = proto_type_list.begin();
        for (auto &proto: reader.ProtoIds()) {
            if (proto.parameters_off != 0) {
                auto *type_list_ptr = reader.dataPtr<dex::TypeList>(proto.parameters_off);
                *proto_it = type_list_ptr;
            }
            ++proto_it;
        }
        dex_flag |= fProto;
    }

    if (dex_flag ^ (dex_flag & fField)) {
        class_field_ids.resize(reader.TypeIds().size());
        int field_idx = 0;
        for (auto &field: reader.FieldIds()) {
            class_field_ids[field.class_idx].emplace_back(field_idx);
            ++field_idx;
        }
        dex_flag |= fField;
    }

    if (dex_flag ^ (dex_flag & fMethod)) {
        class_method_ids.resize(reader.TypeIds().size());
        method_codes.resize(reader.MethodIds().size(), nullptr);
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
        dex_flag |= fMethod;
    }

    if (dex_flag ^ (dex_flag & fOpCodeSeq)) {
        // TODO: init op code seq
        dex_flag |= fOpCodeSeq;
    }
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
                           bool match_any_param) {
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &method_id = reader.MethodIds()[method_idx];
    auto &proto_id = reader.ProtoIds()[method_id.proto_idx];
    auto &shorty = strings[proto_id.shorty_idx];
    if (!match_any_param && !shorty.empty() && !ShortyDescriptorMatch(shorty_match, shorty)) {
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
    if (match_any_param) {
        return true;
    }
    auto type_list_size = type_list ? type_list->size : 0;
    if (param_types.size() != type_list_size) {
        return false;
    }
    for (int i = 0; i < type_list_size; ++i) {
        if (param_types[i] != dex::kNoIndex && type_list->list[i].type_idx != param_types[i]) {
            return false;
        }
    }
    return true;
}

bool DexKit::IsFieldMatch(size_t dex_idx, uint32_t field_idx, uint32_t decl_class,
                          const std::string &field_name, uint32_t field_type) {
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &field_id = reader.FieldIds()[field_idx];
    if (decl_class != dex::kNoIndex && decl_class != field_id.class_idx) {
        return false;
    }
    if (!field_name.empty() && field_name != strings[field_id.name_idx]) {
        return false;
    }
    if (field_type != dex::kNoIndex && field_type != field_id.type_idx) {
        return false;
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

std::string DexKit::GetFieldDescriptor(size_t dex_idx, uint32_t field_idx) {
    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &type_names = type_names_[dex_idx];
    auto &field_id = reader.FieldIds()[field_idx];
    std::string descriptor(type_names[field_id.class_idx]);
    descriptor += "->";
    descriptor += strings[field_id.name_idx];
    descriptor += ':';
    descriptor += strings[reader.TypeIds()[field_id.type_idx].descriptor_idx];
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

inline bool DexKit::NeedMethodMatch(const std::string &method_descriptor,
                                    const std::string &caller_method_declare_class,
                                    const std::string &caller_method_declare_name,
                                    const std::string &caller_method_return_type,
                                    const std::optional<std::vector<std::string>> &caller_method_param_types) {
    return !method_descriptor.empty() || !caller_method_declare_class.empty() ||
           !caller_method_declare_name.empty() || !caller_method_return_type.empty() ||
           caller_method_param_types != std::nullopt;
}

}  // namespace dexkit