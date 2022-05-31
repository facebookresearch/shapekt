/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis.utils

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeCompilerException

fun SType.serialize(): String {
    fun serializeList(sTypes: List<SType>): String = sTypes.map { it.serialize() }.joinToString(",")
    return when (this) {
        is STypeTuple -> serializeList(this.types)
        is SymbolicDim-> this.symbol
        is SymbolicShape -> this.symbol
        is ShapeFunctionCall -> "${this.name}(${serializeList(this.args)})"
        is DimShape -> "[${serializeList(this.dims)}]"
        is WildcardDim, is WildcardShape, is NumericDim -> this.toString()
        is ErrorDim, is ErrorShape, is ErrorUnknownClass -> throw STypeCompilerException("")
    }
}