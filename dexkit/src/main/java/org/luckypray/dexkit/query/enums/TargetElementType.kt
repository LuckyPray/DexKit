package org.luckypray.dexkit.query.enums

import org.luckypray.dexkit.InnerTargetElementType

enum class TargetElementType(val value: Byte) {
    /**
     * [java.lang.annotation.ElementType.TYPE]
     */
    Type(InnerTargetElementType.Type),

    /**
     * [java.lang.annotation.ElementType.FIELD]
     */
    Field(InnerTargetElementType.Field),

    /**
     * [java.lang.annotation.ElementType.METHOD]
     */
    Method(InnerTargetElementType.Method),

    /**
     * [java.lang.annotation.ElementType.PARAMETER]
     */
    Parameter(InnerTargetElementType.Parameter),

    /**
     * [java.lang.annotation.ElementType.CONSTRUCTOR]
     */
    Constructor(InnerTargetElementType.Constructor),

    /**
     * [java.lang.annotation.ElementType.LOCAL_VARIABLE]
     */
    LocalVariable(InnerTargetElementType.LocalVariable),

    /**
     * [java.lang.annotation.ElementType.ANNOTATION_TYPE]
     */
    AnnotationType(InnerTargetElementType.AnnotationType),

    /**
     * [java.lang.annotation.ElementType.PACKAGE]
     */
    Package(InnerTargetElementType.Package),

    // for jdk 1.8

    /**
     * [java.lang.annotation.ElementType.TYPE_PARAMETER]
     */
    TypeParameter(InnerTargetElementType.TypeParameter),

    /**
     * [java.lang.annotation.ElementType.TYPE_USE]
     */
    TypeUse(InnerTargetElementType.TypeUse),
    //TODO add MODULE
    ;
}