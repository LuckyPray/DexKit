// DexKit - An high-performance runtime parsing library for dex
// implemented in C++.
// Copyright (C) 2022-2023 LuckyPray
// https://github.com/LuckyPray/DexKit
//
// This program is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation, either
// version 3 of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// <https://www.gnu.org/licenses/>.
// <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.

#pragma once

#include <cstdint>
#include "slicer/dex_format.h"

namespace dexkit {

// Keep reflection-visible hidden bits such as BRIDGE, VARARGS and SYNTHETIC.
inline uint32_t JavaModifiers(uint32_t access_flags) {
    return access_flags & 0xffffu;
}

// Reflection clears the raw SYNCHRONIZED bit and rebuilds it from DECLARED_SYNCHRONIZED,
// including for native methods. It also hides NATIVE on abstract runtime stubs.
// Android Executable.fixMethodFlags():
// https://android.googlesource.com/platform/libcore/+/refs/tags/android-15.0.0_r1/ojluni/src/main/java/java/lang/reflect/Executable.java#609
inline uint32_t JavaMethodModifiers(uint32_t access_flags) {
    if (access_flags & dex::kAccAbstract) access_flags &= ~dex::kAccNative;
    access_flags &= ~dex::kAccSynchronized;
    if (access_flags & dex::kAccDeclaredSynchronized) access_flags |= dex::kAccSynchronized;
    return JavaModifiers(access_flags);
}

} // namespace dexkit
