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

#ifndef DEXKIT_ERROR_LIST_H
#define DEXKIT_ERROR_LIST_H


// V(name, msg)
#define DEXKIT_ERROR_LIST(V) \
    V(SUCCESS, "Success") \
    V(EXPORT_DEX_FP_NULL, "Export dex file pointer is null") \
    V(FILE_NOT_FOUND, "File not found") \
    V(OPEN_ZIP_FILE_FAILED, "Open zip file failed") \
    V(OPEN_FILE_FAILED, "Open file failed") \
    V(ADD_DEX_AFTER_CROSS_BUILD, "Add dex after cross build")\
    V(WRITE_FILE_INCOMPLETE, "Incomplete file written")


#endif //DEXKIT_ERROR_LIST_H
#undef DEXKIT_ERROR_LIST_H