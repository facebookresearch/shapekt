/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis.utils

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeCompilerException
import shapeTyping.analysis.exceptions.STypeFailure

private fun SType.recursivePredicateAny(p: (SType) -> Boolean): Boolean {
    return p(this) || when (this) {
        is DimShape -> dims.any { it.recursivePredicateAny(p) }
        is ShapeFunctionCall -> args.any { it.recursivePredicateAny(p) }
        is STypeTuple -> types.any { it.recursivePredicateAny(p) }
        else -> false
    }
}

/**
 * Returns true iif `this` contains any symbolic shapes or dims
 */
fun SType.containsSymbols(): Boolean = recursivePredicateAny { it is SymbolicDim || it is SymbolicShape }

fun SType.containsErrors(): Boolean = recursivePredicateAny { it.isError() }

fun SType.containsShapeFunctionCalls(): Boolean = recursivePredicateAny { it is ShapeFunctionCall }


/**
 * Get next generic substitution for `this`. Substitutes all generics in `this`, non-recursively.
 *
 * For example, if we have DimShape(1, B), and B : C, we will return DimShape(1, C).
 */
fun SType.substituteGenericsWithUpperBounds(): SType {
    return when (this) {
        is Shape -> this.substituteGenericsWithUpperBounds()
        is Dim -> this.substituteGenericsWithUpperBounds()
        is STypeTuple -> STypeTuple(this.types.map { it.substituteGenericsWithUpperBounds() })
        is ErrorUnknownClass -> this
    }
}

/**
 * See SType.substituteGenericsWithUpperBounds
 */
fun Dim.substituteGenericsWithUpperBounds(): Dim {
    if (this !is SymbolicDim) return this
    return this.declarationInfo.upperBound
}

/**
 * See SType.substituteGenericsWithUpperBounds
 */
fun Shape.substituteGenericsWithUpperBounds(): Shape {
    if (!this.containsSymbols()) return this
    return when (this) {
        is SymbolicShape -> this.declarationInfo.upperBound
        is DimShape -> DimShape(this.dims.map { it.substituteGenericsWithUpperBounds() })
        is ShapeFunctionCall -> ShapeFunctionCall(this.name, this.args.map { it.substituteGenericsWithUpperBounds() })
        else -> throw Exception("only SymbolicShapes, DimShapes, or ShapeFunctionCalls can be symbolic")
    }
}

/**
 * Given substitutions, substitutes all generics in the current shape (non-recursively). For example,
 * given `this` = [A, B] and substitutions = {A : 1, B : GenericBoundForB}, we return [1, GenericBoundForB]
 *
 */
fun SType.substitute(substitutions: Map<SType, SType>): SType {
    if (!this.containsSymbols()) {
        return this
    }

    fun SType.uninferredError() = when (this) {
        is SymbolicShape -> ErrorShape(STypeFailure("Could not infer type for ${this.symbol}"))
        is SymbolicDim -> ErrorDim(STypeFailure("Could not infer type for ${this.symbol}"))
        else -> throw STypeCompilerException("Type $this is not symbolic!")
    }

    fun SType.getSubstitutionIfExists(): SType {
        var curr = this
        var next = substitutions[this]
        while (next != null) {
            curr = next
            next = substitutions[curr]
        }
        return curr
    }

    return when (this) {
        is SymbolicShape, is SymbolicDim -> this.getSubstitutionIfExists()
        is DimShape -> {
            val newDims = this.dims.map { it.substitute(substitutions) as Dim }
            return DimShape(newDims)
        }
        is ShapeFunctionCall -> {
            val newArgs = this.args.map {
                when {
                    it is SymbolicDim -> it.getSubstitutionIfExists()
                    it is Shape && it.containsSymbols() -> it.substitute(substitutions)
                    else -> it
                }
            }
            return ShapeFunctionCall(this.name, newArgs)
        }
        is NumericDim -> this
        is STypeTuple -> STypeTuple(this.types.map { it.substitute(substitutions) })
        else -> throw Exception("only SymbolicShapes, DimShapes, or ShapeFunctionCalls can be symbolic")
    }
}

/**
 * Iterates through pairs of generic substitutions for [l] and [r].
 *
 * returns pairs in the order of the following pseudocode:
 * for sub_l in substitutions for l {
 *     for sub_r in substitutions for r {
 *         return Pair(sub_l, sub_r)
 *     }
 * }
 *
 */
class BoundSubstitutionIterator(l: SType, val r: SType) : Iterator<Pair<SType, SType>> {
    var currSubForL = l
    var currSubForR = r
    var isFirstPair = true

    override fun hasNext(): Boolean {
        return isFirstPair || currSubForL.containsSymbols() || currSubForR.containsSymbols()
    }

    override fun next(): Pair<SType, SType> {
        if (isFirstPair) return Pair(currSubForL, currSubForR).also { isFirstPair = false }

        // substitute and return next pair.
        if (currSubForR.containsSymbols())
            currSubForR = currSubForR.substituteGenericsWithUpperBounds()
        else if (currSubForL.containsSymbols()) {
            currSubForR = r
            currSubForL = currSubForL.substituteGenericsWithUpperBounds()
        }
        return Pair(currSubForL, currSubForR)
    }
}