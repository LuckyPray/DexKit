#pragma once

#include <sstream>
#include <vector>
#include <string_view>

namespace dexkit {

// Returns the human-readable name for a primitive type
constexpr std::string_view PrimitiveTypeName(char type_char) {
    switch (type_char) {
        case 'B':
            return "byte";
        case 'C':
            return "char";
        case 'D':
            return "double";
        case 'F':
            return "float";
        case 'I':
            return "int";
        case 'J':
            return "long";
        case 'S':
            return "short";
        case 'V':
            return "void";
        case 'Z':
            return "boolean";
        default:
            return "Unknown";
    }
}

static std::vector<std::string> ExtractParamDescriptors(const std::string &descriptors) {
    std::vector<std::string> params;
    const char *p = descriptors.c_str();
    std::stringstream ss;
    while (*p != '\0') {
        switch (*p) {
            case '[': {
                ss << *p++;
                break;
            }
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z': {
                ss << *p++;
                params.emplace_back(ss.str());
                ss.str("");
                break;
            }
            case 'L': {
                do {
                    ss << *p;
                } while (*p++ != ';');
                params.emplace_back(ss.str());
                ss.str("");
                break;
            }
        }
    }
    return params;
}

static std::tuple<std::string, std::string, std::string, std::vector<std::string>>
ExtractMethodDescriptor(std::string &method_descriptor) {
    std::string class_desc, method_name, return_desc;
    size_t pos = method_descriptor.find("->");
    if (pos != std::string::npos) {
        class_desc = method_descriptor.substr(0, pos);
    }
    size_t pos1 = method_descriptor.find('(');
    if (pos1 != std::string::npos) {
        method_name = method_descriptor.substr(pos + 2, pos1 - pos - 2);
    }
    size_t pos2 = method_descriptor.find(')');
    if (pos2 != std::string::npos) {
        return_desc = method_descriptor.substr(pos2 + 1, method_descriptor.size() - pos2 - 1);
    }
    std::vector<std::string> param_descs = ExtractParamDescriptors(method_descriptor.substr(pos1 + 1, pos2 - pos1 - 1));
    return std::make_tuple(class_desc, method_name, return_desc, param_descs);
}

// Converts Declare java type to a type descriptor
// ex. "java.lang.String" becomes "Ljava/lang/String;"
// ex. "int[]" becomes "[I"
static std::string DeclToDescriptor(const std::string &type) {
    std::stringstream desc;
    auto arr_dimensions = std::count(type.begin(), type.end(), '[');
    for (int i = 0; i < arr_dimensions; ++i) {
        desc << '[';
    }
    if (type.find("int") == 0) {
        desc << 'I';
    } else if (type.find("long") == 0) {
        desc << 'J';
    } else if (type.find("float") == 0) {
        desc << 'F';
    } else if (type.find("double") == 0) {
        desc << 'D';
    } else if (type.find("char") == 0) {
        desc << 'C';
    } else if (type.find("byte") == 0) {
        desc << 'B';
    } else if (type.find("short") == 0) {
        desc << 'S';
    } else if (type.find("boolean") == 0) {
        desc << 'Z';
    } else if (type.find("void") == 0) {
        desc << 'V';
    } else {
        desc << 'L';
        for (auto &c: type) {
            desc << (c == '.' ? '/' : c);
        }
        desc << ';';
    }
    return desc.str();
}

static std::string DeclToMatchDescriptor(const std::string &type) {
    if (type.empty()) {
        return "";
    }
    return DeclToDescriptor(type);
}

// Converts parameter and return types to match shorty string, '*' match any type
// ex. "void", {"int", "int[]", "*"} -> "VIL"
// ps: all reference types are represented by a single 'L' character.
static std::string DescriptorToMatchShorty(const std::string &return_type, const std::vector<std::string> &parameter_types) {
    std::stringstream ss;
    if (return_type.empty()) {
        ss << '*';
    } else {
        ss << dex::DescriptorToShorty(return_type.c_str());
    }
    for (auto &parameter_type: parameter_types) {
        if (parameter_type.empty()) {
            ss << '*';
            continue;
        }
        ss << dex::DescriptorToShorty(parameter_type.c_str());
    }
    return ss.str();
}

// matches a match_shorty string against a method proto shorty string
// ex. ShortyDescriptorMatch("**IL", "ILIL") -> true
static bool ShortyDescriptorMatch(const std::string &match_shorty, const std::string_view &method_shorty) {
    if (match_shorty.size() != method_shorty.size()) {
        return false;
    }
    for (int i = 0; i < match_shorty.size(); ++i) {
        if (match_shorty[i] == '*') continue;
        if (match_shorty[i] != method_shorty[i]) {
            return false;
        }
    }
    return true;
}

}