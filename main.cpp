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
    auto classes = dexKit.LocationClasses(obfuscate, true);
    std::cout << "\nLocationClasses -> \n";
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
    auto methods = dexKit.LocationMethods(obfuscate, true);
    std::cout << "\nLocationMethods -> \n";
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
    auto invokedMethod = dexKit.FindMethodInvoked(
            "Lcom/tencent/mobileqq/app/CardHandler;->X6(Lcom/tencent/qphone/base/remote/ToServiceMsg;Lcom/tencent/qphone/base/remote/FromServiceMsg;Ljava/lang/Object;Landroid/os/Bundle;)V",
            {},
            {},
            {},
            {},
            {},
            false);
    std::cout << "\nFindMethodInvoked -> \n";
    for (auto &value: invokedMethod) {
        std::cout << "\t" << value << "\n";
    }

    // returns all subclasses of the specified class
    // result ex.
    // {"Landroidx/core/app/ComponentActivity;"}
    auto subClasses = dexKit.FindSubClasses("Landroid/app/Activity;");
    std::cout << "\nFindSubClasses -> \n";
    for (auto &value: subClasses) {
        std::cout << "\t" << value << "\n";
    }

    // returns all method descriptors matching the prefix of a sequence of bytecode operations
    // result ex.
    // {"Lcom/bumptech/glide/d/c;-><init>()V"}
    auto usedOpPrefixMethods = dexKit.FindMethodOpPrefixSeq(
            {0x70, 0x22, 0x70, 0x5b, 0x22, 0x70, 0x5b, 0x0e},
            "Lcom/bumptech/glide/d/c;",
            "<init>",
            "void",
            {},
            {},
            false);
    std::cout << "\nFindMethodOpPrefixSeq -> \n";
    for (auto &value: usedOpPrefixMethods) {
        std::cout << "\t" << value << "\n";
    }

    // find all used matching string's method
    // if `advanced_match = true` you can use '^' and '$' to restrict string matching,
    // result ex.
    // {"Lcom/tencent/aekit/openrender/internal/Frame$Type;-><clinit>()V"}
    auto usedStringMethods = dexKit.FindMethodUsedString(
            "^NEW$", {},
            {},
            {},
            {},
            {},
            true,
            true);
    std::cout << "\nFindMethodUsedString -> \n";
    for (auto &value: usedStringMethods) {
        std::cout << "\t" << value << "\n";
    }

    auto findMethods = dexKit.FindMethod(
            "com.tencent.mobileqq.x.a",
            "i6",
            "",
            {},
            {},
            true);
    std::cout << "\nFindMethod -> \n";
    for (auto &value: findMethods) {
        std::cout << "\t" << value << "\n";
    }

    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "used time: " << now_ms1.count() - now_ms.count() << " ms\n";

    return 0;
}