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

#include <map>
#include <cstring>
#include <string_view>
#include <utility>
#include <unistd.h>
#if defined(_WIN32) || defined(WIN32)
#include <mmap_windows.h>
#include <codecvt>
#include <locale>
#else
#include <sys/mman.h>
#endif
#include <sys/stat.h>
#include <fcntl.h>
#include <zlib.h>

namespace dexkit {

struct MemMap {
    MemMap() = default;

    explicit MemMap(std::string_view path) { open(path); }

    explicit MemMap(uint32_t len) {
        auto *addr = mmap(nullptr, len, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr != MAP_FAILED) {
            base = static_cast<uint8_t *>(addr);
            size = len;
        }
    }

    explicit MemMap(uint8_t *addr, uint32_t len) {
        auto *map = mmap(addr, len, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (map != MAP_FAILED) {
            base = static_cast<uint8_t *>(addr);
            size = len;
            memcpy((void *) base, addr, len);
#if !(defined(_WIN32) || defined(WIN32))
            mprotect((void *) base, size, PROT_READ);
#endif
        }
    }

    bool open(std::string_view path) {
#if !(defined(_WIN32) || defined(WIN32))
        int m_fd = ::open(path.data(), O_RDONLY | O_CLOEXEC);
#else
        using convert_type = std::codecvt_utf8<wchar_t>;
        auto ws_path = std::wstring_convert<convert_type, wchar_t>().from_bytes(path.data());
        int m_fd = _wopen(ws_path.data(), O_RDONLY | O_BINARY);
#endif
        if (m_fd >= 0) {
            struct stat s{};
            fstat(m_fd, &s);
            auto *addr = mmap(nullptr, s.st_size, PROT_READ, MAP_PRIVATE, m_fd, 0);
            if (addr != MAP_FAILED) {
                base = static_cast<uint8_t *>(addr);
                size = s.st_size;
                fd = m_fd;
                return true;
            }
        }
        return false;
    }

    ~MemMap() {
        if (fd >= 0) close(fd);
        if (ok()) munmap((void *) base, size);
    }

    MemMap(MemMap &&other) noexcept: base(other.base), size(other.size), fd(other.fd) {
        other.base = nullptr;
        other.size = 0;
        other.fd = -1;
    }

    MemMap(const MemMap &) = delete;

    MemMap &operator=(const MemMap &) = delete;

    [[nodiscard]] bool ok() const { return base && size; }

    [[nodiscard]] auto data() const { return base; }

    [[nodiscard]] auto len() const { return size; }

private:
    const uint8_t *base = nullptr;
    size_t size = 0;
    int fd = -1;
};

} // namespace dexkit