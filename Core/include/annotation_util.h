#pragma once

#include <slicer/dex_ir.h>
#include "kmp.h"

namespace dexkit {

// region declared
bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str);
bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx, const std::string &str);
bool FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx, const std::string &str);
bool FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str);
bool FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str);
// endregion

bool FindAnnotationUsingString(ir::Annotation *annotation, dex::u4 ann_idx, const std::string &str) {
    if (ann_idx == dex::kNoIndex || annotation->type->orig_index == ann_idx) {
        // std::vector<AnnotationElement *>
        for (auto &element : annotation->elements) {
            // AnnotationElement->EncodedValue
            if (FindEncodedValueUsingString(element->value, ann_idx, str)) {
                return true;
            }
        }
    }
    return false;
}

bool FindAnnotationSetUsingString(ir::AnnotationSet *annotations, dex::u4 ann_idx, const std::string &str) {
    // std::vector<Annotation *>
    for (auto &annotation : annotations->annotations) {
        if (FindAnnotationUsingString(annotation, ann_idx, str)) {
            return true;
        }
    }
    return false;
}

bool FindAnnotationSetRefListUsingString(ir::AnnotationSetRefList *annotationSetRefList, dex::u4 ann_idx, const std::string &str) {
    // std::vector<AnnotationSet *>
    for (auto &annotationSet : annotationSetRefList->annotations) {
        if (FindAnnotationSetUsingString(annotationSet, ann_idx, str)) {
            return true;
        }
    }
    return false;
}

bool FindEncodedArrayUsingString(ir::EncodedArray *encodedArray, dex::u4 ann_idx, const std::string &str) {
    // std::vector<EncodedValue *>
    for (auto &element : encodedArray->values) {
        if (FindEncodedValueUsingString(element, ann_idx, str)) {
            return true;
        }
    }
    return false;
}

bool FindEncodedValueUsingString(ir::EncodedValue *encodedValue, dex::u4 ann_idx, const std::string &str) {
    switch (encodedValue->type) {
        case dex::kEncodedString: {
            if (kmp::FindIndex(encodedValue->u.string_value->c_str(), str) != -1) {
                return true;
            }
            break;
        }
        case dex::kEncodedAnnotation: {
            if (FindAnnotationUsingString(encodedValue->u.annotation_value, ann_idx, str)) {
                return true;
            }
            break;
        }
        case dex::kEncodedArray: {
            if (FindEncodedArrayUsingString(encodedValue->u.array_value, ann_idx, str)) {
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