#pragma once

#include <string_view>
#include <vector>
#include <thread>
#include <list>
#include <set>
#include <unordered_map>
#include "slicer/reader.h"
#include "file_helper.h"

namespace dexkit {

class DexKit {
public:
    explicit DexKit(std::string_view apk_path, int unzip_thread_num = -1);

    explicit DexKit(std::vector<std::pair<const void *, size_t>> &dex_images);

    ~DexKit() = default;

    void SetThreadNum(int num) {
        thread_num_ = num;
    }

    /**
     * Find classes using strings.
     * @param location_map map of classes -> [location strings].
     * eg. {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> ["TroopClockInHandler"]}
     * @return map of possible classes name.
     * eg. {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> ["Lxadt;"]}
     */
    std::map<std::string, std::vector<std::string>>
    LocationClasses(std::map<std::string, std::set<std::string>> &location_map,
                    bool advanced_match = false);

    /**
     *
     * @param method_descriptor Like "Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V"
     * @return invoke methods descriptor list: <br/> eg.
     * ["Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V",]
     */
    std::vector<std::string>
    FindMethodInvoked(std::string method_descriptor,
                      std::string class_decl_name,
                      std::string method_name,
                      std::string result_class_decl,
                      const std::vector<std::string> &param_class_decls,
                      const std::vector<size_t> &dex_priority,
                      bool match_any_param_if_param_vector_empty);


    std::vector<std::string>
    FindMethodUsedString(std::string str,
                         std::string class_decl_name,
                         std::string method_name,
                         std::string result_class_decl,
                         const std::vector<std::string> &param_class_decls,
                         const std::vector<size_t> &dex_priority,
                         bool match_any_param_if_param_vector_empty,
                         bool advanced_match = false);


    std::vector<std::string>
    FindMethod(std::string class_decl_name,
               std::string method_name,
               std::string result_class_decl,
               const std::vector<std::string> &param_class_decls,
               const std::vector<size_t> &dex_priority,
               bool match_any_param_if_param_vector_empty);

    /**
     *
     * @param class_name Like "Landroid/app/Activity;" or "android.app.Activity"
     * @return sub class descriptor list. <br/>
     * eg. ["Landroid/app/ActivityGroup;", "Landroid/app/AliasActivity;"]
     */
    std::vector<std::string>
    FindSubClasses(std::string class_name);

    /**
     *
     * @param op_prefix_seq [0x70, 0x22, 0x70, 0x5b] <br/>
     * ["invoke-direct", "new-instance", "invoke-direct", "iput-object"]
     * @return return the method descriptor has beginning of the sequence of OpCode. <br/>
     * eg. ["Landroid/arch/lifecycle/ClassesInfoCache;-><init>()V"]
     */
    std::vector<std::string>
    FindMethodOpPrefixSeq(std::vector<uint8_t> &op_prefix_seq,
                          std::string class_decl_name,
                          std::string method_name,
                          std::string result_class_decl,
                          const std::vector<std::string> &param_class_decls,
                          const std::vector<size_t> &dex_priority,
                          bool match_any_param_if_param_vector_empty);

    size_t GetDexNum() {
        return dex_images_.size();
    }

private:
    std::vector<bool> init_flags_;
    std::vector<MemMap> maps_;
    std::vector<std::pair<const void *, size_t>> dex_images_;

    uint32_t thread_num_ = std::thread::hardware_concurrency();
    std::vector<dex::Reader> readers_;

    std::vector<std::vector<std::string_view>> strings_;
    std::vector<std::vector<std::string_view>> type_names_;
    std::vector<std::vector<std::vector<std::uint32_t>>> class_method_ids_;
    std::vector<std::vector<const dex::Code *>> method_codes_;
    std::vector<std::vector<const dex::TypeList *>> proto_type_list_;

    std::vector<std::string> cache_;

    void InitImages();

    void InitCached(size_t dex_idx);

    std::tuple<std::string, std::string, std::vector<std::string>>
    ConvertDescriptors(std::string &return_decl, std::vector<std::string> &param_decls);

    uint32_t FindTypeIdx(size_t dex_idx, std::string &type_desc);

    bool IsMethodMatch(size_t dex_idx, uint32_t method_idx, uint32_t decl_class,
                       const std::string &shorty_match, const std::string &method_name,
                       uint32_t return_type, const std::vector<uint32_t> &param_types,
                       bool match_any_param_if_param_vector_empty);

    std::string GetMethodDescriptor(size_t dex_idx, uint32_t method_idx);

    static std::string GetClassDescriptor(std::string &class_name);

    std::vector<size_t> GetDexPriority(const std::vector<size_t> &dex_priority);
};

}