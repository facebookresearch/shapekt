/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.*
import shapeTyping.extensions.annotations.ShapeFunction

@ShapeFunction
fun concatOnAxis(a: Shape, b: Shape, axis: Dim): Shape? {
    getErrorForArguments("concatOnAxis", a, b, axis)?.let { return ErrorShape(it) }
    return when {
        (a is DimShape && b is DimShape && axis is NumericDim) -> concatDimShapes(a, b, axis.value)
        (a is WildcardShape || b is WildcardShape) -> WildcardShape(
            STypeStrictModeException(
            "concatOnAxis: Cannot guarantee dimension matching for wildcard shapes"
        ))
        else -> null
    }
}

private fun concatDimShapes(a: DimShape, b: DimShape, axis: Int): Shape? {
    return when {
        a.rank != b.rank -> ErrorShape(STypeFailure("concatOnAxis: Can not concat shapes $a and $b due to mismatched ranks."))
        axis < 0 || axis >= a.rank -> ErrorShape(STypeFailure("concatOnAxis: Axis $axis is out of range for shapes $a and $b"))
        else -> {
            val newDims = a.dims.zip(b.dims).mapIndexed { i, pair ->
                if (i == axis)
                    plus(pair.first, pair.second) ?: WildcardDim.DEFAULT
                else
                    dimIntersect(pair.first, pair.second, "concatOnAxis: ")
            }
            newDims.forEachIndexed { i, d ->
                if (d is ErrorDim) return ErrorShape(STypeFailure(
                    (d.error?.message ?: "Dim mismatch between $a, $b") + " at pos $axis"
                ))
            }
            DimShape(newDims)
        }
    }
}