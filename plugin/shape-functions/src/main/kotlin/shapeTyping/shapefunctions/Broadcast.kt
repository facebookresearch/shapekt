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
import shapeTyping.analysis.exceptions.STypeStrictModeException
import shapeTyping.analysis.exceptions.UnsupportedSTypesException
import shapeTyping.extensions.annotations.ShapeFunction

enum class Status {
    SUCCESS,
    FAILURE,
    REDUCIBLE_WITHOUT_GUARANTEE,
    NOT_REDUCIBLE
}

@ShapeFunction
fun broadcast(a: Shape, b: Shape): Shape? {
    when (val error = getErrorForArguments("broadcast", a, b)) {
        is STypeException -> return ErrorShape(error)
    }
    return when {
        (a is WildcardShape || b is WildcardShape) -> WildcardShape(STypeStrictModeException(
            "broadcast: Cannot guarantee dimension matching for wildcard shapes"
        ))
        (a == b) -> a
        containsIrreducibleArguments(a, b) -> null
        (a is DimShape && b is DimShape) -> broadcastDimShapes(a, b)
        else -> ErrorShape(UnsupportedSTypesException("broadcast", a, b))
    }
}

fun broadcastDim(a: Dim, b: Dim): Pair<Status, Dim?> {
    return when {
        (a == NumericDim(1)) -> Pair(Status.SUCCESS, b)
        (b == NumericDim(1)) -> Pair(Status.SUCCESS, a)
        (a is WildcardDim) -> if (b is NumericDim && b != NumericDim(1)) Pair(Status.REDUCIBLE_WITHOUT_GUARANTEE, b)
        else Pair(Status.REDUCIBLE_WITHOUT_GUARANTEE, a)
        (b is WildcardDim) -> if (a is NumericDim && a != NumericDim(1)) Pair(Status.REDUCIBLE_WITHOUT_GUARANTEE, a)
        else Pair(Status.REDUCIBLE_WITHOUT_GUARANTEE, b)
        (a == b) -> Pair(Status.SUCCESS, a)
        (a is SymbolicDim || b is SymbolicDim) -> return Pair(Status.NOT_REDUCIBLE, null)
        else -> Pair(Status.FAILURE, null)
    }
}

fun broadcastDimShapes(a: DimShape, b: DimShape): Shape? {
    if (a.dims.isEmpty()) return b
    if (b.dims.isEmpty()) return a

    var rdiff = a.rank - b.rank

    var broadcastedDims = a.dims.toMutableList()
    if (rdiff < 0) {
        broadcastedDims = (b.dims.take(-rdiff) + broadcastedDims).toMutableList()
        rdiff = 0
    }
    var collectedStatus: Status? = null
    for (j in rdiff..broadcastedDims.lastIndex) {
        val sd = broadcastedDims[j]
        val td = b[j - rdiff]
        // if we have a symbolic dim, can't reduce
        val (status, broadcastedDim) = broadcastDim(sd, td)
        when (status) {
            Status.FAILURE ->
                return ErrorShape(STypeFailure("Cannot broadcast $a and $b with incompatible dims $sd and $td"))
            Status.NOT_REDUCIBLE ->
                collectedStatus = Status.NOT_REDUCIBLE
            Status.REDUCIBLE_WITHOUT_GUARANTEE -> {
                if (collectedStatus == null) {
                    collectedStatus = Status.REDUCIBLE_WITHOUT_GUARANTEE
                }
                broadcastedDims[j] = broadcastedDim!!
            }
            else -> broadcastedDims[j] = broadcastedDim!!
        }
    }
    return when (collectedStatus) {
        Status.NOT_REDUCIBLE -> null
        Status.REDUCIBLE_WITHOUT_GUARANTEE -> DimShape(broadcastedDims,
            STypeStrictModeException("broadcast: cannot guarantee matching of wildcard dims"))
        else -> DimShape(broadcastedDims)
    }
}