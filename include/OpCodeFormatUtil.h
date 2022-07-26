//
// Created by teble on 2022/1/6.
//

#ifndef DEXKIT_OPCODEFORMATUTIL_H
#define DEXKIT_OPCODEFORMATUTIL_H

#include <bits/stdc++.h>

typedef uint8_t ubyte;
typedef uint16_t ushort;
typedef uint32_t uint;
typedef uint32_t uleb128;

class OpCodeFormatUtil {
private:
    const std::string *opcodeFormat = new std::string[]{
            "10x", "12x", "22x", "32x", "12x", "22x", "32x", "12x", "22x", "32x",
            "11x", "11x", "11x", "11x", "10x", "11x", "11x", "11x", "11n", "21s",
            "31i", "21h", "21s", "31i", "51l", "21h", "21c", "31c", "21c", "11x",
            "11x", "21c", "22c", "12x", "21c", "22c", "35c", "3rc", "31t", "11x",
            "10t", "20t", "30t", "31t", "31t", "23x", "23x", "23x", "23x", "23x",
            "22t", "22t", "22t", "22t", "22t", "22t", "21t", "21t", "21t", "21t",
            "21t", "21t", "10x", "10x", "10x", "10x", "10x", "10x", "23x", "23x",
            "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x",
            "23x", "23x", "22c", "22c", "22c", "22c", "22c", "22c", "22c", "22c",
            "22c", "22c", "22c", "22c", "22c", "22c", "21c", "21c", "21c", "21c",
            "21c", "21c", "21c", "21c", "21c", "21c", "21c", "21c", "21c", "21c",
            "35c", "35c", "35c", "35c", "35c", "10x", "3rc", "3rc", "3rc", "3rc",
            "3rc", "10x", "10x", "12x", "12x", "12x", "12x", "12x", "12x", "12x",
            "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x",
            "12x", "12x", "12x", "12x", "23x", "23x", "23x", "23x", "23x", "23x",
            "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x",
            "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x", "23x",
            "23x", "23x", "23x", "23x", "23x", "23x", "12x", "12x", "12x", "12x",
            "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x",
            "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x",
            "12x", "12x", "12x", "12x", "12x", "12x", "12x", "12x", "22s", "22s",
            "22s", "22s", "22s", "22s", "22s", "22s", "22b", "22b", "22b", "22b",
            "22b", "22b", "22b", "22b", "22b", "22b", "22b", "10x", "10x", "10x",
            "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x",
            "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x", "10x",
            "45cc", "4rcc", "35c", "3rc", "21c", "21c"
    };

    const std::map<std::string, int> formatMap = {
            {"10x",  0x02},
            {"12x",  0x02},
            {"11n",  0x02},
            {"11x",  0x02},
            {"10t",  0x02},
            {"20t",  0x04},
            {"20bc", 0x04},
            {"22x",  0x04},
            {"21t",  0x04},
            {"21s",  0x04},
            {"21h",  0x04},
            {"21c",  0x04},
            {"23x",  0x04},
            {"22b",  0x04},
            {"22t",  0x04},
            {"22s",  0x04},
            {"22c",  0x04},
            {"22cs", 0x04},
            {"30t",  0x06},
            {"32x",  0x06},
            {"31i",  0x06},
            {"31t",  0x06},
            {"31c",  0x06},
            {"35c",  0x06},
            {"35ms", 0x06},
            {"35mi", 0x06},
            {"3rc",  0x06},
            {"3rms", 0x06},
            {"3rmi", 0x06},
            {"45cc", 0x08},
            {"4rcc", 0x08},
            {"51l",  0x0a},
    };

public:
    int getOpSize(uint hex) {
        return formatMap.at(opcodeFormat[hex & 0xff00 >> 8]);
    }

    int getOpSize(const ubyte *p) {
        return formatMap.at(opcodeFormat[*p & 0xff]);
    }

    int getOpSize(ubyte op) {
        return formatMap.at(opcodeFormat[op & 0xff]);
    }
};

#endif //DEXKIT_OPCODEFORMATUTIL_H
