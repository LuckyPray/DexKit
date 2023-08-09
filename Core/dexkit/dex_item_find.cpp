#include "dex_item.h"

namespace dexkit {

std::vector<ClassBean>
DexItem::FindClass(const schema::FindClass *query) {
    return {};
}

std::vector<MethodBean>
DexItem::FindMethod(const schema::FindMethod *query) {
    return {};
}

std::vector<FieldBean>
DexItem::FindField(const schema::FindField *query) {
    return {};
}

}