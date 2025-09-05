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
#include <string>
#include <string_view>
#include <vector>
#include <map>
#include <memory>
#include <limits>
#include <cassert>
#include "mmap.h"

namespace dexkit {

static constexpr uint32_t SIG_LOCAL_FILE = 0x04034B50u;
static constexpr uint32_t SIG_DATA_DESC = 0x08074B50u;
static constexpr uint32_t SIG_CFR = 0x02014B50u;
static constexpr uint32_t SIG_EOCD = 0x06054B50u;
static constexpr uint32_t SIG_ZIP64_LOC = 0x07064B50u;
static constexpr uint32_t SIG_ZIP64_EOCD = 0x06064B50u;

static constexpr uint16_t GPBF_HAS_DATA_DESC = 0x0008;   // 通用标志位 bit 3
static constexpr uint16_t COMP_STORE = 0x0000;
static constexpr uint16_t COMP_DEFLATE = 0x0008;

#if defined(_MSC_VER)
#  pragma pack(push, 1)
#  define ZIP_PACKED
#else
#  define ZIP_PACKED __attribute__((packed))
#endif

struct ZIP_PACKED LocalFileHeader {
    uint32_t signature;         // 0x04034B50
    uint16_t version_needed;
    uint16_t gp_flags;
    uint16_t method;
    uint16_t dos_time;
    uint16_t dos_date;
    uint32_t crc32;
    uint32_t comp_size;         // maybe 0（若使用 Data Descriptor）
    uint32_t uncomp_size;       // maybe 0
    uint16_t name_len;
    uint16_t extra_len;

    [[nodiscard]] const uint8_t *name() const {
        return reinterpret_cast<const uint8_t *>(this) + sizeof(LocalFileHeader);
    }

    [[nodiscard]] const uint8_t *extra() const {
        return name() + name_len;
    }

    [[nodiscard]] const uint8_t *data() const {
        return extra() + extra_len;
    }
};

static_assert(sizeof(LocalFileHeader) == 30, "LocalFileHeader must be 30 bytes");

struct ZIP_PACKED CentralDirHeader {
    uint32_t signature;           // 0x02014B50
    uint16_t ver_made_by;
    uint16_t ver_needed;
    uint16_t gp_flags;
    uint16_t method;
    uint16_t dos_time;
    uint16_t dos_date;
    uint32_t crc32;
    uint32_t comp_size32;         // 0xFFFFFFFF -> ZIP64 extra
    uint32_t uncomp_size32;       // 0xFFFFFFFF -> ZIP64 extra
    uint16_t name_len;
    uint16_t extra_len;
    uint16_t cmt_len;
    uint16_t disk_start;
    uint16_t int_attr;
    uint32_t ext_attr;
    uint32_t lfh_offset32;        // 0xFFFFFFFF -> ZIP64 extra

    [[nodiscard]] const uint8_t *name() const {
        return reinterpret_cast<const uint8_t *>(this) + sizeof(CentralDirHeader);
    }

    [[nodiscard]] const uint8_t *extra() const {
        return name() + name_len;
    }

    [[nodiscard]] const uint8_t *comment() const {
        return extra() + extra_len;
    }
};

static_assert(sizeof(CentralDirHeader) == 46, "CentralDirHeader must be 46 bytes");

struct ZIP_PACKED EOCD {
    uint32_t signature;               // 0x06054B50
    uint16_t disk_no;
    uint16_t cd_start_disk_no;
    uint16_t entries_on_disk;
    uint16_t entries_total;
    uint32_t cd_size32;
    uint32_t cd_offset32;
    uint16_t cmt_len;

    [[nodiscard]] const uint8_t *comment() const {
        return reinterpret_cast<const uint8_t *>(this) + sizeof(EOCD);
    }
};

static_assert(sizeof(EOCD) == 22, "EOCD must be 22 bytes");

struct ZIP_PACKED Zip64EOCDLocator {
    uint32_t signature;           // 0x07064B50
    uint32_t start_disk_number;
    uint64_t zip64_eocd_offset;
    uint32_t total_number_of_disks;
};
static_assert(sizeof(Zip64EOCDLocator) == 20, "Zip64EOCDLocator must be 20 bytes");

struct ZIP_PACKED Zip64EOCD {
    uint32_t signature;             // 0x06064B50
    uint64_t size_of_record;        // 不含 signature+size(12 字节)
    uint16_t ver_made_by;
    uint16_t ver_needed;
    uint32_t number_of_this_disk;
    uint32_t cd_start_disk_number;
    uint64_t entries_on_this_disk;
    uint64_t entries_total;
    uint64_t cd_size;
    uint64_t cd_offset;

