#pragma once

#include <sstream>
#include <vector>
#include <string_view>
#include "slicer/reader.h"

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

static bool CheckIsDescriptor(const std::string &type) {
    if (type.empty()) {
        return false;
    }
    if (type.find(']') != std::string::npos) {
        return false;
    }
    if (type[0] == '[') {
        return true;
    }
    if (type.front() == 'L' && type.back() == ';') {
        return true;
    }
    if (type.size() == 1) {
        switch (type.front()) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z':
                return true;
            default:
                return false;
        }
    }
    return false;
}

// Converts Declare java type to a type descriptor
// ex. "java.lang.String" becomes "Ljava/lang/String;"
// ex. "int[]" becomes "[I"
static std::string DeclToDescriptor(const std::string &type) {
    if (CheckIsDescriptor(type)) {
        return type;
    }
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

static std::string GetClassDescriptor(std::string class_name) {
    std::replace(class_name.begin(), class_name.end(), '.', '/');
    if (class_name.length() > 0 && class_name[0] != 'L') {
        class_name = "L" + class_name + ";";
    }
    return class_name;
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
static std::string
DescriptorToMatchShorty(const std::string &return_type, const std::vector<std::string> &parameter_types) {
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

static std::tuple<std::string, std::string, std::string, std::vector<std::string>>
ExtractMethodDescriptor(const std::string &input_method_descriptor,
                        const std::string &input_method_class,
                        const std::string &input_method_name,
                        const std::string &input_method_return_type,
                        const std::optional<std::vector<std::string>> &input_method_param_types) {
    std::string declared_class_descriptor, method_name, return_type_descriptor;
    std::vector<std::string> param_descs;
    if (!input_method_descriptor.empty()) {
        size_t pos = input_method_descriptor.find("->");
        if (pos != std::string::npos) {
            declared_class_descriptor = input_method_descriptor.substr(0, pos);
        }
        size_t pos1 = input_method_descriptor.find('(');
        if (pos1 != std::string::npos) {
            method_name = input_method_descriptor.substr(pos + 2, pos1 - pos - 2);
        }
        size_t pos2 = input_method_descriptor.find(')');
        if (pos2 != std::string::npos) {
            return_type_descriptor = input_method_descriptor.substr(pos2 + 1,
                                                                     input_method_descriptor.size() - pos2 - 1);
        }
        param_descs = ExtractParamDescriptors(input_method_descriptor.substr(pos1 + 1, pos2 - pos1 - 1));
    } else {
        declared_class_descriptor = GetClassDescriptor(input_method_class);
        method_name = input_method_name;
        return_type_descriptor = DeclToMatchDescriptor(input_method_return_type);
        for (auto &param_decl: input_method_param_types.value_or(std::vector<std::string>())) {
            param_descs.emplace_back(DeclToMatchDescriptor(param_decl));
        }
    }
    return std::make_tuple(declared_class_descriptor, method_name, return_type_descriptor, param_descs);
}

static std::tuple<std::string, std::string, std::string>
ExtractFieldDescriptor(const std::string &input_field_descriptor,
                       const std::string &input_field_class,
                       const std::string &input_field_name,
                       const std::string &input_field_type) {
    std::string declared_class_descriptor, field_name, field_type_descriptor;
    if (!input_field_descriptor.empty()) {
        size_t pos = input_field_descriptor.find("->");
        if (pos != std::string::npos) {
            declared_class_descriptor = input_field_descriptor.substr(0, pos);
        }
        size_t pos1 = input_field_descriptor.find(':');
        if (pos1 != std::string::npos) {
            field_name = input_field_descriptor.substr(pos + 2, pos1 - pos - 2);
        }
        field_type_descriptor = input_field_descriptor.substr(pos1 + 1, input_field_descriptor.size() - pos1 - 1);
    } else {
        declared_class_descriptor = GetClassDescriptor(input_field_class);
        field_name = input_field_name;
        field_type_descriptor = DeclToMatchDescriptor(input_field_type);
    }
    return std::make_tuple(declared_class_descriptor, field_name, field_type_descriptor);
}

} // namespace dexkit