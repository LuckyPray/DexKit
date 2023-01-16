#pragma once

#include <slicer/dex_ir.h>
#include "kmp.h"

namespace dexkit {

// region declared
bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str, int match_type);

bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx,
                                  const std::string &str, int match_type);

bool
FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx,
                                    const std::string &str, int match_type);

bool
FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str, int match_type);

bool
FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str, int match_type);
// endregion

bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str, int match_type) {
    if (ann_idx == dex::kNoIndex || annotation->type->orig_index == ann_idx) {
        // std::vector<AnnotationElement *>
        for (auto &element: annotation->elements) {
            // AnnotationElement->EncodedValue
            if (FindEncodedValueUsingString(element->value, ann_idx, str, match_type)) {
                return true;
            }
        }
    }
    return false;
}

bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx,
                                  const std::string &str, int match_type) {
    // std::vector<Annotation *>
    for (auto &annotation: annotations->annotations) {
        if (FindAnnotationUsingString(annotation, ann_idx, str, match_type)) {
            return true;
        }
    }
    return false;
}

bool
FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx,
                                    const std::string &str, int match_type) {
    // std::vector<AnnotationSet *>
    for (auto &annotationSet: annotationSetRefList->annotations) {
        if (FindAnnotationSetUsingString(annotationSet, ann_idx, str, match_type)) {
            return true;
        }
    }
    return false;
}

bool
FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str, int match_type) {
    // std::vector<EncodedValue *>
    for (auto &element: encodedArray->values) {
        if (FindEncodedValueUsingString(element, ann_idx, str, match_type)) {
            return true;
        }
    }
    return false;
}

bool
FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str, int match_type) {
    switch (encodedValue->type) {
        case dex::kEncodedString: {
            if (match_type == mFull) {
                return encodedValue->u.string_value->c_str() == str;
            }
            uint8_t flag = 0;
            if (match_type == mSimilarRegex) {
                if (str[0] == '^') {
                    flag |= 1;
                }
                if (str.back() == '$') {
                    flag |= 2;
                }
            }
            std::string_view real_str = str;
            if (flag) {
                real_str = real_str.substr(1, real_str.size() - 2);
            }
            if (kmp::FindIndex(encodedValue->u.string_value->c_str(), real_str) != -1) {
                return true;
            }
            break;
        }
        case dex::kEncodedAnnotation: {
            if (FindAnnotationUsingString(encodedValue->u.annotation_value, ann_idx, str, match_type)) {
                return true;
            }
            break;
        }
        case dex::kEncodedArray: {
            if (FindEncodedArrayUsingString(encodedValue->u.array_value, ann_idx, str, match_type)) {
                return true;
            }
            break;
        }
        default:
            break;
    }
    return false;
}

} // namespace dexkit