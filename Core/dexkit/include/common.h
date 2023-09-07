#pragma once

#include <cstdint>

namespace dexkit {

void _checkFailed(const char *expr, int line, const char *file);

#ifndef EPS
#define EPS 1e-6
#endif

#ifndef DEXKIT_CHECK
#ifdef NDEBUG
#define DEXKIT_CHECK(expr)
#else
#define DEXKIT_CHECK(expr) do { if(!(expr)) dexkit::_checkFailed(#expr, __LINE__, __FILE__); } while(false)
#endif
#endif

union FloatInt {
    int32_t int_value;
    float float_value;
};

union DoubleLong {
    int64_t long_value;
    double double_value;
};

union NumberValue {
    int8_t L8;
    int16_t L16;
    FloatInt L32;
    DoubleLong L64;
};

enum NumberType: uint8_t {
    BYTE = 1,
    SHORT = 2,
    INT = 3,
    LONG = 4,
    FLOAT = 5,
    DOUBLE = 6
};

struct EncodeNumber {
    NumberType type;
    NumberValue value;
};

constexpr uint8_t GetNumberSize(NumberType type) {
    switch (type) {
        case BYTE: return 8;
        case SHORT: return 16;
        case INT: return 32;
        case LONG: return 64;
        case FLOAT: return 32;
        case DOUBLE: return 64;
    }
}

inline bool NumberTypeEqual(NumberType a, NumberType b) {
    if (a < FLOAT && b < FLOAT) return true;
    if (a >= FLOAT && b >= FLOAT) return true;
    return false;
}

}