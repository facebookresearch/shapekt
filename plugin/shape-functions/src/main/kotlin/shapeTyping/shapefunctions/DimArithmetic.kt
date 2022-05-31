/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeException
import shapeTyping.extensions.annotations.ShapeFunction

@ShapeFunction
fun plus(a: Dim, b: Dim): Dim? {
    when (val error = getErrorForArguments("plus", a, b)) {
        is STypeException -> return ErrorDim(error)
    }
    return when {
        (a is NumericDim && b is NumericDim) -> NumericDim(a.value + b.value)
        (a is WildcardDim || b is WildcardDim) -> WildcardDim()
        else -> null
    }
}

@ShapeFunction
fun times(a: Dim, b: Dim): Dim? {
    when (val error = getErrorForArguments("plus", a, b)) {
        is STypeException -> return ErrorDim(error)
    }
    return when {
        (a is NumericDim && b is NumericDim) -> NumericDim(a.value * b.value)
        (a is WildcardDim || b is WildcardDim) -> WildcardDim()
        else -> null
    }
}