    [[nodiscard]] const uint8_t *extensible() const {
        return reinterpret_cast<const uint8_t *>(this) + sizeof(Zip64EOCD);
    }

    [[nodiscard]] uint64_t total_size() const {
        return 12u + size_of_record;
    }
};

static_assert(sizeof(Zip64EOCD) == 56, "Zip64EOCD minimum 56 bytes");

#if defined(_MSC_VER)
#  pragma pack(pop)
#endif
#undef ZIP_PACKED


inline bool in_range(const MemMap &m, const void *p, size_t need) {
    auto b = reinterpret_cast<uintptr_t>(m.data());
    auto e = b + m.len();
    auto q = reinterpret_cast<uintptr_t>(p);
    return q >= b && need <= m.len() && (q + need) <= e;
}

struct Entry {
    std::string name;
    uint16_t method = 0;
    uint16_t flags = 0;
    uint32_t crc32 = 0;
    bool zip64 = false;

    uint64_t comp_size = 0;
    uint64_t uncomp_size = 0;

    uint64_t lfh_offset = 0;
    uint64_t data_offset = 0;
};

class ZipArchive {
public:
    using DeflateProbe = bool (*)(const uint8_t *comp, size_t comp_len, uint64_t &out_uncomp_len);

    void SetDeflateProbe(DeflateProbe fn) { probe_deflate_ = fn; }

    static std::unique_ptr<ZipArchive> Open(const MemMap &mm, bool allow_local_scan = false) {
        if (!mm.ok()) return nullptr;
        auto za = std::unique_ptr<ZipArchive>(new ZipArchive(mm));
        auto has_cd = za->parse_from_central();
        // trust central
        if (has_cd && !allow_local_scan) {
            return za;
        }
        if ((!has_cd || allow_local_scan) && za->scan_from_local()) {
            return za;
        }
        return nullptr;
    }

    [[nodiscard]] const Entry *Find(std::string_view name) const {
        auto it = map_.find(std::string(name));
        if (it == map_.end()) return nullptr;
        return &entries[it->second];
    }

    [[nodiscard]] const std::vector<Entry> &GetEntries() const { return entries; }

    [[nodiscard]] bool GetCompressedSlice(const Entry &e, const uint8_t *&ptr, size_t &len) const {
        if (e.data_offset + e.comp_size > mm_.len()) return false;
        ptr = mm_.data() + e.data_offset;
        len = static_cast<size_t>(e.comp_size);
        return true;
    }

    [[nodiscard]] MemMap GetUncompressData(const Entry& e) const {
        MemMap out(e.uncomp_size);
        if (!out.ok()) return {};

        const auto *lfh = reinterpret_cast<const LocalFileHeader *>(mm_.data() + e.lfh_offset);

        if (e.method == COMP_STORE) {
            if (e.uncomp_size != e.comp_size) return {};
            std::memcpy(const_cast<uint8_t*>(out.data()), lfh->data(), e.uncomp_size);
        } else if (e.method == COMP_DEFLATE) {
            z_stream s{};
            s.zalloc = Z_NULL;
            s.zfree  = Z_NULL;
            s.opaque = nullptr;

            s.next_in = const_cast<Bytef *>(reinterpret_cast<const Bytef *>(lfh->data()));
            s.avail_in = static_cast<uInt>(e.comp_size);
            s.next_out = const_cast<Bytef *>(reinterpret_cast<const Bytef *>(out.data()));
            s.avail_out = static_cast<uInt>(out.len());

            int ret = inflateInit2(&s, -MAX_WBITS);
            if (ret != Z_OK) return {};

            while (true) {
                ret = inflate(&s, Z_NO_FLUSH);
                if (ret == Z_STREAM_END) break;
                if (ret != Z_OK) { inflateEnd(&s); return {}; }
                if (s.avail_out == 0 && s.avail_in != 0) { inflateEnd(&s); return {}; }
            }
            inflateEnd(&s);

            if (s.total_out != e.uncomp_size) return {};
        } else {
            return {};
        }

#if !(defined(_WIN32) || defined(WIN32))
        mprotect(const_cast<uint8_t*>(out.data()), out.len(), PROT_READ);
#endif
        return out;
    }

private:
    explicit ZipArchive(const MemMap &mm) : mm_(mm) {}

