/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis.utils

import shapeTyping.analysis.*

object STypeSubtyper {

    fun SType.isSubtypeOf(other: SType): Boolean = when (this.subtype(other)) {
        SubtypeResult.NOTSUBTYPE -> false
        SubtypeResult.STRICTSUBTYPE, SubtypeResult.SUBTYPE -> true
    }
    fun SType.isStrictSubtypeOf(other: SType): Boolean = this.subtype(other) == SubtypeResult.STRICTSUBTYPE

    fun SType.subtype(other: SType): SubtypeResult =
        when (this) {
            is Shape -> this.subtype(other)
            is Dim -> this.subtype(other)
            is STypeTuple -> this.subtype(other)
            else -> SubtypeResult.NOTSUBTYPE
        }

    enum class SubtypeResult {
        SUBTYPE,
        STRICTSUBTYPE,
        NOTSUBTYPE
    }

    private fun Dim.subtype(other: SType): SubtypeResult {
        if (this is ErrorDim) return SubtypeResult.NOTSUBTYPE
        return when (other) {
            this -> SubtypeResult.SUBTYPE
            is WildcardDim -> when {
                this is SymbolicDim -> this.upperBoundedBy(other)
                this is NumericDim -> SubtypeResult.STRICTSUBTYPE
                // TODO: shape function calls returning a dim should be subtypes
                else -> SubtypeResult.NOTSUBTYPE
            }
            is SymbolicDim -> if (this is SymbolicDim) this.upperBoundedBy(other) else SubtypeResult.NOTSUBTYPE
            is NumericDim -> {
                when (this) {
                    is NumericDim -> if (this.value == other.value) SubtypeResult.SUBTYPE else SubtypeResult.NOTSUBTYPE
                    is SymbolicDim -> this.upperBoundedBy(other)
                    else -> SubtypeResult.NOTSUBTYPE
                }
            }
            else -> SubtypeResult.NOTSUBTYPE
        }
    }

    private fun Shape.subtype(other: SType): SubtypeResult {
        if (this is ErrorShape) return SubtypeResult.NOTSUBTYPE
        return when (other) {
            this -> SubtypeResult.SUBTYPE
            is WildcardShape -> when {
                this is SymbolicShape -> this.upperBoundedBy(other)
                this is DimShape -> SubtypeResult.STRICTSUBTYPE
                this is ShapeFunctionCall -> SubtypeResult.SUBTYPE // TODO: Could be strict depending on the shape function.
                else -> SubtypeResult.NOTSUBTYPE
            }
            is SymbolicShape -> if (this is SymbolicShape) this.upperBoundedBy(other) else SubtypeResult.NOTSUBTYPE
            is DimShape -> {
                when (this) {
                    is DimShape -> this.dims.subtype(other.dims)
                    is SymbolicShape -> this.upperBoundedBy(other)
                    else -> SubtypeResult.NOTSUBTYPE
                }
            }
            else -> SubtypeResult.NOTSUBTYPE
        }
    }

    private fun SymbolicDim.upperBoundedBy(other: Dim): SubtypeResult {
        fun stepSubtypeTest(bound: Dim, strictStatus: Boolean): SubtypeResult =
            when (val subtypeTest = bound.subtype(other)) {
                SubtypeResult.STRICTSUBTYPE, SubtypeResult.SUBTYPE ->
                    if (strictStatus) SubtypeResult.STRICTSUBTYPE else subtypeTest
                else -> SubtypeResult.NOTSUBTYPE
            }

        var currentUpperBound = this.declarationInfo.upperBound
        var strictStatus = this.declarationInfo.isStrictBound
        while (currentUpperBound is SymbolicDim) {
            when (val step = stepSubtypeTest(currentUpperBound, strictStatus)) {
                SubtypeResult.NOTSUBTYPE -> {}
                else -> return step
            }
            strictStatus = currentUpperBound.declarationInfo.isStrictBound
            currentUpperBound = currentUpperBound.declarationInfo.upperBound
        }
        return stepSubtypeTest(currentUpperBound, strictStatus)
    }

    private fun SymbolicShape.upperBoundedBy(other: Shape): SubtypeResult {
        fun stepSubtypeTest(bound: Shape, strictStatus: Boolean): SubtypeResult =
            when (val subtypeTest = bound.subtype(other)) {
                SubtypeResult.STRICTSUBTYPE, SubtypeResult.SUBTYPE ->
                    if (strictStatus) SubtypeResult.STRICTSUBTYPE else subtypeTest
                else -> SubtypeResult.NOTSUBTYPE
            }

        var currentUpperBound = this.declarationInfo.upperBound
        var strictStatus = this.declarationInfo.isStrictBound
        while (currentUpperBound is SymbolicShape) {
            when (val step = stepSubtypeTest(currentUpperBound, strictStatus)) {
                SubtypeResult.NOTSUBTYPE -> {}
                else -> return step
            }
            strictStatus = currentUpperBound.declarationInfo.isStrictBound
            currentUpperBound = currentUpperBound.declarationInfo.upperBound
        }
        return stepSubtypeTest(currentUpperBound, strictStatus)
    }

    private fun STypeTuple.subtype(other: SType): SubtypeResult =
        if (other is STypeTuple) this.types.subtype(other.types) else SubtypeResult.NOTSUBTYPE

    // A list is considered to subtype another list if each of its element is a subtype of the element in matching position
    // in the other list. It is a strict subtype if at least one element is a strict subtype.
    private fun List<SType>.subtype(other: List<SType>): SubtypeResult {
        if (this.size != other.size) return SubtypeResult.NOTSUBTYPE
        var strictStatus = false
        this.zip(other).forEach {
            val (t1, t2) = it
            when (t1.subtype(t2)) {
                SubtypeResult.NOTSUBTYPE -> return SubtypeResult.NOTSUBTYPE
                SubtypeResult.STRICTSUBTYPE -> strictStatus = true
                SubtypeResult.SUBTYPE -> {}
            }
        }
        return if (strictStatus) SubtypeResult.STRICTSUBTYPE else SubtypeResult.SUBTYPE
    }

}