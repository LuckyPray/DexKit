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

    dexkit::DexKit dexKit("../dex/qq-8.9.2/qq-8.9.2.apk");

    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());

    // 返回混淆map中包含所有字符串的类, 高级搜索可以使用 '^'与'$'限制字符串匹配，与正则表达式语义一致
    auto res = dexKit.LocationClasses(obfuscate, true);
//    dexKit.SetThreadNum(1);
    for (auto &[key, value]: res) {
        std::cout << key << " -> \n";
        for (auto &v: value) {
            std::cout << "\t" << v << "\n";
        }
    }

    std::vector<std::string> v;
    std::vector<size_t> p{};
    // 返回所有通过invoke-kind指令调用指定的方法的方法描述符
    // 指定了方法签名后会自动填充后面的参数
    // 如果不指定方法签名则可按参数模糊匹配，使用空串则代表模糊匹配
    auto res1 = dexKit.FindMethodInvoked("Landroid/widget/TextView;->setCompoundDrawablesWithIntrinsicBounds(IIII)V",
                                         "", "", "", v, p, false);
    std::cout << "FindMethodInvoked -> \n";
    for (auto &value: res1) {
        std::cout << "\t" << value << "\n";
    }

    // 返回所有指定类的子类，自动判断该类是否是接口或抽象类
    auto res2 = dexKit.FindSubClasses("Landroid/app/Activity;");
    std::cout << "FindSubClasses -> \n";
    for (auto &value: res2) {
        std::cout << "\t" << value << "\n";
    }

    std::vector<uint8_t> op_seq{0x70, 0x22, 0x70, 0x5b, 0x22, 0x70, 0x5b, 0x0e};
    // 返回所有符合字节码op序列前缀的方法描述符
    auto res3 = dexKit.FindMethodOpPrefixSeq(op_seq, "Lcom/bumptech/glide/d/c;", "<init>", "void", {}, p, false);
    std::cout << "FindMethodOpPrefixSeq -> \n";
    for (auto &value: res3) {
        std::cout << "\t" << value << "\n";
    }

    // 返回所有使用了匹配字符串的方法描述符
    // 高级搜索可以使用 '^'与'$'限制字符串匹配，与正则表达式语义一致
    auto res4 = dexKit.FindMethodUsedString("^NEW$", {}, {}, {}, {}, p, true, true);
    std::cout << "FindMethodUsedString -> \n";
    for (auto &value: res4) {
        std::cout << "\t" << value << "\n";
    }

    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    std::cout << "used time: " << now_ms1.count() - now_ms.count() << " ms\n";
//    while (true) sleep(1);
    return 0;
}