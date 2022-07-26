#include <iostream>
#include <fstream>
#include <chrono>
#include <stack>
#include "slicer/reader.h"
#include "acdat/Builder.h"

#pragma clang diagnostic push
#pragma ide diagnostic ignored "LocalValueEscapesScope"
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
        std::function<void(int, int, string)> callback = [&strIndexMap, index](int begin, int end,
                                                                               const string &value) -> void {
            strIndexMap[index] = value;
            cout << "str: " << value << ", index: " << index << "\n";
        };
        acdat.parseText(str, callback);
    }
    if (strIndexMap.empty()) {
        cout << "该dex不存在特征串" << endl;
        return;
    }
    bool confuseFlag = false;
    int classDefSize = (int) reader.ClassDefs().size();
    for (int idx = 0; idx < classDefSize; ++idx) {
        reader.CreateClassIr(idx);
        auto ir = reader.GetIr();
        // class_def_item -> static_values
        if (!ir->classes[idx]->static_fields.empty()) {
            cout << "index: " << idx << ", class descriptor: "
                 << ir->classes[idx]->type->descriptor->c_str() << endl;
            for (auto &static_field: ir->classes[idx]->static_fields) {
                queue<ir::EncodedValue *> encodeValueQueue;
                for (auto value: ir->classes[idx]->static_init->values) {
                    encodeValueQueue.push(value);
                }
                while (!encodeValueQueue.empty()) {
                    auto value = encodeValueQueue.front();
                    encodeValueQueue.pop();
                    switch (value->type) {
                        case dex::kEncodedString: {
                            int index = (int) value->u.string_value->orig_index;
                            cout << "DBG: strIdx: " << index << ", str: " << value->u.string_value->c_str() << endl;
                            if (strIndexMap.contains(index)) {
                                cout << "class: " << ir->classes[idx]->type->descriptor->c_str()
                                     << " 在static_fields区域包含字符串: " << strIndexMap[index] << "\n";
                                return;
                            }
                            break;
                        }
                        case dex::kEncodedArray: {
                            for (auto v: value->u.array_value->values) {
                                encodeValueQueue.push(v);
                            }
                            break;
                        }
                        case dex::kEncodedAnnotation: {
                            for (auto v: value->u.annotation_value->elements) {
                                encodeValueQueue.push(v->value);
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }
        // class_def_item -> class_data_item -> direct_methods
        if (!ir->classes[idx]->direct_methods.empty()) {
            for (auto method: ir->classes[idx]->direct_methods) {
                auto insns = method->code->instructions;
                int codeIndex = 0;
                while (codeIndex < insns.size()) {
                    auto ident = insns[codeIndex];
                    switch (ident) {
                        case 0x0100: {
                            // https://source.android.com/devices/tech/dalvik/dalvik-bytecode?hl=zh-cn#packed-switch
                            // packed-switch-payload
                            auto size = insns[codeIndex + 1];
                            codeIndex += (size * 2 + 4);
                            break;
                        }
                        case 0x0200: {
                            // sparse-switch-payload
                            auto size = insns[codeIndex + 1];
                            codeIndex += (size * 4 + 2);
                            break;
                        }
                        case 0x0300: {
                            // fill-array-data-payload
                            auto element_width = insns[codeIndex + 1];
                            uint16_t size = insns[codeIndex + 2] | (insns[codeIndex + 3] << 16);
                            codeIndex += ((element_width * size + 1) / 2 + 4);
                            break;
                        }
                        default: {

                        }
                    }
                    uint8_t op = ident & 0xff;
                    auto size = ident & 0xff >> 8;
                    if (op == 0 && codeIndex + 1 < insns.size()) {
                        auto type = insns[codeIndex + 1];
                        if (type >= 1 && type <= 3) {
                            switch (type) {
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3: {
                                    // fill-array-data-payload
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        // class_def_item -> class_data_item -> virtual_methods
        if (!ir->classes[idx]->virtual_methods.empty()) {

        }
    }
}

int main() {
    map<string, vector<string>> obfuscate = {
            {"Lcom/tencent/mobileqq/activity/ChatActivityFacade;",               {"reSendEmo"}},
            {"Lcooperation/qzone/PlatformInfor;",                                {"52b7f2", "qimei"}},
            {"Lcom/tencent/mobileqq/troop/clockin/handler/TroopClockInHandler;", {"TroopClockInHandler"}},
            {"test",                                                             {"mark_uin_upload"}},
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

    for (int index = 1; index <= 1; ++index) {
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

#pragma clang diagnostic pop