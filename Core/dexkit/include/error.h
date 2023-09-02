#pragma once

#include <cstdint>

namespace dexkit {

enum class Error : uint32_t {
    SUCCESS,
    EXPORT_DEX_FP_NULL,
    FILE_NOT_FOUND,
    OPEN_ZIP_FILE_FAILED,
    OPEN_FILE_FAILED,
    ADD_DEX_AFTER_CROSS_BUILD,
};

} // namespace dexkit