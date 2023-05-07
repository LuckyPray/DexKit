#include "dex_kit.h"
#include "file_helper.h"
#include "acdat/Builder.h"
#include "thread_pool.h"
#include "byte_code_util.h"
#include "opcode_util.h"
#include "code_format.h"
#include "kmp.h"
#include "annotation_util.h"

namespace dexkit {

const dex::Code emptyCode{};

static std::string GetPackagePath(const std::string &find_package);

using namespace acdat;

DexKit::DexKit(std::string_view apk_path, int unzip_thread_num) {
    AddPath(apk_path, unzip_thread_num);
}

void DexKit::AddImages(std::vector<std::unique_ptr<MemMap>> dex_images) {
    int ort_size = (int) dex_images_.size();
    int new_size = (int) (ort_size + dex_images.size());
    dex_images_.resize(new_size);
    for (int i = ort_size; i < new_size; ++i) {
        auto &image = *dex_images[i - ort_size];
        dex_images_[i] = std::make_pair(image.addr(), image.len());
        maps_.emplace_back(std::move(dex_images[i - ort_size]));
    }
    InitImages(ort_size, new_size);
}

void DexKit::AddPath(std::string_view apk_path, int unzip_thread_num) {
    auto map = MemMap(apk_path);
    if (!map.ok()) {
        return;
    }
    auto zip_file = ZipFile::Open(map);
    if (!zip_file) return;
    std::vector<std::pair<int, ZipLocalFile *>> dexs;
    for (int idx = 1;; ++idx) {
        auto entry_name = "classes" + (idx == 1 ? std::string() : std::to_string(idx)) + ".dex";
        auto entry = zip_file->Find(entry_name);
        if (!entry) {
            break;
        }
        dexs.emplace_back(idx, entry);
    }
    int ort_size = (int) dex_images_.size();
    int new_size = (int) (ort_size + dexs.size());
    dex_images_.resize(new_size);
    int maps_org_size = (int) maps_.size();
    maps_.resize(maps_.size() + dexs.size());
    ThreadPool pool(unzip_thread_num == -1 ? thread_num_ : unzip_thread_num);
    std::vector<std::future<void>> futures;
    for (auto &dex_pair: dexs) {
        auto v = pool.enqueue([this, &dex_pair, ort_size, maps_org_size]() {
            auto dex_image = dex_pair.second->uncompress();
            if (!dex_image.ok()) {
                return;
            }
            int idx = dex_pair.first - 1;
            dex_images_[ort_size + idx] = std::make_pair(dex_image.addr(), dex_image.len());
            maps_[maps_org_size + idx] = std::make_unique<MemMap>(std::move(dex_image));
        });
        futures.emplace_back(std::move(v));
    }
    for (auto &f: futures) {
        f.get();
    }
    maps_.emplace_back(std::make_unique<MemMap>(std::move(map)));
    InitImages(ort_size, new_size);
}

void DexKit::ExportDexFile(std::string &out_dir) {
    for (auto &[image, size]: dex_images_) {
        auto file_name = out_dir;
        if (file_name.back() != '/') {
            file_name += '/';
        }
        file_name += "classes_" + std::to_string(size) + ".dex";
        // write file
        FILE *fp = fopen(file_name.c_str(), "wb");
        if (fp == nullptr) {
            continue;
        }
        fwrite(image, 1, size, fp);
        fclose(fp);
    }
}

std::map<std::string, std::vector<std::string>>
DexKit::BatchFindClassesUsingStrings(std::map<std::string, std::set<std::string>> &location_map,
                                     int match_type,
                                     const std::string &find_package,
                                     const std::vector<size_t> &dex_priority) {
    auto acdat = AhoCorasickDoubleArrayTrie<std::string>();
    std::map<std::string, std::string> buildMap;
    std::map<std::string, uint8_t> flag_map;
    for (auto &[name, str_set]: location_map) {
        for (auto &str: str_set) {
            std::string new_str;
            if (match_type == mFull) {
                match_type = mSimilarRegex;
                new_str = "^" + str + "$";
            } else {
                new_str = str;
            }
            uint32_t l = 0, r = new_str.size();
            uint8_t flag = 0;
            if (match_type == mSimilarRegex) {
                if (new_str[0] == '^') {
                    l = 1;
                    flag |= 1;
                }
                if (new_str[new_str.size() - 1] == '$') {
                    r = new_str.size() - 1;
                    flag |= 2;
                }
            }
            auto origin_str = new_str.substr(l, r - l);
            flag_map[origin_str] = flag;
            buildMap[origin_str] = origin_str;
        }
    }
    std::string package_path = GetPackagePath(find_package);
    Builder<std::string>().build(buildMap, &acdat);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(
                pool.enqueue([this, dex_idx, &acdat, &location_map, &flag_map, &package_path]()
                                     -> std::map<std::string, std::vector<std::string>> {
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
                                [&string_map, index, &flag_map, &string](int begin, int end,
                                                                         const std::string &value) -> void {
                                    auto flag = flag_map[value];
                                    if ((flag & 1 && begin != 0) ||
                                        (flag >> 1 && end != string.size())) {
                                        return;
                                    }
                                    string_map[index] = ((flag & 1) ? "^" : "") + value +
                                                        ((flag >> 1) ? "$" : "");
                                };
                        acdat.parseText(string.data(), callback);
                    }

                    if (string_map.empty()) {
                        return {};
                    }
                    for (int i = 0; i < type_names.size(); ++i) {
                        auto class_name = type_names[i];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
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
                            std::set_intersection(search_set.begin(), search_set.end(),
                                                  value_set.begin(),
                                                  value_set.end(), std::inserter(vec, vec.begin()));
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
DexKit::BatchFindMethodsUsingStrings(std::map<std::string, std::set<std::string>> &location_map,
                                     int match_type,
                                     const std::string &find_package,
                                     const std::vector<size_t> &dex_priority) {
    auto acdat = AhoCorasickDoubleArrayTrie<std::string>();
    std::map<std::string, std::string> buildMap;
    std::map<std::string, uint8_t> flag_map;
    for (auto &[name, str_set]: location_map) {
        for (auto &str: str_set) {
            std::string new_str;
            if (match_type == mFull) {
                match_type = mSimilarRegex;
                new_str = "^" + str + "$";
            } else {
                new_str = str;
            }
            uint32_t l = 0, r = new_str.size();
            uint8_t flag = 0;
            if (match_type == mSimilarRegex) {
                if (new_str[0] == '^') {
                    l = 1;
                    flag |= 1;
                }
                if (new_str[new_str.size() - 1] == '$') {
                    r = new_str.size() - 1;
                    flag |= 2;
                }
            }
            auto origin_str = new_str.substr(l, r - l);
            flag_map[origin_str] = flag;
            buildMap[origin_str] = origin_str;
        }
    }
    auto package_path = GetPackagePath(find_package);
    Builder<std::string>().build(buildMap, &acdat);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(
                pool.enqueue([this, dex_idx, &acdat, &location_map, &flag_map, &package_path]()
                                     -> std::map<std::string, std::vector<std::string>> {
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
                                [&string_map, index, &flag_map, &string](int begin, int end,
                                                                         const std::string &value) -> void {
                                    auto flag = flag_map[value];
                                    if ((flag & 1 && begin != 0) ||
                                        (flag >> 1 && end != string.size())) {
                                        return;
                                    }
                                    string_map[index] = ((flag & 1) ? "^" : "") + value +
                                                        ((flag >> 1) ? "$" : "");
                                };
                        acdat.parseText(string.data(), callback);
                    }

                    if (string_map.empty()) {
                        return {};
                    }
                    for (int i = 0; i < type_names.size(); ++i) {
                        auto class_name = type_names[i];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
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
                                std::set_intersection(search_set.begin(), search_set.end(),
                                                      value_set.begin(),
                                                      value_set.end(),
                                                      std::inserter(vec, vec.begin()));
                                if (vec.size() == value_set.size()) {
                                    result[real_method].emplace_back(
                                            GetMethodDescriptor(dex_idx, method_idx));
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

std::map<std::string, std::vector<std::string>>
DexKit::FindMethodCaller(const std::string &method_descriptor,
                         const std::string &method_declare_class,
                         const std::string &method_declare_name,
                         const std::string &method_return_type,
                         const std::optional<std::vector<std::string>> &method_param_types,
                         const std::string &caller_method_descriptor,
                         const std::string &caller_method_declare_class,
                         const std::string &caller_method_declare_name,
                         const std::string &caller_method_return_type,
                         const std::optional<std::vector<std::string>> &caller_method_param_types,
                         bool unique_result,
                         const std::string &source_file,
                         const std::string &find_package,
                         const std::vector<size_t> &dex_priority) {
    // be invoked method
    auto method = ExtractMethodDescriptor(
            method_descriptor,
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string match_shorty = DescriptorToMatchShorty(method);
    bool match_any_param = !method.parameter_types.has_value();

    // caller method
    auto caller_method = ExtractMethodDescriptor(
            caller_method_descriptor,
            caller_method_declare_class,
            caller_method_declare_name,
            caller_method_return_type,
            caller_method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_method);
    bool caller_match_any_param = !caller_method.parameter_types.has_value();
    bool need_caller_match = NeedMethodMatch(caller_method);

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &method, &match_shorty, &match_any_param,
                        &caller_method, &caller_match_shorty, &caller_match_any_param,
                        &need_caller_match, unique_result, &package_path, &source_file]()
                        -> std::map<std::string, std::vector<std::string>> {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // be invoked method
                    auto class_idx = dex::kNoIndex;
                    auto return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!method.declaring_class.empty()) {
                        class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (!method.return_type.empty()) {
                        return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (return_type == dex::kNoIndex) {
                            std::map<std::string, std::vector<std::string>>();
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            auto type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            param_types.emplace_back(type);
                        }
                    }

                    // caller method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_method.parameter_types.has_value()) {
                        for (auto &v: caller_method.parameter_types.value()) {
                            auto type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    std::map<dex::u2, std::vector<dex::u2>> index_map;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (need_caller_match) {
                                if (!IsMethodMatch(dex_idx,
                                                   method_idx,
                                                   caller_class_idx,
                                                   caller_match_shorty,
                                                   caller_method.name,
                                                   caller_return_type,
                                                   caller_param_types,
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
                                        if (IsMethodMatch(dex_idx,
                                                          index,
                                                          class_idx,
                                                          match_shorty,
                                                          method.name,
                                                          return_type,
                                                          param_types,
                                                          match_any_param)) {
                                            auto descriptor = GetMethodDescriptor(dex_idx,
                                                                                  method_idx);
                                            index_map[method_idx].emplace_back(index);
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
                        if (unique_result) {
                            std::set<dex::u2> set(value.begin(), value.end());
                            value.assign(set.begin(), set.end());
                        }
                        auto caller_descriptor = GetMethodDescriptor(dex_idx, key);
                        std::vector<std::string> be_called_descriptors;
                        for (auto v: value) {
                            auto be_called_descriptor = GetMethodDescriptor(dex_idx, v);
                            be_called_descriptors.emplace_back(be_called_descriptor);
                        }
                        result.emplace(caller_descriptor, be_called_descriptors);
                    }
                    return result;
                })
        );
    }
    std::map<std::string, std::vector<std::string>> result;
    for (auto &f: futures) {
        auto r = f.get();
        result.insert(r.begin(), r.end());
    }
    return result;
}

std::map<std::string, std::vector<std::string>>
DexKit::FindMethodInvoking(const std::string &method_descriptor,
                           const std::string &method_declare_class,
                           const std::string &method_declare_name,
                           const std::string &method_return_type,
                           const std::optional<std::vector<std::string>> &method_param_types,
                           const std::string &be_called_method_descriptor,
                           const std::string &be_called_method_declare_class,
                           const std::string &be_called_method_declare_name,
                           const std::string &be_called_method_return_type,
                           const std::optional<std::vector<std::string>> &invoking_method_param_types,
                           bool unique_result,
                           const std::string &source_file,
                           const std::string &find_package,
                           const std::vector<size_t> &dex_priority) {

    // caller caller_method
    auto caller_method = ExtractMethodDescriptor(
            method_descriptor,
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_method);
    bool caller_match_any_param = !caller_method.parameter_types.has_value();

    // be called caller_method
    auto be_called_method = ExtractMethodDescriptor(
            be_called_method_descriptor,
            be_called_method_declare_class,
            be_called_method_declare_name,
            be_called_method_return_type,
            invoking_method_param_types);
    std::string be_called_match_shorty = DescriptorToMatchShorty(be_called_method);
    bool be_called_match_any_param = !be_called_method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &caller_method, &caller_match_shorty, &caller_match_any_param,
                        &be_called_method, &be_called_match_shorty, &be_called_match_any_param,
                        unique_result, &package_path, &source_file]()
                        -> std::map<std::string, std::vector<std::string>> {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // caller caller_method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_method.parameter_types.has_value()) {
                        for (auto &v: caller_method.parameter_types.value()) {
                            auto type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    // be called caller_method
                    auto be_called_class_idx = dex::kNoIndex;
                    auto be_called_return_type = dex::kNoIndex;
                    std::vector<uint32_t> be_called_param_types;
                    if (!be_called_method.declaring_class.empty()) {
                        be_called_class_idx = FindTypeIdx(dex_idx,
                                                          be_called_method.declaring_class);
                        if (be_called_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (!be_called_method.return_type.empty()) {
                        be_called_return_type = FindTypeIdx(dex_idx, be_called_method.return_type);
                        if (be_called_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (be_called_method.parameter_types.has_value()) {
                        for (auto &v: be_called_method.parameter_types.value()) {
                            auto type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            be_called_param_types.emplace_back(type);
                        }
                    }

                    std::map<dex::u2, std::vector<dex::u2>> index_map;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               caller_class_idx,
                                               caller_match_shorty,
                                               caller_method.name,
                                               caller_return_type,
                                               caller_param_types,
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
                                        if (IsMethodMatch(dex_idx,
                                                          index,
                                                          be_called_class_idx,
                                                          be_called_match_shorty,
                                                          be_called_method.name,
                                                          be_called_return_type,
                                                          be_called_param_types,
                                                          be_called_match_any_param)) {
                                            index_map[method_idx].emplace_back(index);
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
                        if (unique_result) {
                            std::set<dex::u2> set(value.begin(), value.end());
                            value.assign(set.begin(), set.end());
                        }
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

std::map<std::string, std::vector<std::string>>
DexKit::FindMethodUsingField(const std::string &field_descriptor,
                             const std::string &field_declare_class,
                             const std::string &field_declare_name,
                             const std::string &field_type,
                             uint32_t used_flags,
                             const std::string &caller_method_descriptor,
                             const std::string &caller_method_declare_class,
                             const std::string &caller_method_declare_name,
                             const std::string &caller_method_return_type,
                             const std::optional<std::vector<std::string>> &caller_method_param_types,
                             bool unique_result,
                             const std::string &source_file,
                             const std::string &find_package,
                             const std::vector<size_t> &dex_priority) {
    // be getter field
    auto field = ExtractFieldDescriptor(
            field_descriptor,
            field_declare_class,
            field_declare_name,
            field_type);
    if (used_flags == 0) used_flags = fGetting | fSetting;

    // caller method
    auto caller_method = ExtractMethodDescriptor(
            caller_method_descriptor,
            caller_method_declare_class,
            caller_method_declare_name,
            caller_method_return_type,
            caller_method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_method);
    bool caller_match_any_param = !caller_method.parameter_types.has_value();
    bool need_caller_match = NeedMethodMatch(caller_method);

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<std::string>>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &field, &used_flags,
                        &caller_method, &caller_match_shorty, &caller_match_any_param,
                        &need_caller_match, unique_result, &package_path, &source_file]()
                        -> std::map<std::string, std::vector<std::string>> {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    // be getter field
                    auto declared_class_idx = dex::kNoIndex;
                    auto field_type_idx = dex::kNoIndex;
                    if (!field.declaring_class.empty()) {
                        declared_class_idx = FindTypeIdx(dex_idx, field.declaring_class);
                        if (declared_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (!field.type.empty()) {
                        field_type_idx = FindTypeIdx(dex_idx, field.type);
                        if (field_type_idx == dex::kNoIndex) {
                            return {};
                        }
                    }

                    // caller method
                    auto caller_class_idx = dex::kNoIndex;
                    auto caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_method.parameter_types.has_value()) {
                        for (auto &v: caller_method.parameter_types.value()) {
                            auto type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    std::map<dex::u2, std::vector<dex::u2>> index_map;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (need_caller_match) {
                                if (!IsMethodMatch(dex_idx,
                                                   method_idx,
                                                   caller_class_idx,
                                                   caller_match_shorty,
                                                   caller_method.name,
                                                   caller_return_type,
                                                   caller_param_types,
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
                                auto is_getter = ((op >= 0x52 && op <= 0x58) ||
                                                  (op >= 0x60 && op <= 0x66));
                                // iput, iput-wide, iput-object, iput-boolean, iput-byte, iput-char, iput-short
                                // sput, sput-wide, sput-object, sput-boolean, sput-byte, sput-char, sput-short
                                auto is_setter = ((op >= 0x59 && op <= 0x5f) ||
                                                  (op >= 0x67 && op <= 0x6d));
                                if ((is_getter && (used_flags & fGetting)) ||
                                    (is_setter && (used_flags & fSetting))) {
                                    auto index = ReadShort(ptr);
                                    if (IsFieldMatch(dex_idx,
                                                     index,
                                                     declared_class_idx,
                                                     field.name,
                                                     field_type_idx)) {
                                        index_map[method_idx].emplace_back(index);
                                    }
                                }
                                p += width;
                            }
                        }
                    }
                    std::map<std::string, std::vector<std::string>> result;
                    for (auto &[key, value]: index_map) {
                        if (unique_result) {
                            std::set<dex::u2> set(value.begin(), value.end());
                            value.assign(set.begin(), set.end());
                        }
                        auto caller_descriptor = GetMethodDescriptor(dex_idx, key);
                        std::vector<std::string> be_called_descriptors;
                        for (auto v: value) {
                            auto using_field_descriptor = GetFieldDescriptor(dex_idx, v);
                            be_called_descriptors.emplace_back(using_field_descriptor);
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
DexKit::FindMethodUsingString(const std::string &using_utf8_string,
                              int match_type,
                              const std::string &method_declare_class,
                              const std::string &method_declare_name,
                              const std::string &method_return_type,
                              const std::optional<std::vector<std::string>> &method_param_types,
                              bool unique_result,
                              const std::string &source_file,
                              const std::string &find_package,
                              const std::vector<size_t> &dex_priority) {
    // caller method
    auto caller_method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_method);
    bool caller_match_any_param = !caller_method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &using_utf8_string, &match_type,
                        &caller_method, &caller_match_shorty, caller_match_any_param,
                        unique_result, &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault);
                    auto &strings = strings_[dex_idx];
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    uint8_t flag = 0;
                    if (match_type == mSimilarRegex) {
                        if (using_utf8_string[0] == '^') {
                            flag |= 1;
                        }
                        if (using_utf8_string[using_utf8_string.size() - 1] == '$') {
                            flag |= 2;
                        }
                    }
                    std::string_view real_str = using_utf8_string;
                    if (flag) {
                        real_str = real_str.substr(1, real_str.size() - 2);
                    }
                    std::set<uint32_t> matched_strings;
                    for (int str_idx = 0; str_idx < strings.size(); ++str_idx) {
                        auto &string = strings[str_idx];
                        auto find_idx = kmp::FindIndex(string, real_str);
                        if (match_type == mFull) {
                            if (find_idx == 0 && string.size() == real_str.size()) {
                                matched_strings.emplace(str_idx);
                            }
                            continue;
                        }
                        if (find_idx == -1 ||
                            (flag & 1 && find_idx != 0) ||
                            (flag & 2 && find_idx != string.size() - real_str.size())) {
                            continue;
                        }
                        matched_strings.emplace(str_idx);
                    }
                    if (matched_strings.empty()) {
                        return {};
                    }

                    auto caller_class_idx = dex::kNoIndex;
                    uint32_t caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_class_idx != dex::kNoIndex) {
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_method.parameter_types.has_value()) {
                        for (auto &v: caller_method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               caller_class_idx,
                                               caller_match_shorty,
                                               caller_method.name,
                                               caller_return_type, caller_param_types,
                                               caller_match_any_param)) {
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
                                            auto descriptor = GetMethodDescriptor(dex_idx,
                                                                                  method_idx);
                                            result.emplace_back(descriptor);
                                            if (unique_result) {
                                                goto label;
                                            }
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
DexKit::FindMethodUsingNumber(int64_t using_number,
                              const std::string &method_declare_class,
                              const std::string &method_declare_name,
                              const std::string &method_return_type,
                              const std::optional<std::vector<std::string>> &method_param_types,
                              bool unique_result,
                              const std::string &source_file,
                              const std::string &find_package) {

    // caller method
    auto caller_method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(caller_method);
    bool caller_match_any_param = !caller_method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority({})) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &using_number,
                        &caller_method, &caller_match_shorty, caller_match_any_param,
                        unique_result, &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    auto caller_class_idx = dex::kNoIndex;
                    uint32_t caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!caller_method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, caller_method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_class_idx != dex::kNoIndex) {
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!caller_method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, caller_method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_method.parameter_types.has_value()) {
                        for (auto &v: caller_method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               caller_class_idx,
                                               caller_match_shorty,
                                               caller_method.name,
                                               caller_return_type, caller_param_types,
                                               caller_match_any_param)) {
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
                                if ((op >= 0x12 && op <= 0x14)
                                || (op >= 0x16 && op <= 0x18)
                                || (op >= 0xd0 && op <= 0xd7) // int16
                                || (op >= 0xe0 && op <= 0xe2)) { // int8
                                    bool match_flag = false;
                                    switch (op) {
                                        case 0x12: { // const/4, int4
                                            auto value = (int8_t) *ptr >> 4;
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0x13:   // const/16, int16
                                        case 0x16: { // const-wide/16, int16
                                            auto value = (int16_t) ReadShort(ptr);
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0x15: { // const/high16
                                            auto value = (int32_t) ReadShort(ptr) << 16;
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0x14:   // const, int32|uint32|float
                                        case 0x17: { // const-wide/32, int32
                                            auto value = (int32_t) ReadInt(ptr);
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0x18: { // const-wide, int64|uint64|double
                                            auto value = (int64_t) ReadLong(ptr);
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0x19: { // const-wide/high16
                                            auto value = (int64_t) ReadShort(ptr) << 48;
                                            break;
                                        }
                                        case 0xd0:
                                        case 0xd1:
                                        case 0xd2:
                                        case 0xd3:
                                        case 0xd4:
                                        case 0xd5:
                                        case 0xd6:
                                        case 0xd7: { // binop/lit16
                                            auto value = (int16_t) ReadShort(ptr);
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        case 0xd8:
                                        case 0xd9:
                                        case 0xda:
                                        case 0xdb:
                                        case 0xdc:
                                        case 0xdd:
                                        case 0xde:
                                        case 0xdf:
                                        case 0xe0:
                                        case 0xe1:
                                        case 0xe2: { // binop/lit8
                                            auto value = (int8_t) *ptr;
                                            if (value == using_number) {
                                                match_flag = true;
                                            }
                                            break;
                                        }
                                        default:
                                            break;
                                    }
                                    if (match_flag) {
                                        result.emplace_back(GetMethodDescriptor(dex_idx, method_idx));
                                        if (unique_result) {
                                            goto label;
                                        }
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
DexKit::FindClassUsingAnnotation(const std::string &annotation_class,
                                 const std::string &annotation_using_string,
                                 int match_type,
                                 const std::string &source_file,
                                 const std::string &find_package,
                                 const std::vector<size_t> &dex_priority) {
    std::string annotation_class_desc = GetClassDescriptor(annotation_class);

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;

    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &annotation_class_desc, &match_type, &annotation_using_string, &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault | fAnnotation);
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto &class_annotations = class_annotations_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    auto annotation_class_idx = dex::kNoIndex;
                    if (!annotation_class_desc.empty()) {
                        annotation_class_idx = FindTypeIdx(dex_idx, annotation_class_desc);
                        if (annotation_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        auto &annotations = class_annotations[c_idx];
                        if (annotations == nullptr || annotations->class_annotation == nullptr) {
                            continue;
                        }
                        bool find = FindAnnotationSetUsingString(annotations->class_annotation,
                                                                 annotation_class_idx,
                                                                 annotation_using_string,
                                                                 match_type);
                        if (find) {
                            auto descriptor = type_names[c_idx];
                            result.emplace_back(descriptor);
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
DexKit::FindFieldUsingAnnotation(const std::string &annotation_class,
                                 const std::string &annotation_using_string,
                                 int match_type,
                                 const std::string &field_declare_class,
                                 const std::string &field_declare_name,
                                 const std::string &field_type,
                                 const std::string &source_file,
                                 const std::string &find_package,
                                 const std::vector<size_t> &dex_priority) {
    std::string annotation_class_desc = GetClassDescriptor(annotation_class);

    // be getter field
    auto field = ExtractFieldDescriptor(
            {},
            field_declare_class,
            field_declare_name,
            field_type);

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;

    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &annotation_class_desc, &annotation_using_string, &match_type,
                        &field, &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault | fAnnotation);
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto &class_annotations = class_annotations_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    auto annotation_class_idx = dex::kNoIndex;
                    if (!annotation_class_desc.empty()) {
                        annotation_class_idx = FindTypeIdx(dex_idx, annotation_class_desc);
                        if (annotation_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }

                    auto declared_class_idx = dex::kNoIndex;
                    auto field_type_idx = dex::kNoIndex;
                    if (!field.declaring_class.empty()) {
                        declared_class_idx = FindTypeIdx(dex_idx, field.declaring_class);
                        if (declared_class_idx == dex::kNoIndex) {
                            return {};
                        }
                        lower = declared_class_idx;
                        upper = declared_class_idx + 1;
                    }
                    if (!field.type.empty()) {
                        field_type_idx = FindTypeIdx(dex_idx, field.type);
                        if (field_type_idx == dex::kNoIndex) {
                            return {};
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        auto &annotations = class_annotations[c_idx];
                        if (annotations == nullptr) {
                            continue;
                        }
                        for (auto &field_annotation: annotations->field_annotations) {
                            auto field_idx = field_annotation->field_decl->orig_index;
                            if (!IsFieldMatch(dex_idx,
                                              field_idx,
                                              declared_class_idx,
                                              field.name,
                                              field_type_idx)) {
                                continue;
                            }
                            bool find = FindAnnotationSetUsingString(field_annotation->annotations,
                                                                     annotation_class_idx,
                                                                     annotation_using_string,
                                                                     match_type);
                            if (find) {
                                auto descriptor = GetFieldDescriptor(dex_idx, field_idx);
                                result.emplace_back(descriptor);
                            }
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
DexKit::FindMethodUsingAnnotation(const std::string &annotation_class,
                                  const std::string &annotation_using_string,
                                  int match_type,
                                  const std::string &method_declare_class,
                                  const std::string &method_declare_name,
                                  const std::string &method_return_type,
                                  const std::optional<std::vector<std::string>> &method_param_types,
                                  const std::string &source_file,
                                  const std::string &find_package,
                                  const std::vector<size_t> &dex_priority) {
    std::string annotation_class_desc = GetClassDescriptor(annotation_class);
    // using annotation's method
    auto method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string caller_match_shorty = DescriptorToMatchShorty(method);
    bool caller_match_any_param = !method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &annotation_class_desc, &annotation_using_string, &match_type,
                        &method, &caller_match_shorty, &caller_match_any_param,
                        &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault | fAnnotation);
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto &class_annotations = class_annotations_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();

                    auto annotation_class_idx = dex::kNoIndex;
                    if (!annotation_class_desc.empty()) {
                        annotation_class_idx = FindTypeIdx(dex_idx, annotation_class_desc);
                        if (annotation_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }

                    auto caller_class_idx = dex::kNoIndex;
                    uint32_t caller_return_type = dex::kNoIndex;
                    std::vector<uint32_t> caller_param_types;
                    if (!method.declaring_class.empty()) {
                        caller_class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (caller_class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (caller_class_idx != dex::kNoIndex) {
                        lower = caller_class_idx;
                        upper = caller_class_idx + 1;
                    }
                    if (!method.return_type.empty()) {
                        caller_return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (caller_return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            caller_param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        auto &annotations = class_annotations[c_idx];
                        if (annotations == nullptr) {
                            continue;
                        }
                        for (auto &method_annotation: annotations->method_annotations) {
                            auto method_idx = method_annotation->method_decl->orig_index;
                            if (!IsMethodMatch(dex_idx, method_idx, caller_class_idx,
                                               caller_match_shorty,
                                               method.name,
                                               caller_return_type, caller_param_types,
                                               caller_match_any_param)) {
                                continue;
                            }
                            bool find = FindAnnotationSetUsingString(method_annotation->annotations,
                                                                     annotation_class_idx,
                                                                     annotation_using_string,
                                                                     match_type);
                            if (find) {
                                auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                result.emplace_back(descriptor);
                            }
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
DexKit::FindMethod(const std::string &method_descriptor,
                   const std::string &method_declare_class,
                   const std::string &method_declare_name,
                   const std::string &method_return_type,
                   const std::optional<std::vector<std::string>> &method_param_types,
                   const std::string &source_file,
                   const std::string &find_package,
                   const std::vector<size_t> &dex_priority) {

    auto method = ExtractMethodDescriptor(
            method_descriptor,
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string match_shorty = DescriptorToMatchShorty(method);
    bool match_any_param = !method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &method, &match_shorty, &match_any_param,
                        &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault);
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto class_idx = dex::kNoIndex;
                    if (!method.declaring_class.empty()) {
                        class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!method.return_type.empty()) {
                        return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (IsMethodMatch(dex_idx,
                                              method_idx,
                                              class_idx,
                                              match_shorty,
                                              method.name,
                                              return_type,
                                              param_types,
                                              match_any_param)) {
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
DexKit::FindClass(const std::string &source_file,
                  const std::string &find_package,
                  const std::vector<size_t> &dex_priority) {
    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (auto &dex_idx: GetDexPriority(dex_priority)) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &source_file, &package_path]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault);
                    auto &type_names = type_names_[dex_idx];
                    auto &type_declared_flag = type_declared_flag_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    uint32_t lower = 0, upper = type_names.size();
                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        // Skip this dex does not own the type of class_def
                        if (!type_declared_flag[c_idx]) {
                            continue;
                        }
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        result.emplace_back(type_names[c_idx]);
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
                [this, dex_idx, &class_descriptor]()
                        -> std::vector<std::string> {
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
                        return {};
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
DexKit::FindMethodUsingOpPrefixSeq(const std::vector<uint8_t> &op_prefix_seq,
                                   const std::string &method_declare_class,
                                   const std::string &method_declare_name,
                                   const std::string &method_return_type,
                                   const std::optional<std::vector<std::string>> &method_param_types,
                                   const std::string &source_file,
                                   const std::string &find_package,
                                   const std::vector<size_t> &dex_priority) {
    auto method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string match_shorty = DescriptorToMatchShorty(method);
    bool match_any_param = !method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &op_prefix_seq, &method, &match_shorty, &match_any_param,
                        &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault);
                    auto &method_codes = method_codes_[dex_idx];
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];

                    auto class_idx = dex::kNoIndex;
                    if (!method.declaring_class.empty()) {
                        class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!method.return_type.empty()) {
                        return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               class_idx,
                                               match_shorty,
                                               method.name,
                                               return_type,
                                               param_types,
                                               match_any_param)) {
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

std::vector<std::string>
DexKit::FindMethodUsingOpCodeSeq(const std::vector<uint8_t> &op_seq,
                                 const std::string &method_declare_class,
                                 const std::string &method_declare_name,
                                 const std::string &method_return_type,
                                 const std::optional<std::vector<std::string>> &method_param_types,
                                 const std::string &source_file,
                                 const std::string &find_package,
                                 const std::vector<size_t> &dex_priority) {
    auto method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string match_shorty = DescriptorToMatchShorty(method);
    bool match_any_param = !method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::vector<std::string>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &op_seq, &method, &match_shorty, &match_any_param,
                        &package_path, &source_file]()
                        -> std::vector<std::string> {
                    InitCached(dex_idx, fDefault | fOpCodeSeq);
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto &method_opcode_seq = method_opcode_seq_[dex_idx];

                    auto class_idx = dex::kNoIndex;
                    if (!method.declaring_class.empty()) {
                        class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!method.return_type.empty()) {
                        return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            param_types.emplace_back(type);
                        }
                    }

                    std::vector<std::string> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               class_idx,
                                               match_shorty,
                                               method.name,
                                               return_type, param_types, match_any_param)) {
                                continue;
                            }
                            if (!IsInitMethodOpCodeSeq(dex_idx, method_idx)) {
                                InitMethodOpCodeSeq(dex_idx, method_idx);
                            }
                            auto &opcode_seq = method_opcode_seq[method_idx];
                            if (opcode_seq.size() < op_seq.size()) {
                                continue;
                            }
                            int ret = kmp::FindIndex(opcode_seq, op_seq);
                            if (ret != -1) {
                                auto descriptor = GetMethodDescriptor(dex_idx, method_idx);
                                result.push_back(descriptor);
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


std::map<std::string, std::vector<uint8_t>>
DexKit::GetMethodOpCodeSeq(const std::string &method_descriptor,
                           const std::string &method_declare_class,
                           const std::string &method_declare_name,
                           const std::string &method_return_type,
                           const std::optional<std::vector<std::string>> &method_param_types,
                           const std::string &source_file,
                           const std::string &find_package,
                           const std::vector<size_t> &dex_priority) {

    auto method = ExtractMethodDescriptor(
            {},
            method_declare_class,
            method_declare_name,
            method_return_type,
            method_param_types);
    std::string match_shorty = DescriptorToMatchShorty(method);
    bool match_any_param = !method.parameter_types.has_value();

    auto package_path = GetPackagePath(find_package);
    ThreadPool pool(thread_num_);
    std::vector<std::future<std::map<std::string, std::vector<uint8_t>>>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        futures.emplace_back(pool.enqueue(
                [this, dex_idx, &method, &match_shorty, &match_any_param,
                        &package_path, &source_file]()
                        -> std::map<std::string, std::vector<uint8_t>> {
                    InitCached(dex_idx, fDefault | fOpCodeSeq);
                    auto &class_method_ids = class_method_ids_[dex_idx];
                    auto &type_names = type_names_[dex_idx];
                    auto &class_source_files = class_source_files_[dex_idx];
                    auto &method_opcode_seq = method_opcode_seq_[dex_idx];

                    auto class_idx = dex::kNoIndex;
                    if (!method.declaring_class.empty()) {
                        class_idx = FindTypeIdx(dex_idx, method.declaring_class);
                        if (class_idx == dex::kNoIndex) {
                            return {};
                        }
                    }
                    uint32_t lower = 0, upper = type_names.size();
                    if (class_idx != dex::kNoIndex) {
                        lower = class_idx;
                        upper = class_idx + 1;
                    }
                    uint32_t return_type = dex::kNoIndex;
                    std::vector<uint32_t> param_types;
                    if (!method.return_type.empty()) {
                        return_type = FindTypeIdx(dex_idx, method.return_type);
                        if (return_type == dex::kNoIndex) {
                            return {};
                        }
                    }
                    if (method.parameter_types.has_value()) {
                        for (auto &v: method.parameter_types.value()) {
                            uint32_t type = dex::kNoIndex;
                            if (!v.empty()) {
                                type = FindTypeIdx(dex_idx, v);
                                if (type == dex::kNoIndex) {
                                    return {};
                                }
                            }
                            param_types.emplace_back(type);
                        }
                    }

                    std::map<std::string, std::vector<uint8_t>> result;
                    for (auto c_idx = lower; c_idx < upper; ++c_idx) {
                        if (!source_file.empty() && class_source_files[c_idx] != source_file) {
                            continue;
                        }
                        auto &class_name = type_names[c_idx];
                        if (!package_path.empty() && class_name.rfind(package_path, 0) != 0) {
                            continue;
                        }
                        for (auto method_idx: class_method_ids[c_idx]) {
                            if (!IsMethodMatch(dex_idx,
                                               method_idx,
                                               class_idx,
                                               match_shorty,
                                               method.name,
                                               return_type,
                                               param_types,
                                               match_any_param)) {
                                continue;
                            }
                            if (!IsInitMethodOpCodeSeq(dex_idx, method_idx)) {
                                InitMethodOpCodeSeq(dex_idx, method_idx);
                            }
                            auto method_desc = GetMethodDescriptor(dex_idx, method_idx);
                            auto &opcode_seq = method_opcode_seq[method_idx];
                            result[method_desc] = opcode_seq;
                        }
                    }
                    return result;
                })
        );
    }
    std::map<std::string, std::vector<uint8_t>> result;
    for (auto &f: futures) {
        auto r = f.get();
        result.insert(r.begin(), r.end());
    }
    return result;
}

uint32_t
DexKit::GetClassAccessFlags(const std::string &class_descriptor) {
    auto class_desc = GetClassDescriptor(class_descriptor);

    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        InitCached(dex_idx, fDefault);
        auto &class_access_flags = class_access_flags_[dex_idx];

        auto class_idx = FindTypeIdx(dex_idx, class_desc);
        if (class_idx == dex::kNoIndex) {
            continue;
        }
        return class_access_flags[class_idx];
    }
    return -1;
}

uint32_t
DexKit::GetMethodAccessFlags(const std::string &method_descriptor) {
    auto method = ExtractMethodDescriptor(
            method_descriptor,
            "",
            "",
            "",
            null_param);

    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        InitCached(dex_idx, fDefault);
        auto &class_method_ids = class_method_ids_[dex_idx];
        auto &method_access_flags = method_access_flags_[dex_idx];

        auto class_idx = FindTypeIdx(dex_idx, method.declaring_class);
        if (class_idx == dex::kNoIndex) {
            continue;
        }
        for (auto method_idx: class_method_ids[class_idx]) {
            auto desc = GetMethodDescriptor(dex_idx, method_idx);
            if (desc == method_descriptor) {
                return method_access_flags[method_idx];
            }
        }
    }
    return -1;
}

uint32_t
DexKit::GetFieldAccessFlags(const std::string &field_descriptor) {
    auto field = ExtractFieldDescriptor(
            field_descriptor,
            "",
            "",
            "");

    ThreadPool pool(thread_num_);
    std::vector<std::future<uint32_t>> futures;
    for (int dex_idx = 0; dex_idx < readers_.size(); ++dex_idx) {
        InitCached(dex_idx, fDefault | fField);
        auto &class_field_ids = class_field_ids_[dex_idx];
        auto &field_access_flags = field_access_flags_[dex_idx];

        auto class_idx = FindTypeIdx(dex_idx, field.declaring_class);
        if (class_idx == dex::kNoIndex) {
            continue;
        }
        for (auto field_idx: class_field_ids[class_idx]) {
            auto desc = GetFieldDescriptor(dex_idx, field_idx);
            if (desc == field_descriptor) {
                return field_access_flags[field_idx];
            }
        }
    }
    return -1;
}


void DexKit::InitImages(int begin, int end) {
    for (int i = begin; i < end; ++i) {
        auto &[image, size] = dex_images_[i];
        auto reader = dex::Reader((const dex::u1 *) image, size);
        readers_.emplace_back(std::move(reader));
    }
    strings_.resize(dex_images_.size());
    type_names_.resize(dex_images_.size());
    type_ids_map_.resize(dex_images_.size());
    type_declared_flag_.resize(dex_images_.size());
    class_source_files_.resize(dex_images_.size());
    class_access_flags_.resize(dex_images_.size());
    class_method_ids_.resize(dex_images_.size());
    method_access_flags_.resize(dex_images_.size());
    class_field_ids_.resize(dex_images_.size());
    field_access_flags_.resize(dex_images_.size());
    method_codes_.resize(dex_images_.size());
    proto_type_list_.resize(dex_images_.size());
    method_opcode_seq_.resize(dex_images_.size());
    method_opcode_seq_init_flag_.resize(dex_images_.size());
    class_annotations_.resize(dex_images_.size());

    init_flags_.resize(dex_images_.size(), fHeader);
}

void DexKit::InitCached(size_t dex_idx, dex::u4 flag) {
    auto &dex_flag = init_flags_[dex_idx];
    if (((dex_flag & flag) ^ flag) == 0) return;

    bool need_init_strings = (flag & fString) && ((dex_flag & fString) == 0);
    bool need_init_type_names = (flag & fType) && ((dex_flag & fType) == 0);
    bool need_init_proto_type_list = (flag & fProto) && ((dex_flag & fProto) == 0);
    bool need_init_class_field_ids = (flag & fField) && ((dex_flag & fField) == 0);
    bool need_init_class_method_ids = (flag & fMethod) && ((dex_flag & fMethod) == 0);
    bool need_init_method_op_code_seq = (flag & fOpCodeSeq) && ((dex_flag & fOpCodeSeq) == 0);
    bool need_init_annotations = (flag & fAnnotation) && ((dex_flag & fAnnotation) == 0);

    auto &reader = readers_[dex_idx];
    auto &strings = strings_[dex_idx];
    auto &type_names = type_names_[dex_idx];
    auto &type_ids_map = type_ids_map_[dex_idx];
    auto &type_declared_flag = type_declared_flag_[dex_idx];
    auto &class_source_files = class_source_files_[dex_idx];
    auto &class_access_flags = class_access_flags_[dex_idx];
    auto &class_method_ids = class_method_ids_[dex_idx];
    auto &method_access_flags = method_access_flags_[dex_idx];
    auto &class_field_ids = class_field_ids_[dex_idx];
    auto &field_access_flags = field_access_flags_[dex_idx];
    auto &method_codes = method_codes_[dex_idx];
    auto &proto_type_list = proto_type_list_[dex_idx];
    auto &method_opcode_seq = method_opcode_seq_[dex_idx];
    auto &method_opcode_seq_init_flag = method_opcode_seq_init_flag_[dex_idx];
    auto &class_annotations = class_annotations_[dex_idx];

    if (need_init_strings) {
        strings.resize(reader.StringIds().size());
        auto strings_it = strings.begin();
        for (auto &str: reader.StringIds()) {
            auto *str_ptr = reader.dataPtr<dex::u1>(str.string_data_off);
            ReadULeb128(&str_ptr);
            *strings_it++ = reinterpret_cast<const char *>(str_ptr);
        }
        dex_flag |= fString;
    }

    if (need_init_type_names) {
        type_names.resize(reader.TypeIds().size());
        auto type_names_it = type_names.begin();
        int idx = 0;
        for (auto &type_id: reader.TypeIds()) {
            *type_names_it = strings[type_id.descriptor_idx];
            type_ids_map[*type_names_it++] = idx++;
        }
        dex_flag |= fType;
    }

    if (need_init_proto_type_list) {
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

    if (need_init_class_field_ids) {
        class_field_ids.resize(reader.TypeIds().size());
        int field_idx = 0;
        for (auto &field: reader.FieldIds()) {
            class_field_ids[field.class_idx].emplace_back(field_idx);
            ++field_idx;
        }
        dex_flag |= fField;
    }

    if (need_init_class_method_ids
        || (need_init_annotations && (dex_flag & fMethod) == 0)
        || (need_init_method_op_code_seq && (dex_flag & fMethod) == 0)) {

        type_declared_flag.resize(reader.TypeIds().size(), false);
        class_source_files.resize(reader.TypeIds().size());
        class_access_flags.resize(reader.TypeIds().size());
        class_method_ids.resize(reader.TypeIds().size());
        method_access_flags.resize(reader.MethodIds().size());
        method_codes.resize(reader.MethodIds().size(), nullptr);
        field_access_flags.resize(reader.FieldIds().size());

        method_opcode_seq.resize(reader.MethodIds().size());
        method_opcode_seq_init_flag.resize(reader.MethodIds().size(), false);

        for (auto &class_def: reader.ClassDefs()) {
            if (class_def.source_file_idx != dex::kNoIndex) {
                class_source_files[class_def.class_idx] = strings[class_def.source_file_idx];
            }
            type_declared_flag[class_def.class_idx] = true;
            if (class_def.class_data_off == 0) {
                continue;
            }
            const auto *class_data = reader.dataPtr<dex::u1>(class_def.class_data_off);
            dex::u4 static_fields_size = ReadULeb128(&class_data);
            dex::u4 instance_fields_count = ReadULeb128(&class_data);
            dex::u4 direct_methods_count = ReadULeb128(&class_data);
            dex::u4 virtual_methods_count = ReadULeb128(&class_data);

            auto &methods = class_method_ids[class_def.class_idx];

            for (dex::u4 i = 0, field_idx = 0; i < static_fields_size; ++i) {
                field_idx += ReadULeb128(&class_data);
                field_access_flags[field_idx] = ReadULeb128(&class_data);
            }

            for (dex::u4 i = 0, field_idx = 0; i < instance_fields_count; ++i) {
                field_idx += ReadULeb128(&class_data);
                field_access_flags[field_idx] = ReadULeb128(&class_data);
            }

            for (dex::u4 i = 0, method_idx = 0; i < direct_methods_count; ++i) {
                method_idx += ReadULeb128(&class_data);
                method_access_flags[method_idx] = ReadULeb128(&class_data);
                dex::u4 code_off = ReadULeb128(&class_data);
                if (code_off == 0) {
                    method_codes[method_idx] = &emptyCode;
                } else {
                    method_codes[method_idx] = reader.dataPtr<const dex::Code>(code_off);
                }
                methods.emplace_back(method_idx);
            }
            for (dex::u4 i = 0, method_idx = 0; i < virtual_methods_count; ++i) {
                method_idx += ReadULeb128(&class_data);
                method_access_flags[method_idx] = ReadULeb128(&class_data);
                dex::u4 code_off = ReadULeb128(&class_data);
                if (code_off == 0) {
                    method_codes[method_idx] = &emptyCode;
                } else {
                    method_codes[method_idx] = reader.dataPtr<const dex::Code>(code_off);
                }
                methods.emplace_back(method_idx);
            }
        }
        dex_flag |= fMethod;
    }

    if (need_init_annotations) {
        class_annotations.resize(reader.TypeIds().size(), nullptr);

        for (auto &class_def: reader.ClassDefs()) {
            if (class_def.annotations_off == 0) {
                continue;
            }
            auto annotations = reader.ExtractAnnotations(class_def.annotations_off);
            class_annotations[class_def.class_idx] = annotations;
        }
        dex_flag |= fAnnotation;
    }

    if (need_init_method_op_code_seq) {
        // lazy init in find opcode method
        dex_flag |= fOpCodeSeq;
    }
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
    if (decl_class != dex::kNoIndex && decl_class != method_id.class_idx) {
        return false;
    }
    if (return_type != dex::kNoIndex && return_type != proto_id.return_type_idx) {
        return false;
    }
    if (!match_any_param && !shorty.empty() && !ShortyDescriptorMatch(shorty_match, shorty)) {
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
    auto &type_ids_map = type_ids_map_[dex_idx];
    if (type_ids_map.find(type_desc) != type_ids_map.end()) {
        return type_ids_map[type_desc];
    }
    return dex::kNoIndex;
}

void DexKit::InitMethodOpCodeSeq(size_t dex_idx, uint32_t method_idx) {
    auto &code = method_codes_[dex_idx][method_idx];
    method_opcode_seq_init_flag_[dex_idx][method_idx] = true;
    if (code) {
        auto p = code->insns;
        auto end_p = p + code->insns_size;
        auto &opcode_seq = method_opcode_seq_[dex_idx][method_idx];

        while (p < end_p) {
            uint8_t op = *p & 0xff;
            opcode_seq.emplace_back(op);
            p += GetBytecodeWidth(p);
        }
    }
}

bool DexKit::IsInitMethodOpCodeSeq(size_t dex_idx, uint32_t method_idx) {
    return method_opcode_seq_init_flag_[dex_idx][method_idx];
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

inline bool DexKit::NeedMethodMatch(const MethodDescriptor &method_descriptor) {
    return !method_descriptor.declaring_class.empty() ||
           !method_descriptor.name.empty() ||
           !method_descriptor.return_type.empty() ||
           method_descriptor.parameter_types != std::nullopt;
}

static std::string GetPackagePath(const std::string &find_package) {
    if (find_package.empty() || find_package == "/") {
        return {};
    }
    auto path = find_package;
    if (find_package[0] == '/') {
        path = "L" + path.substr(1);
    } else {
        path = "L" + path;
    }
    if (path.back() != '/') {
        path += '/';
    }
    return path;
}

}  // namespace dexkit
