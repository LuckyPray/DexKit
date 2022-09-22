#pragma once

#include <string_view>
#include <utility>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <zlib.h>
#include <iostream>

namespace dexkit {

struct MemMap {
    MemMap() = default;

    explicit MemMap(std::string_view file_name) {
        int fd = open(file_name.data(), O_RDONLY | O_CLOEXEC);
        if (fd > 0) {
            struct stat s{};
            fstat(fd, &s);
            auto *addr = mmap(nullptr, s.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
            if (addr != MAP_FAILED) {
                addr_ = static_cast<uint8_t *>(addr);
                len_ = s.st_size;
            }
        }
        close(fd);
    }

    explicit MemMap(size_t size) {
        auto *addr = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr != MAP_FAILED) {
            addr_ = static_cast<uint8_t *>(addr);
            len_ = size;
        }
    }

    ~MemMap() {
        if (ok()) {
            munmap(addr_, len_);
        }
    }

    [[nodiscard]] bool ok() const { return addr_ && len_; }

    [[nodiscard]] auto addr() const { return addr_; }

    [[nodiscard]] auto len() const { return len_; }

    MemMap(MemMap &&other) noexcept: addr_(other.addr_), len_(other.len_) {
        other.addr_ = nullptr;
        other.len_ = 0;
    }

    MemMap &operator=(MemMap &&other) noexcept {
        new(this) MemMap(std::move(other));
        return *this;
    }

    MemMap(const MemMap &) = delete;

    MemMap &operator=(const MemMap &) = delete;

private:
    uint8_t *addr_ = nullptr;
    size_t len_ = 0;
};

static void *myalloc([[maybe_unused]] void *q, unsigned n, unsigned m) {
    return calloc(n, m);
}

static void myfree([[maybe_unused]] void *q, void *p) {
    (void) q;
    free(p);
}

struct [[gnu::packed]] ZipLocalFile {
    static ZipLocalFile *from(uint8_t *begin) {
        auto *file = reinterpret_cast<ZipLocalFile *>(begin);
        if (file->signature == 0x04034b50u) {
            return file;
        } else {
            return nullptr;
        }
    }

    uint32_t getDataDescriptorSize() {
        if (this->flags & 0x8u) {
            auto nextPtr = reinterpret_cast<uint8_t *>(this) + sizeof(ZipLocalFile) +
                    this->file_name_length + this->extra_length + this->compress_size;
            auto descSign = reinterpret_cast<uint32_t *>(nextPtr);
            if (*descSign == 0x08074b50u) {
                return 16;
            } else {
                return 12;
            }
        }
        return 0;
    }

    ZipLocalFile *next() {
        return from(reinterpret_cast<uint8_t *>(this) +
                    sizeof(ZipLocalFile) + file_name_length + extra_length + compress_size + getDataDescriptorSize());
    }

    MemMap uncompress() {
        if (compress == 0x8) {
            MemMap out(uncompress_size);
            if (!out.ok()) {
                return {};
            }
            int err;
            z_stream d_stream; /* decompression stream */

            d_stream.zalloc = myalloc;
            d_stream.zfree = myfree;
            d_stream.opaque = nullptr;

            d_stream.next_in = data();
            d_stream.avail_in = compress_size;
            d_stream.next_out = out.addr();            /* discard the output */
            d_stream.avail_out = out.len();

            err = inflateInit2(&d_stream, -MAX_WBITS);
            if (err != Z_OK) {
                return {};
            }

            for (int c = 0;; ++c) {
                err = inflate(&d_stream, Z_NO_FLUSH);
                if (err == Z_STREAM_END) break;
                if (err != Z_OK) {
                    return {};
                }
            }

            err = inflateEnd(&d_stream);
            if (err != Z_OK) {
                return {};
            }

            if (d_stream.total_out != uncompress_size) {
                return {};
            }
            mprotect(out.addr(), out.len(), PROT_READ);
            return out;
        } else if (compress == 0 && compress_size == uncompress_size) {
            MemMap out(uncompress_size);
            memcpy(out.addr(), data(), uncompress_size);
            mprotect(out.addr(), out.len(), PROT_READ);
            return out;
        }
        return {};
    }

    std::string_view file_name() {
        return {name, file_name_length};
    }

    uint8_t *data() {
        return reinterpret_cast<uint8_t *>(this) + sizeof(ZipLocalFile) + file_name_length +
               extra_length;
    }

    [[maybe_unused]] uint32_t signature;
    [[maybe_unused]] uint16_t version;
    [[maybe_unused]] uint16_t flags;
    [[maybe_unused]] uint16_t compress;
    [[maybe_unused]] uint16_t last_modify_time;
    [[maybe_unused]] uint16_t last_modify_date;
    [[maybe_unused]] uint32_t crc;
    [[maybe_unused]] uint32_t compress_size;
    [[maybe_unused]] uint32_t uncompress_size;
    [[maybe_unused]] uint16_t file_name_length;
    [[maybe_unused]] uint16_t extra_length;
    [[maybe_unused]] char name[0];
};

class ZipFile {
public:
    static std::unique_ptr<ZipFile> Open(const MemMap &map) {
        static ZipLocalFile *local_file;
        local_file = ZipLocalFile::from(map.addr());
        if (!local_file) return nullptr;
        auto r = std::make_unique<ZipFile>();
        while (local_file) {
            r->entries.emplace(local_file->file_name(), local_file);
            local_file = local_file->next();
        }
        return r;
    }

    ZipLocalFile *Find(std::string_view entry_name) {
        if (auto i = entries.find(entry_name); i != entries.end()) {
            return i->second;
        }
        return nullptr;
    }

private:
    std::map<std::string_view, ZipLocalFile *> entries;
};

}
