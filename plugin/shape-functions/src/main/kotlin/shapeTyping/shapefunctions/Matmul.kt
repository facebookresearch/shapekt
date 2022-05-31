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
import java.lang.Error

/* Matmul shape function written to be compatible with the DiffKt API. */
@ShapeFunction
fun matmul(a: Shape, b: Shape): Shape? {
    checkMinimumRankIfExists(a, 1, "a")?.let {return it }
    checkMinimumRankIfExists(b, 1, "b")?.let { return it }

    when (val error = getErrorForArguments("matmul", a, b)) {
        is STypeException -> return ErrorShape(error)
    }

    return when {
        containsIrreducibleArguments(a, b) -> null
        (a is DimShape && b is DimShape) -> matmulDimShapes(a, b)
        (a is WildcardShape && b is WildcardShape) -> DimShape(WildcardDim.DEFAULT, WildcardDim.DEFAULT).withException(
            STypeStrictModeException("matmul: Dimensions of Shape and Shape are not guaranteed to be compatible.")
        )
        (a is DimShape && b is WildcardShape) -> {
            val resultDims = if (a.rank == 1) listOf(NumericDim(1), WildcardDim.DEFAULT) else
                a.sliceToDimList(0, a.rank - 1) + WildcardDim.DEFAULT
            DimShape(resultDims).withException(
                STypeStrictModeException("matmul: Cannot guarantee compatible dimensions for $a and Shape.")
            )
        }
        (a is WildcardShape && b is DimShape) -> {
            val resultDims = if (b.rank == 1) listOf(WildcardDim.DEFAULT, NumericDim(1)) else
                b.sliceToDimList(0, b.rank - 2) + WildcardDim.DEFAULT + b[b.rank-1]
            DimShape(resultDims).withException(
                STypeStrictModeException("matmul: Cannot guarantee compatible dimensions for Shape and $b.")
            )
        }
        else -> ErrorShape(UnsupportedSTypesException("matmul", a, b))
    }
}

private fun matmulDimShapes(a: DimShape, b: DimShape): Shape {
    val paddedA = if (a.rank == 1) DimShape(NumericDim(1), a[0]) else a
    val paddedB = if (b.rank == 1) DimShape(b[0], NumericDim(1)) else b
    val resultRank = paddedA.rank.coerceAtLeast(paddedB.rank)

    val (m, k1) = Pair(paddedA[paddedA.rank - 2], paddedA[paddedA.rank - 1])
    val (k2, n) = Pair(paddedB[paddedB.rank - 2], paddedB[paddedB.rank - 1])

    val mismatchException: STypeStrictModeException? = when {
        conflictingDims(k1, k2) ->
            return ErrorShape(STypeFailure("Shapes $a and $b have incompatible inner dimensions for matmul: $k1 and $k2"))
        k1 != k2 -> STypeStrictModeException("Dimensions $k1, $k2 not guaranteed compatible for matmul")
        k1 is WildcardDim || k2 is WildcardDim -> STypeStrictModeException("Dimensions $k1, $k2 not guaranteed compatible for matmul")
        else -> null
    }

    fun batchDimMatch(d1: Dim, d2: Dim): Dim =
        when {
            d1 == d2 -> d1

            // Propagate errors:
            d1 is ErrorDim -> d1
            d2 is ErrorDim -> d2

            d1 == NumericDim(1) -> d2
            d2 == NumericDim(1) -> d1

            // Resolve cases where dim classes match:
            d1 is NumericDim && d2 is NumericDim -> // Equality case already checked above!
                ErrorDim(STypeFailure("matmul: Incompatible batching dimensions $d1 and $d2"))
            d1 is WildcardDim && d2 is WildcardDim -> WildcardDim((STypeStrictModeException("matmul: Wildcard dimensions not guaranteed to be compatible")))
            d1 is SymbolicDim && d2 is SymbolicDim -> if (d1 == d2) d1 else ErrorDim(STypeFailure("matmul: Incompatible batching dimensions $d1 and $d2"))

            // Resolve differing dim classes with warning, prioritizing Numeric > Symbolic > Wildcard
            d1 is NumericDim -> d1.withException(STypeStrictModeException("matmul: Dimension $d1 may not be compatible with $d2"))
            d2 is NumericDim -> d2.withException(STypeStrictModeException("matmul: Dimension $d1 may not be compatible with $d2"))
            d1 is SymbolicDim -> d1.withException(STypeStrictModeException("matmul: Dimension $d1 may not be compatible with $d2"))
            d2 is SymbolicDim -> d2.withException(STypeStrictModeException("matmul: Dimension $d1 may not be compatible with $d2"))

            else -> throw STypeCompilerException("Illegal state for matmul analysis!")
        }

    val offsetA = resultRank - paddedA.rank
    val offsetB = resultRank - paddedB.rank
    val batchDims = (0 until resultRank - 2).map { i ->
        val aDim = if (i < offsetA) NumericDim(1) else paddedA[i - offsetA]
        val bDim = if (i < offsetB) NumericDim(1) else paddedB[i - offsetB]
        batchDimMatch(aDim, bDim)
    }
    return batchDims.firstOrNull { it.isError() }?.let { ErrorShape(it.error!!) } ?:
        DimShape(batchDims + m + n).let { if (mismatchException != null) it.withException(mismatchException) else it }

}
