#pragma once

#include <slicer/dex_ir.h>
#include "kmp.h"

namespace dexkit {

// region declared
bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str,
                               bool advanced_match);

bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx,
                                  const std::string &str, bool advanced_match);

bool
FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx,
                                    const std::string &str, bool advanced_match);

bool
FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str,
                            bool advanced_match);

bool
FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str,
                            bool advanced_match);
// endregion

bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str,
                               bool advanced_match) {
    if (ann_idx == dex::kNoIndex || annotation->type->orig_index == ann_idx) {
        // std::vector<AnnotationElement *>
        for (auto &element: annotation->elements) {
            // AnnotationElement->EncodedValue
            if (FindEncodedValueUsingString(element->value, ann_idx, str, advanced_match)) {
                return true;
            }
        }
    }
    return false;
}

bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx,
                                  const std::string &str, bool advanced_match) {
    // std::vector<Annotation *>
    for (auto &annotation: annotations->annotations) {
        if (FindAnnotationUsingString(annotation, ann_idx, str, advanced_match)) {
            return true;
        }
    }
    return false;
}

bool
FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx,
                                    const std::string &str, bool advanced_match) {
    // std::vector<AnnotationSet *>
    for (auto &annotationSet: annotationSetRefList->annotations) {
        if (FindAnnotationSetUsingString(annotationSet, ann_idx, str, advanced_match)) {
            return true;
        }
    }
    return false;
}

bool
FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str,
                            bool advanced_match) {
    // std::vector<EncodedValue *>
    for (auto &element: encodedArray->values) {
        if (FindEncodedValueUsingString(element, ann_idx, str, advanced_match)) {
            return true;
        }
    }
    return false;
}

bool
FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str,
                            bool advanced_match) {
    switch (encodedValue->type) {
        case dex::kEncodedString: {
            uint8_t flag = 0;

            if (advanced_match) {
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
            if (FindAnnotationUsingString(encodedValue->u.annotation_value, ann_idx, str,
                                          advanced_match)) {
                return true;
            }
            break;
        }
        case dex::kEncodedArray: {
            if (FindEncodedArrayUsingString(encodedValue->u.array_value, ann_idx, str,
                                            advanced_match)) {
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