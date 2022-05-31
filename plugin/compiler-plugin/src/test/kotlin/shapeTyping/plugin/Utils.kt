/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import shapeTyping.analysis.*
import shapeTyping.annotations.SType
import java.io.File

fun getResource(resourceName: String): File =
        File(Thread.currentThread().contextClassLoader.getResource(resourceName)!!.path)

@SType("S: Shape")
class Tensor(val shape: @SType("S") RuntimeShape = RuntimeShape(listOf()))

@SType("S: Shape")
class RuntimeShape(val dims : List<Int>) {
    constructor(vararg dims : @SType("S") Int) : this(dims.toList())
}

fun basicShapeSymbol(symbol: String, bound: Shape = WildcardShape.DEFAULT): SymbolicShape = SymbolicShape(
    symbol,
    ShapeDeclarationInfo(bound, false, 0)
)

fun basicDimSymbol(symbol: String, bound: Dim = WildcardDim.DEFAULT): SymbolicDim = SymbolicDim(
    symbol,
    DimDeclarationInfo(bound, false, 0)
)