    bool parse_from_central() {
        const EOCD *eocd = find_eocd();
        if (!eocd) return false;

        uint64_t cd_off = eocd->cd_offset32;
        uint64_t cd_len = eocd->cd_size32;
        uint64_t num = eocd->entries_total;
        bool is_zip64 = (eocd->cd_offset32 == 0xFFFFFFFFu)
                        || (eocd->cd_size32 == 0xFFFFFFFFu)
                        || (eocd->entries_total == 0xFFFFu)
                        || (eocd->entries_on_disk == 0xFFFFu);

        if (is_zip64) {
            if (reinterpret_cast<const uint8_t *>(eocd) < mm_.data() + 20) return false;
            const auto *loc = reinterpret_cast<const Zip64EOCDLocator *>(reinterpret_cast<const uint8_t *>(eocd) - 20);
            if (!in_range(mm_, loc, sizeof(Zip64EOCDLocator)) || loc->signature != SIG_ZIP64_LOC) return false;

            if (loc->zip64_eocd_offset > mm_.len() || loc->zip64_eocd_offset + sizeof(Zip64EOCD) > mm_.len())
                return false;

            const auto *z64 = reinterpret_cast<const Zip64EOCD *>(mm_.data() + loc->zip64_eocd_offset);
            if (!in_range(mm_, z64, sizeof(Zip64EOCD)) || z64->signature != SIG_ZIP64_EOCD) return false;

            cd_off = z64->cd_offset;
            cd_len = z64->cd_size;
            num = z64->entries_total;
        }

        return parse_cd_block(cd_off, cd_len, num);
    }

    [[nodiscard]] const EOCD *find_eocd() const {
        if (mm_.len() < sizeof(EOCD)) return nullptr;
        const uint8_t *end = mm_.data() + mm_.len();
        size_t max_back = std::min<size_t>(mm_.len(), 22 + 0xFFFFu);
        for (size_t back = 0; back <= max_back - 22; ++back) {
            const uint8_t *p = end - 22 - back;
            const auto *e = reinterpret_cast<const EOCD *>(p);
            if (!in_range(mm_, e, sizeof(EOCD))) break;
            if (e->signature != SIG_EOCD) continue;
            if (p + sizeof(EOCD) + e->cmt_len == end) return e;
        }
        return nullptr;
    }

    bool parse_cd_block(uint64_t cd_off, uint64_t cd_len, uint64_t expected_num) {
        if (cd_off + cd_len > mm_.len()) return false;
        const uint8_t *p = mm_.data() + cd_off;
        const uint8_t *end = p + cd_len;

        entries.clear();
        map_.clear();
        while (p + sizeof(CentralDirHeader) <= end) {
            const auto *c = reinterpret_cast<const CentralDirHeader *>(p);
            if (c->signature != SIG_CFR) break;

            size_t hdr_total = sizeof(CentralDirHeader) + c->name_len + c->extra_len + c->cmt_len;
            if (p + hdr_total > end) return false;

            Entry e{};
            e.flags = c->gp_flags;
            e.method = c->method;
            e.crc32 = c->crc32;
            e.zip64 = false;

            if (c->name_len) e.name.assign(reinterpret_cast<const char *>(c->name()), c->name_len);

            e.comp_size = c->comp_size32;
            e.uncomp_size = c->uncomp_size32;
            e.lfh_offset = c->lfh_offset32;

            if (c->extra_len) {
                const uint8_t *ex = c->extra();
                const uint8_t *ex_end = ex + c->extra_len;
                while (ex + 4 <= ex_end) {
                    uint16_t tag = ex[0] | (uint16_t(ex[1]) << 8);
                    uint16_t xlen = ex[2] | (uint16_t(ex[3]) << 8);
                    ex += 4;
                    if (ex + xlen > ex_end) break;
                    // zip64 extra field
                    if (tag == 0x0001) {
                        const uint8_t *q = ex;
                        if (c->uncomp_size32 == 0xFFFFFFFFu && q + 8 <= ex + xlen) {
                            e.uncomp_size = read_u64(q);
                            q += 8;
                            e.zip64 = true;
                        }
                        if (c->comp_size32 == 0xFFFFFFFFu && q + 8 <= ex + xlen) {
                            e.comp_size = read_u64(q);
                            q += 8;
                            e.zip64 = true;
                        }
                        if (c->lfh_offset32 == 0xFFFFFFFFu && q + 8 <= ex + xlen) {
                            e.lfh_offset = read_u64(q);
                            q += 8;
                            e.zip64 = true;
                        }
                    }
                    ex += xlen;
                }
            }

            if (e.lfh_offset + sizeof(LocalFileHeader) > mm_.len()) return false;
            const auto *lfh = reinterpret_cast<const LocalFileHeader *>(mm_.data() + e.lfh_offset);
            if (lfh->signature != SIG_LOCAL_FILE) return false;
            if (!in_range(mm_, lfh, sizeof(LocalFileHeader))) return false;
            if (e.lfh_offset + sizeof(LocalFileHeader) + lfh->name_len + lfh->extra_len > mm_.len()) return false;
            e.data_offset = e.lfh_offset + sizeof(LocalFileHeader) + lfh->name_len + lfh->extra_len;

            entries.push_back(std::move(e));
            map_[entries.back().name] = entries.size() - 1;

            p += hdr_total;
        }

        return !entries.empty();
    }

