/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.DimShape
import shapeTyping.analysis.ErrorShape
import shapeTyping.analysis.Shape
import shapeTyping.analysis.WildcardShape
import shapeTyping.analysis.exceptions.STypeException
import shapeTyping.analysis.exceptions.STypeStrictModeException
import shapeTyping.extensions.annotations.ShapeFunction

@ShapeFunction
fun concat(a: Shape, b: Shape): Shape? {
    when (val error = getErrorForArguments("concat", a, b)) {
        is STypeException -> return ErrorShape(error)
    }
    return when {
            (a is DimShape && b is DimShape) -> DimShape(a.dims + b.dims)
            (a is WildcardShape || b is WildcardShape) -> WildcardShape(STypeStrictModeException(
                "concat: Cannot guarantee dimension matching for wildcard shapes"
            ))
            else -> null
    }
}