/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeException
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.extensions.annotations.ShapeFunction
import kotlin.math.min

@ShapeFunction
fun slice(s: Shape, start: Dim, end: Dim, axis: Dim): Shape? {
    when (val error = getErrorForArguments("slice", start, end, axis)) {
        is STypeException -> return ErrorShape(error)
    }
    return when {
        containsIrreducibleArguments(s, start, end, axis) -> null
        s is DimShape && start is NumericDim && end is NumericDim && axis is NumericDim -> {
            when {
                (axis.value < 0 || axis.value >= s.rank) ->
                    ErrorShape(STypeFailure("slice: axis $axis invalid for shape $s of rank ${s.rank}"))
                (end.value < start.value) ->
                    ErrorShape(STypeFailure("slice: end axis $end should be greater than or equal to start axis $start"))
                else -> {
                    val axisSize = (s[axis.value] as? NumericDim)?.value ?: return null
                    val endIdx = min(axisSize, end.value)
                    val sliceSize = endIdx - start.value
                    DimShape(s.dims.mapIndexed { i, dim -> if (i == axis.value) NumericDim(sliceSize) else dim })
                }
            }
        }
        s is WildcardShape -> WildcardShape.DEFAULT
        else -> null
    }
}