    static uint64_t read_u64(const uint8_t *p) {
        uint32_t lo = p[0] | (uint32_t(p[1]) << 8) | (uint32_t(p[2]) << 16) | (uint32_t(p[3]) << 24);
        uint32_t hi = p[4] | (uint32_t(p[5]) << 8) | (uint32_t(p[6]) << 16) | (uint32_t(p[7]) << 24);
        return (uint64_t(hi) << 32) | lo;
    }

    static inline bool all_zero(const uint8_t *p, size_t n) {
        for (size_t i = 0; i < n; ++i) if (p[i] != 0) return false;
        return true;
    }

    static void parse_lfh_extra(const LocalFileHeader *lfh, Entry &e) {
        if (lfh->extra_len == 0) return;
        const uint8_t *ex = lfh->extra();
        const uint8_t *ex_end = ex + lfh->extra_len;

        while (true) {
            if (ex_end - ex < 4) {
                if ((ex_end - ex) > 0 && all_zero(ex, size_t(ex_end - ex)));
                break;
            }
            uint16_t tag = ex[0] | (uint16_t(ex[1]) << 8);
            uint16_t xlen = ex[2] | (uint16_t(ex[3]) << 8);
            ex += 4;
            if (xlen > size_t(ex_end - ex)) {
                if (all_zero(ex, size_t(ex_end - ex)));
                break;
            }

            // zip64 extra field
            if (tag == 0x0001) {
                const uint8_t *q = ex;
                if (lfh->uncomp_size == 0xFFFFFFFFu && q + 8 <= ex + xlen) {
                    e.uncomp_size = read_u64(q);
                    q += 8;
                    e.zip64 = true;
                }
                if (lfh->comp_size == 0xFFFFFFFFu && q + 8 <= ex + xlen) {
                    e.comp_size = read_u64(q);
                    q += 8;
                    e.zip64 = true;
                }
            }

            ex += xlen;
            if (ex == ex_end) break;
        }
    }

