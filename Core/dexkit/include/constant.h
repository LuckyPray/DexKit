#pragma once

#include <optional>
#include <string_view>
#include <vector>

#include "slicer/dex_ir.h"

// null_param is used to match any param
const static std::optional<std::vector<std::string>> null_param = std::nullopt;
const static std::vector<std::string> empty_param;

// init cached flag
constexpr dex::u4 fHeader = 0x0001;
constexpr dex::u4 fString = 0x0002;
constexpr dex::u4 fType = 0x0004;
constexpr dex::u4 fProto = 0x0008;
constexpr dex::u4 fField = 0x0010;
constexpr dex::u4 fMethod = 0x0020;
constexpr dex::u4 fAnnotation = 0x0040;
constexpr dex::u4 fOpCodeSeq = 0x1000;
constexpr dex::u4 fDefault = fHeader | fString | fType | fProto | fMethod;

// used field flag
constexpr dex::u4 fGetting = 0x1;
constexpr dex::u4 fSetting = 0x2;
constexpr dex::u4 fUsing = fGetting | fSetting;

// match type
// mFull           : full string match, eg.
//     full_match(search = "abc", target = "abc") = true
//     full_match(search = "abc", target = "abcd") = false
constexpr dex::u4 mFull = 0;
// mContains       : contains string match, eg.
//     contains_match(search = "abc", target = "abcd") = true
//     contains_match(search = "abc", target = "abc") = true
//     contains_match(search = "abc", target = "ab") = false
constexpr dex::u4 mContains = 1;
// mSimilarRegex   : similar regex matches, only support: '^', '$' eg.
//     similar_regex_match(search = "abc", target = "abc") == true
//     similar_regex_match(search = "^abc", target = "abc") == true
//     similar_regex_match(search = "abc$", target = "bc") == true
//     similar_regex_match(search = "^abc$", target = "abc") == true
//     similar_regex_match(search = "^abc$", target = "abcd") == false
constexpr dex::u4 mSimilarRegex = 2;
