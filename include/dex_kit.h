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
    explicit DexKit(std::string_view apk_path);

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
    std::map<std::string_view, std::vector<std::string_view>> LocationClasses(
            std::map<std::string_view, std::set<std::string_view>> &location_map);

    /**
     *
     * @param method_descriptor Like "Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V"
     * @return invoke methods descriptor list: <br/> eg.
     * ["Landroidx/activity/ComponentActivity;->onCreate(Landroid/os/Bundle;)V",]
     */
    std::vector<std::string> FindMethodInvoked(std::string_view method_descriptor);

    /**
     *
     * @param class_name Like "Landroid/app/Activity;" or "android.app.Activity"
     * @return sub class descriptor list. <br/>
     * eg. ["Landroid/app/ActivityGroup;", "Landroid/app/AliasActivity;"]
     */
    std::vector<std::string> FindSubClasses(std::string class_name);

private:
    std::mutex mutex_;
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

    void InitCached(int dex_idx);

    std::string GetMethodDescriptor(int dex_idx, uint32_t method_idx);

    static std::string GetClassDescriptor(std::string class_name);
};

}