    bool scan_from_local() {
        if (already_local_scan) return !entries.empty();
        already_local_scan = true;

        uint64_t off = 0;

        while (off + sizeof(LocalFileHeader) <= mm_.len()) {
            const auto *lfh = reinterpret_cast<const LocalFileHeader *>(mm_.data() + off);
            if (lfh->signature != SIG_LOCAL_FILE) break;
            if (!in_range(mm_, lfh, sizeof(LocalFileHeader))) break;

            // android don't support encrypt entry
            if (lfh->gp_flags & 1) return false;

            const uint64_t name_extra = uint64_t(lfh->name_len) + lfh->extra_len;
            const uint64_t data_off = off + sizeof(LocalFileHeader) + name_extra;
            if (data_off > mm_.len()) return false;

            Entry e{};
            if (lfh->name_len) e.name.assign(reinterpret_cast<const char *>(lfh->name()), lfh->name_len);

            e.flags = lfh->gp_flags;
            e.method = lfh->method;
            e.crc32 = lfh->crc32;
            e.lfh_offset = off;
            e.data_offset = data_off;

            e.comp_size = lfh->comp_size;
            e.uncomp_size = lfh->uncomp_size;

            parse_lfh_extra(lfh, e);

            uint64_t data_and_dd_advance = 0;
            const bool need_dd = ((lfh->gp_flags & GPBF_HAS_DATA_DESC) || e.comp_size == 0 || e.uncomp_size == 0);

            if (need_dd) {
                if (!parse_data_descriptor_after(data_off, e, data_and_dd_advance)) break;
            } else {
                data_and_dd_advance = e.comp_size;
            }

            auto it = map_.find(e.name);
            if (it != map_.end()) {
                auto &tgt = entries[it->second];
                if (need_dd) {
                    tgt.comp_size = e.comp_size;
                    tgt.uncomp_size = e.uncomp_size;
                    tgt.crc32 = e.crc32;
                    tgt.zip64 = (tgt.zip64 || e.zip64);
                }
                tgt.lfh_offset = e.lfh_offset;
                tgt.data_offset = e.data_offset;
                tgt.flags = e.flags;
                tgt.method = e.method;
            } else {
                entries.push_back(std::move(e));
                map_[entries.back().name] = entries.size() - 1;
            }

            off = data_off + data_and_dd_advance;
        }
        return !entries.empty();
    }

    bool parse_data_descriptor_after(uint64_t data_off, Entry &e, uint64_t &advance) const {
        if (!(e.method == COMP_STORE || e.method == COMP_DEFLATE)) return false;

        const uint8_t *const base = mm_.data();
        const uint8_t *const data = base + data_off;
        const uint8_t *const file_end = base + mm_.len();

        auto is_next_header = [&](const uint8_t *p) -> bool {
            if (p + 4 > file_end) return false;
            uint32_t s = *reinterpret_cast<const uint32_t *>(p);
            return (s == SIG_LOCAL_FILE || s == SIG_CFR || s == SIG_EOCD || s == SIG_ZIP64_LOC);
        };

        const size_t dd_body = e.zip64 ? (4 + 8 + 8) : (4 + 4 + 4);

        auto parse_and_validate = [&](const uint8_t *next) -> bool {
            if (next < data) return false;
            if (size_t(next - data) < dd_body) return false;

            const uint8_t *dd_start = next - dd_body;
            bool with_sig = false;

            if (dd_start >= data + 4) {
                uint32_t maybe = *reinterpret_cast<const uint32_t *>(dd_start - 4);
                if (maybe == SIG_DATA_DESC) {
                    with_sig = true;
                    dd_start -= 4;
                }
            }

            const uint8_t *q = dd_start + (with_sig ? 4 : 0);
            if (q + 4 > file_end) return false;
            uint32_t crc = *reinterpret_cast<const uint32_t *>(q);
            q += 4;

            uint64_t comp = 0, uncomp = 0;
            if (e.zip64) {
                if (q + 8 + 8 > file_end) return false;
                comp = read_u64(q);
                q += 8;
                uncomp = read_u64(q);
                q += 8;
            } else {
                if (q + 4 + 4 > file_end) return false;
                comp = *reinterpret_cast<const uint32_t *>(q);
                q += 4;
                uncomp = *reinterpret_cast<const uint32_t *>(q);
                q += 4;
            }

            const auto data_len = static_cast<uint64_t>((dd_start) - data);

            if (e.method == COMP_STORE) {
                if (!(comp == uncomp && comp == data_len)) return false;
            } else { // DEFLATE
                if (comp != data_len) return false;
                if (!with_sig) {
                    if (probe_deflate_) {
                        uint64_t out_len = 0;
                        if (!probe_deflate_(data, size_t(comp), out_len)) return false;
                        if (out_len != uncomp) return false;
                    }
                }
            }

            e.crc32 = crc;
            e.comp_size = comp;
            e.uncomp_size = uncomp;
            advance = data_len + (with_sig ? (dd_body + 4) : dd_body);
            return true;
        };

        const uint8_t *scan = data;
        while (scan + 4 <= file_end) {
            if (is_next_header(scan)) {
                if (parse_and_validate(scan)) return true;
            }
            ++scan;
        }

        return !entries.empty();
    }

private:
    DeflateProbe probe_deflate_ = nullptr;
    const MemMap &mm_;
    std::vector<Entry> entries;
    std::map<std::string, size_t> map_;
    bool already_local_scan = false;
};

} // namespace zip
