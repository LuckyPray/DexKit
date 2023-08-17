#pragma once

#include <string_view>
#include <slicer/dex_bytecode.h>

namespace dexkit {

constexpr std::string_view GetOpcodeFormat(uint8_t opcode);
constexpr uint8_t GetOpcodeLen(dex::u1 opcode);
constexpr size_t GetBytecodeWidth(const dex::u2 *bytecode);

constexpr std::string_view op_names[] = {
#define INSTRUCTION_NAME(o, c, pname, ...) pname,

#include "slicer/dex_instruction_list.h"
        DEX_INSTRUCTION_LIST(INSTRUCTION_NAME)
#undef DEX_INSTRUCTION_LIST
#undef INSTRUCTION_NAME
};
static_assert(sizeof(op_names) / sizeof(std::string_view) == 256);

constexpr dex::InstructionFormat ins_formats[] = {
#define INSTRUCTION_NAME(o, c, pname, format, ...) dex::format,

#include "slicer/dex_instruction_list.h"
        DEX_INSTRUCTION_LIST(INSTRUCTION_NAME)
#undef DEX_INSTRUCTION_LIST
#undef INSTRUCTION_NAME
};
static_assert(sizeof(ins_formats) / sizeof(dex::InstructionFormat) == 256);

constexpr std::string_view GetOpcodeFormat(uint8_t opcode) {
    switch (ins_formats[opcode]) {
#define EMIT_INSTRUCTION_FORMAT_NAME(name) \
    case dex::k##name:                          \
        return  #name;
#include "slicer/dex_instruction_list.h"
        DEX_INSTRUCTION_FORMAT_LIST(EMIT_INSTRUCTION_FORMAT_NAME)
#undef EMIT_INSTRUCTION_FORMAT_NAME
#undef DEX_INSTRUCTION_FORMAT_LIST
#undef DEX_INSTRUCTION_LIST
    }
}

constexpr uint8_t GetOpcodeLen(dex::u1 opcode) {
    switch (ins_formats[opcode]) {
        case dex::k10x:
        case dex::k12x:
        case dex::k11n:
        case dex::k11x:
        case dex::k10t:
            return 1;
        case dex::k20t:
        case dex::k20bc:
        case dex::k21c:
        case dex::k22x:
        case dex::k21s:
        case dex::k21t:
        case dex::k21h:
        case dex::k23x:
        case dex::k22b:
        case dex::k22s:
        case dex::k22t:
        case dex::k22c:
        case dex::k22cs:
            return 2;
        case dex::k30t:
        case dex::k31t:
        case dex::k31c:
        case dex::k32x:
        case dex::k31i:
        case dex::k35c:
        case dex::k35ms:
        case dex::k35mi:
        case dex::k3rc:
        case dex::k3rms:
        case dex::k3rmi:
            return 3;
        case dex::k45cc:
        case dex::k4rcc:
            return 4;
        case dex::k51l:
            return 5;
    }
    abort();
}

constexpr size_t GetBytecodeWidth(const dex::u2 *bytecode) {
    if (*bytecode == dex::kPackedSwitchSignature) {
        return 4 + bytecode[1] * 2;
    } else if (*bytecode == dex::kSparseSwitchSignature) {
        return 2 + bytecode[1] * 4;
    } else if (*bytecode == dex::kArrayDataSignature) {
        dex::u2 elemWidth = bytecode[1];
        dex::u4 len = bytecode[2] | (((dex::u4) bytecode[3]) << 16);
        // The plus 1 is to round up for odd size and width.
        return 4 + (elemWidth * len + 1) / 2;
    }
    return GetOpcodeLen(*bytecode & 0xff);
}

}
