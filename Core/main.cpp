#include <iostream>
#include <fstream>
#include <chrono>
#include <stack>
#include <map>
#include <set>
#include "dex_kit.h"
#include "code_format.h"

int main() {
    std::map<std::string, std::set<std::string>> obfuscate = {
            {"Lcom/tencent/mobileqq/activity/ChatActivityFacade;",               {"^reSendEmo"}},
            {"Lcooperation/qzone/PlatformInfor;",                                {"52b7f2", "qimei"}},
            {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;", {"TroopClockInHandler"}},
            {"com.tencent.widget.CustomWidgetUtil",                              {"^NEW$"}},
    };

    dexkit::DexKit dexKit("../dex/qq-8.9.2.apk");

    // default threadNum used std::thread::hardware_concurrency()
    // dexKit.SetThreadNum(1);

    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());

    // find classes that use all strings in all methods of this class
    // if `advanced_match = true` you can use '^' and '$' to restrict string matching,
    // which is consistent with regular expression semantics.
    // result ex.
    // {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> {"Lxadt;"}}
    auto classes = dexKit.BatchFindClassesUsingStrings(obfuscate, true);
    std::cout << "\nBatchFindClassesUsedStrings -> \n";
    for (auto &[key, value]: classes) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }

    // Find methods that use all strings in all method of dex.
    // if `advanced_match = true` you can use '^' and '$' to restrict string matching,
    // which is consistent with regular expression semantics.
    // result ex.
    // {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;" -> {"Lxadt;->a()V"}}
    auto methods = dexKit.BatchFindMethodsUsingStrings(obfuscate, true);
    std::cout << "\nBatchFindMethodsUsedStrings -> \n";
    for (auto &[key, value]: classes) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }


    // Find caller for specified method.
    // after specifying method_descriptor, the subsequent parameters are automatically filled in,
    // If the method_descriptor is not specified, fuzzy matching can be performed by parameters, and an empty string is used to represent fuzzy matching.
    // result ex.
    // {"Lcom/qzone/album/ui/widget/AlbumDialog;->n(I)V"}
    auto beInvokedMethod = dexKit.FindMethodBeInvoked(
            "",
            "com.tencent.qphone.base.remote.ToServiceMsg",
            "<init>",
            "",
            dexkit::null_param,
            "Lcom/tencent/mobileqq/msf/sdk/MsfServiceSdk;",
            "getRegQueryAccountMsg",
            "",
            dexkit::null_param);
    std::cout << "\nFindMethodBeInvoked -> \n";
    for (auto &value: beInvokedMethod) {
        std::cout << "\t" << value << "\n";
    }

    auto invokingMethods = dexKit.FindMethodInvoking(
            "",
            "Lcom/tencent/mobileqq/msf/sdk/MsfServiceSdk;",
            "syncGetServerConfig",
            "",
            dexkit::null_param,
            "",
            "",
            "",
            dexkit::null_param);
    std::cout << "\nFindMethodInvoking -> \n";
    for (auto &[key, value]: invokingMethods) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }

    auto usingFieldMethods = dexKit.FindMethodUsingField(
            "",
            "",
            "",
            "Landroid/widget/TextView;",
            dexkit::fGetting,
            "Lcom/tencent/mobileqq/activity/aio/item/TextItemBuilder;",
            "",
            "void",
            std::vector<std::string>{"", "Lcom/tencent/mobileqq/data/ChatMessage;"});
    std::cout << "\nFindFieldBeUsed -> \n";
    for (auto &value: usingFieldMethods) {
        std::cout << "\t" << value << "\n";
    }

    // find all used matching string's method
    // if `advanced_match = true` you can use '^' and '$' to restrict string matching,
    // result ex.
    // {"Lcom/tencent/aekit/openrender/internal/Frame$Type;-><clinit>()V"}
    auto usedStringMethods = dexKit.FindMethodUsingString(
            "^NEW$",
            true,
            "",
            "",
            "",
            dexkit::null_param);
    std::cout << "\nFindMethodUsedString -> \n";
    for (auto &value: usedStringMethods) {
        std::cout << "\t" << value << "\n";
    }

    auto findMethods = dexKit.FindMethod(
            "Lcom/tencent/mobileqq/msf/sdk/MsfServiceSdk;",
            "",
            "int",
            dexkit::empty_param);
    std::cout << "\nFindMethod -> \n";
    for (auto &value: findMethods) {
        std::cout << "\t" << value << "\n";
    }

    // returns all subclasses of the specified class
    // result ex.
    // {"Landroidx/core/app/ComponentActivity;"}
    auto subClasses = dexKit.FindSubClasses("Lcom/tencent/mobileqq/activity/aio/BaseBubbleBuilder$d;");
    std::cout << "\nFindSubClasses -> \n";
    for (auto &value: subClasses) {
        std::cout << "\t" << value << "\n";
    }

    // returns all method descriptors matching the prefix of a sequence of bytecode operations
    // result ex.
    // {"Lcom/bumptech/glide/d/c;-><init>()V"}
    auto usedOpPrefixMethods = dexKit.FindMethodOpPrefixSeq(
            {0x70, 0x22, 0x70, 0x5b, 0x22, 0x70, 0x5b, 0x0e},
            "",
            "<init>",
            "V",
            dexkit::empty_param);
    std::cout << "\nFindMethodOpPrefixSeq -> \n";
    for (auto &value: usedOpPrefixMethods) {
        std::cout << "\t" << value << "\n";
    }

    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "used time: " << now_ms1.count() - now_ms.count() << " ms\n";

    return 0;
}