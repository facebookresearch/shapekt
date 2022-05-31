/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.analysis.exceptions.STypeStrictModeException
import shapeTyping.analysis.utils.STypeSubtyper.isSubtypeOf

fun checkRankIfExists(s: Shape, expectedRank: Int, paramName: String): ErrorShape? =
    if (s is DimShape && s.rank != expectedRank)
        ErrorShape(STypeFailure("$paramName must have rank $expectedRank. Got ${s.rank}"))
    else null

fun checkMinimumRankIfExists(s: Shape, expectedRank: Int, paramName: String): ErrorShape? =
    if (s is DimShape && s.rank < expectedRank)
        ErrorShape(STypeFailure("$paramName must have rank at least $expectedRank. Got ${s.rank}"))
    else null

fun getErrorForArguments(fnName: String, vararg args: SType): STypeFailure? {
    args.forEachIndexed { i, tpe ->
        if (tpe.isError()) return STypeFailure(tpe.error?.toString() ?: "$fnName: Error in argument $i: $tpe")
    }
    return null
}

fun containsIrreducibleArguments(vararg args: SType): Boolean = args.any { it is ShapeFunctionCall || it.isSymbolic() }

fun dimIntersect(a: Dim, b: Dim, errorPrefix: String = ""): Dim = when {
    a == b -> if (a.error != null) a else b
    a.isSubtypeOf(b) -> a.withException(STypeStrictModeException("$errorPrefix$b may not be exactly equal to $a"))
    b.isSubtypeOf(a) -> b.withException(STypeStrictModeException("$errorPrefix$a may not be exactly equal to $b"))
    else -> {
        val (conflicting, bounds) = conflictingBounds(a, b)
        if (conflicting) ErrorDim(STypeFailure("${errorPrefix}Dims $a and $b cannot be equal"))
        else dimIntersect(bounds.first, bounds.second, errorPrefix)
    }
}

fun conflictingDims(a: Dim, b: Dim): Boolean = conflictingBounds(a, b).first


// Returns whether the dims have conflicting bounds, and the first non-symbolic bounds of each dim
private fun conflictingBounds(a: Dim, b: Dim): Pair<Boolean, Pair<Dim, Dim>> {
    var concreteBoundA = a
    var concreteBoundB = b
    while (concreteBoundA is SymbolicDim) {
        concreteBoundA = concreteBoundA.declarationInfo.upperBound
    }
    while (concreteBoundB is SymbolicDim) {
        concreteBoundB = concreteBoundB.declarationInfo.upperBound
    }
    val result = when {
        concreteBoundA is NumericDim && concreteBoundB is NumericDim -> concreteBoundA.value != concreteBoundB.value
        concreteBoundA is ErrorDim && concreteBoundB !is ErrorDim -> true
        concreteBoundB is ErrorDim && concreteBoundA !is ErrorDim -> true
        else -> false // At least one of the bounds is Any
    }
    return Pair(result, Pair(concreteBoundA, concreteBoundB))
}