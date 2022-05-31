/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import shapeTyping.analysis.*
import shapeTyping.analysis.utils.BoundSubstitutionIterator
import shapeTyping.analysis.utils.containsSymbols
import shapeTyping.analysis.utils.STypeSubtyper.isStrictSubtypeOf
import shapeTyping.analysis.utils.STypeSubtyper.isSubtypeOf

data class ArgTypeToConstraint(val argType: SType, val constraint: SType)

/**
 * Checks that each argument shape matches its expected shape in, and gets a map of substitutions for this call.
 */
fun inferArgsAgainstConstraint(
    argsToConstraints: List<ArgTypeToConstraint>,
    substitutions: Map<SType, SType>
): Map<SType, SType>? =
    argsToConstraints.fold(substitutions as Map<SType, SType>?) { subs, argConstraint ->
        if (subs != null) inferArgAgainstConstraint(argConstraint.argType, argConstraint.constraint, subs) else null
    }

/**
 * Attempts to match each argument to its expected shape from left-to-right, and returns a map of any non-conflicting
 * substitutions that it can extract from these matches. Note that all substitutions from an invalid argument are
 * discarded, not only the one which caused the mismatch.
 */
fun latestValidInference(
    argsToConstraints: List<ArgTypeToConstraint>,
    substitutions: Map<SType, SType>
): Pair<Map<SType, SType>, Boolean> =
    argsToConstraints.fold(Pair(substitutions, true)) { subInfo, argConstraint ->
        val inferredArgs = inferArgAgainstConstraint(argConstraint.argType, argConstraint.constraint, subInfo.first)
        if (inferredArgs != null) Pair(inferredArgs, subInfo.second) else Pair(subInfo.first, false)
    }

/**
 * Checks that the argument shape matches its expected shape in, and gets a map of substitutions combining existing
 * substitutions with those newly inferred from this argument.
 */
fun inferArgAgainstConstraint(
    argument: SType,
    constraint: SType,
    substitutions: Map<SType, SType>
): Map<SType, SType>? {
    val newSubstitutions = argument.inferSubstitutionMap(constraint, substitutions) ?: return null
    return newSubstitutions + substitutions
}

fun SType.fullyInferredFrom(substitutions: Map<SType, SType>): Boolean {
    return when (this) {
        is SymbolicShape, is SymbolicDim -> substitutions[this] != null
        is DimShape -> this.dims.all { it.fullyInferredFrom(substitutions) }
        is ShapeFunctionCall -> this.args.all { it.fullyInferredFrom(substitutions) }
        is STypeTuple -> this.types.all { it.fullyInferredFrom(substitutions) }
        else -> true
    }
}

internal fun SType.inferSubstitutionMap(
    other: SType,
    previousSubstitutions: Map<SType, SType>
): Map<SType, SType>? {

    if (!other.containsSymbols()) {
        return null
    }
    var prevSubForOther = other

    fun getSubstitutions(currSubForThis: SType): Map<SType, SType>? {
        val newSubstitutions = currSubForThis.getSubstitutionsToSubstituteInto(prevSubForOther) ?: return null
        if (newSubstitutions.keys.any {
            it in previousSubstitutions && newSubstitutions[it] != previousSubstitutions[it]
        }) return null
        return newSubstitutions
    }

    // we have a symbolic shape, try substituting
    for ((currSubForThis, currSubForOther) in BoundSubstitutionIterator(this, other)) {
        if (currSubForThis.isSubtypeOf(currSubForOther)) {
            return getSubstitutions(currSubForThis)
        }
        prevSubForOther = currSubForOther
    }
    return null
}

internal fun SType.getSubstitutionsToSubstituteInto(other: SType): Map<SType, SType>? {
    require(other.containsSymbols()) { "cannot substitute into non-symbolic shape" }
    if (this == other) return emptyMap()
    return when (other) {
        // TODO: Add some bounds checking to this (just check subtype of upper bound for symbolic). Figure out chains of symbols?
        // For now assume/enforce bounds don't have symbols
        is SymbolicShape -> if (this is Shape && other.canSubstituteWith(this)) mapOf(other to this) else null
        is SymbolicDim -> if (this is Dim && other.canSubstituteWith(this)) mapOf(other to this) else null
        is DimShape -> {
            assert(this is DimShape && this.dims.size == other.dims.size)
            val zippedDims = other.dims.zip((this as DimShape).dims)
            zippedDims.filter { (otherDim, _) -> otherDim is SymbolicDim }.toMap()
        }
        is ShapeFunctionCall -> {
            val res = mutableMapOf<SType, SType>()
            assert(this is ShapeFunctionCall && this.args.size == other.args.size)
            val zippedArgs = other.args.zip((this as ShapeFunctionCall).args)
            zippedArgs.forEach { (otherDim, thisDim) ->
                if (otherDim is SymbolicDim || otherDim is SymbolicShape) {
                    res[otherDim] = thisDim
                }

            }
            res
        }
        is STypeTuple -> {
            assert(this is STypeTuple && this.types.size == other.types.size)
            val zippedSTypes = other.types.zip((this as STypeTuple).types)
            val res = mutableMapOf<SType, SType>()
            zippedSTypes.forEach {
                if (it.first.containsSymbols()) {
                    val subs = it.second.getSubstitutionsToSubstituteInto(it.first)
                    if (subs == null || subs.keys.any { it in res && res[it] != subs[it] }) return null
                    res.putAll(subs)
                }
            }
            res
        }
        else -> throw Exception("only SymbolicShapes, DimShapes, or ShapeFunctionCalls can be symbolic")
    }
}

private fun SymbolicDim.canSubstituteWith(other: Dim): Boolean = if (this.declarationInfo.isStrictBound)
    other.isStrictSubtypeOf(this.declarationInfo.upperBound) else other.isSubtypeOf(this.declarationInfo.upperBound)

private fun SymbolicShape.canSubstituteWith(other: Shape): Boolean = if (this.declarationInfo.isStrictBound)
    other.isStrictSubtypeOf(this.declarationInfo.upperBound) else other.isSubtypeOf(this.declarationInfo.upperBound)

fun SymbolicShape.allBounds(): List<Shape> {
    var curr: Shape = this
    val result = mutableListOf<Shape>()
    while (curr is SymbolicShape) {
        val next = curr.declarationInfo.upperBound
        result.add(next)
        curr = next
    }
    return result
}

fun SymbolicDim.allBounds(): List<Dim> {
    var curr: Dim = this
    val result = mutableListOf<Dim>()
    while (curr is SymbolicDim) {
        val next = curr.declarationInfo.upperBound
        result.add(next)
        curr = next
    }
    return result
}