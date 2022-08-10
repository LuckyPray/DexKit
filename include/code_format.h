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

// Converts a type descriptor to human-readable "dotted" form.  For
// example, "Ljava/lang/String;" becomes "java.lang.String", and
// "[I" becomes "int[]".
static std::string DescriptorToDecl(const char *descriptor) {
    std::stringstream ss;

    int array_dimensions = 0;
    while (*descriptor == '[') {
        ++array_dimensions;
        ++descriptor;
    }

    if (*descriptor == 'L') {
        for (++descriptor; *descriptor != ';'; ++descriptor) {
            ss << (*descriptor == '/' ? '.' : *descriptor);
        }
    } else {
        ss << PrimitiveTypeName(*descriptor);
    }

    // add the array brackets
    for (int i = 0; i < array_dimensions; ++i) {
        ss << "[]";
    }

    return ss.str();
}

static std::vector<std::string> ExtractParamDescriptors(const std::string& descriptors) {
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
ExtractMethodDescriptor(std::string method_descriptor) {
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

// Converts a type descriptor to a single "shorty" char
// (ex. "LFoo;" and "[[I" become 'L', "I" stays 'I')
static char DescriptorToShorty(const char *descriptor) {
    // skip array dimensions
    int array_dimensions = 0;
    while (*descriptor == '[') {
        ++array_dimensions;
        ++descriptor;
    }

    char short_descriptor = *descriptor;
    if (short_descriptor == 'L') {
        // skip the full class name
        for (; *descriptor && *descriptor != ';'; ++descriptor);
    }

    return array_dimensions > 0 ? 'L' : short_descriptor;
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
    if (type.rfind("int") == 0) {
        desc << 'I';
    } else if (type.rfind("long") == 0) {
        desc << 'J';
    } else if (type.rfind("float") == 0) {
        desc << 'F';
    } else if (type.rfind("double") == 0) {
        desc << 'D';
    } else if (type.rfind("char") == 0) {
        desc << 'C';
    } else if (type.rfind("byte") == 0) {
        desc << 'B';
    } else if (type.rfind("short") == 0) {
        desc << 'S';
    } else if (type.rfind("boolean") == 0) {
        desc << 'Z';
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