package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerTargetElementType

enum class TargetElementType(val value: Byte) {
    Type(InnerTargetElementType.Type),
    Field(InnerTargetElementType.Field),
    Method(InnerTargetElementType.Method),
    Parameter(InnerTargetElementType.Parameter),
    Constructor(InnerTargetElementType.Constructor),
    LocalVariable(InnerTargetElementType.LocalVariable),
    AnnotationType(InnerTargetElementType.AnnotationType),
    Package(InnerTargetElementType.Package),
    // for jdk 1.8
    TypeParameter(InnerTargetElementType.TypeParameter),
    TypeUse(InnerTargetElementType.TypeUse),
    ;
}