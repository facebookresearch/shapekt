/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis

fun Shape.asListOfInt(): List<Int> {
    require(this is DimShape && this.dims.all { it is NumericDim })
    return this.dims.map { (it as NumericDim).value }
}

fun SType.isError(): Boolean {
    return this is ErrorDim || this is ErrorShape || this is ErrorUnknownClass
}

fun SType.isSymbolic(): Boolean {
    return this is SymbolicDim || this is SymbolicShape
}