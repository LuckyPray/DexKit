/*
 * DexKit - An high-performance runtime parsing library for dex
 * implemented in C++
 * Copyright (C) 2022-2023 LuckyPray
 * https://github.com/LuckyPray/DexKit
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 * <https://github.com/LuckyPray/DexKit/blob/master/LICENSE>.
 */

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