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

@ShapeFunction
fun flatten(s: Shape, numAxesToKeep: Dim): Shape? {
    when (val error = getErrorForArguments("flatten", s, numAxesToKeep)) {
        is STypeException -> return ErrorShape(error)
    }
    return when {
        containsIrreducibleArguments(s, numAxesToKeep) -> null
        s is DimShape && numAxesToKeep is NumericDim -> {
            val n = numAxesToKeep.value
            if (n < 0 || n > s.rank)
                ErrorShape(STypeFailure("Invalid arguments to flatten: $s and $n"))
            else {
                val prefix = s.dims.subList(0, n)
                val suffix = s.dims.subList(n, s.rank).let {
                    if (it.isNotEmpty()) it.reduce { x, y -> times(x, y) ?: WildcardDim.DEFAULT }
                    else null
                }
                DimShape(if (suffix != null) prefix + suffix else prefix)
            }
        }
        s is WildcardShape -> WildcardShape.DEFAULT
        else -> null
    }
}