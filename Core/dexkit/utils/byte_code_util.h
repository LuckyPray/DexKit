#pragma once

#include "slicer/dex_format.h"

namespace dexkit {

inline dex::u4 ReadInt(const dex::u2 **ptr) {
    const dex::u2 *p = *ptr;
    int res = *p++;
    res |= (*p++ << 16);
    *ptr = p;
    return res;
}

inline dex::u4 ReadInt(const dex::u2 *ptr) {
    return ptr[0] | (ptr[1] << 16);
}

inline dex::u2 ReadShort(const dex::u2 **ptr) {
    const dex::u2 *p = *ptr;
    int res = *p++;
    *ptr = p;
    return res;
}

inline dex::u2 ReadShort(const dex::u2 *ptr) {
    return *ptr;
}

inline dex::u4 ReadULeb128(const dex::u1 **ptr) {
    const dex::u1 *p = *ptr;
    dex::u1 size = 0;
    dex::u4 res = 0;
    do {
        res |= (*p & 0x7f) << 7 * size++;
    } while ((*p++ & 0x80) == 0x80);
    *ptr = p;
    return res;
}

}
