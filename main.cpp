#include <iostream>
#include <fstream>
#include "slicer/reader.h"
#include "acdat/Builder.h"

using namespace dex;
using namespace std;

void locateClassInDex(dex::Reader &reader,
                      AhoCorasickDoubleArrayTrie<string> &acdat,
                      map<string, vector<string>> &resourceMap,
                      map<string, vector<string>> &resultMap) {
    map<int, string> strIndexMap;
    int strSize = (int) reader.StringIds().size();
    for (int index = 0; index < strSize; ++index) {
        const char *str = reader.GetStringMUTF8(index);
        std::function<void(int, int, string)> callback = [&strIndexMap, index](int begin, int end, const string &value) -> void {
            strIndexMap[index] = value;
        };
        acdat.parseText(str, callback);
        if (strIndexMap.empty()) {
            cout << "该dex不存在特征串" << endl;
            return;
        }
        bool confuseFlag = false;
        int classDefSize = (int) reader.ClassDefs().size();
        for (int idx = 0; idx < classDefSize; ++idx) {
            reader.CreateClassIr(idx);
            auto ir = reader.GetIr();
            if (ir->classes[idx]->static_fields.size() > 0) {

            }
        }
    }
}

int main() {
    map<string, vector<string>> obfuscate = {
            {"Lcom/tencent/mobileqq/activity/ChatActivityFacade;",               {"reSendEmo"}},
            {"Lcooperation/qzone/PlatformInfor;",                                {"52b7f2", "qimei"}},
            {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;", {"TroopClockInHandler"}}
    };

    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch());

    auto acdat = AhoCorasickDoubleArrayTrie<string>();
    map<string, string> buildMap;
    map<string, vector<string>> result;
    for (auto &[name, vec]: obfuscate) {
        for (auto &str: vec) {
            buildMap[str] = str;
        }
    }
    Builder<string>().build(buildMap, &acdat);

    for (int index = 1; index <= 29; ++index) {
        string path;
        if (index == 1) {
            path = "../dex/qq-8.9.2/classes.dex";
        } else {
            path = "../dex/qq-8.9.2/classes" + to_string(index) + ".dex";
        }
        ifstream in(path);
        std::vector<uint8_t> buf{std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
        dex::Reader reader(buf.data(), buf.size());

        locateClassInDex(reader, acdat, obfuscate, result);
    }
    auto now1 = std::chrono::system_clock::now();
    auto now_ms1 = std::chrono::duration_cast<std::chrono::milliseconds>(now1.time_since_epoch());
    cout << "used time: " << now_ms1.count() - now_ms.count() << endl;
    return 0;
}