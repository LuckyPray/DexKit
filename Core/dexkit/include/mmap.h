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
#include <map>
#include <memory>
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

    explicit MemMap(size_t len) {
        auto *addr = mmap(nullptr, len, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr != MAP_FAILED) {
            base = static_cast<uint8_t *>(addr);
            size = len;
        }
    }

    explicit MemMap(const uint8_t *addr, size_t len) : MemMap(len) {
        if (ok()) {
            memcpy(base, addr, len);
#if !(defined(_WIN32) || defined(WIN32))
            mprotect(base, size, PROT_READ);
#endif
        }
    }

    // The owner keeps borrowed bytes alive; only owning mappings may unmap them.
    static MemMap view(const uint8_t *addr, size_t len, std::shared_ptr<const void> owner) {
        if (!addr || len == 0 || !owner) return {};
        MemMap view;
        view.base = const_cast<uint8_t *>(addr);
        view.size = len;
        view.owns_mapping = false;
        view.owner = std::move(owner);
        return view;
    }

    static MemMap slice(std::shared_ptr<const MemMap> mapping, size_t offset, size_t len) {
        if (!mapping || !mapping->ok() || offset > mapping->len()
            || len > mapping->len() - offset) return {};
        const auto *data = mapping->data() + offset;
        return view(data, len, std::move(mapping));
    }

    bool open(std::string_view path) {
        if (ok()) return false;
#if !(defined(_WIN32) || defined(WIN32))
        int m_fd = ::open(path.data(), O_RDONLY | O_CLOEXEC);
#else
        using convert_type = std::codecvt_utf8<wchar_t>;
        auto ws_path = std::wstring_convert<convert_type, wchar_t>().from_bytes(path.data());
        int m_fd = _wopen(ws_path.data(), O_RDONLY | O_BINARY);
#endif
        if (m_fd >= 0) {
            struct stat s{};
            if (fstat(m_fd, &s) != 0 || s.st_size <= 0
                || static_cast<uint64_t>(s.st_size) > SIZE_MAX) {
                close(m_fd);
                return false;
            }
            auto *addr = mmap(nullptr, s.st_size, PROT_READ, MAP_PRIVATE, m_fd, 0);
            if (addr != MAP_FAILED) {
                base = static_cast<uint8_t *>(addr);
                size = s.st_size;
                fd = m_fd;
                owns_mapping = true;
                return true;
            }
            close(m_fd);
        }
        return false;
    }

    ~MemMap() {
        if (fd >= 0) close(fd);
        if (owns_mapping && ok()) munmap((void *) base, size);
    }

    MemMap(MemMap &&other) noexcept
            : base(other.base), size(other.size), fd(other.fd),
              owns_mapping(other.owns_mapping), owner(std::move(other.owner)) {
        other.base = nullptr;
        other.size = 0;
        other.fd = -1;
    }

    MemMap(const MemMap &) = delete;

    MemMap &operator=(const MemMap &) = delete;

    [[nodiscard]] bool ok() const { return base && size; }

    [[nodiscard]] uint8_t* data() const { return base; }

    [[nodiscard]] size_t len() const { return size; }

private:
    uint8_t *base = nullptr;
    size_t size = 0;
    int fd = -1;
    bool owns_mapping = true;
    std::shared_ptr<const void> owner;
};

} // namespace dexkit
