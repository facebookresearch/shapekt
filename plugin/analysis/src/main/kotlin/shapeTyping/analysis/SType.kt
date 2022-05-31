/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis

import shapeTyping.analysis.exceptions.STypeException

sealed class SType(open val error: STypeException?) {
    abstract fun withException(error: STypeException): SType
}

sealed class Shape(override val error: STypeException?) : SType(error) {
    abstract override fun withException(error: STypeException): Shape
}
sealed class Dim(override val error: STypeException?): SType(error) {
    abstract override fun withException(error: STypeException): Dim
}

data class STypeTuple(val types: List<SType>, override val error: STypeException? = null) : SType(error) {
    override fun toString(): String = "SType($types)"
    override fun withException(error: STypeException): STypeTuple = STypeTuple(types, error)
    override fun equals(other: Any?): Boolean {
        return other is STypeTuple && types == other.types
    }
    fun map(f: (SType) -> SType): STypeTuple = STypeTuple(types.map(f), error)
}

data class ErrorUnknownClass(override val error: STypeException) : SType(error) {
    override fun toString(): String = "ERROR${error.let { ": $it" } ?: ""}"
    override fun withException(error: STypeException): ErrorUnknownClass = ErrorUnknownClass(error)
}

data class ErrorShape(override val error: STypeException) : Shape(error) {
    override fun toString(): String = "ERROR_SHAPE${error.let { ": $it" } ?: ""}"
    override fun withException(error: STypeException): ErrorShape = ErrorShape(error)
}

data class ShapeDeclarationInfo(val upperBound: Shape, val isStrictBound: Boolean, val scopeID: Int)

// e.g. A
data class SymbolicShape(
    val symbol: String,
    val declarationInfo: ShapeDeclarationInfo,
    override val error: STypeException? = null
): Shape(error) {
    override fun toString(): String = "$symbol: ${declarationInfo.upperBound}"
    override fun withException(error: STypeException): SymbolicShape = SymbolicShape(symbol, declarationInfo, error)

    override fun equals(other: Any?): Boolean {
        if (other !is SymbolicShape) return false
        return this.symbol == other.symbol && this.declarationInfo.scopeID == other.declarationInfo.scopeID
    }
}

data class WildcardShape(override val error: STypeException? = null) : Shape(error) {
    override fun toString() = "Shape"
    override fun withException(error: STypeException): WildcardShape = WildcardShape(error)
    override fun equals(other: Any?): Boolean = other is WildcardShape
    companion object {
        val DEFAULT = WildcardShape()
    }
}

// e.g. matmul([1,2], A)
data class ShapeFunctionCall(
    val name: String,
    val args: List<SType>,
    override val error: STypeException? = null
): Shape(error) {
    override fun toString(): String = "$name(${args.joinToString(",")})"
    override fun withException(error: STypeException): ShapeFunctionCall = ShapeFunctionCall(name, args, error)

    override fun equals(other: Any?): Boolean =
        if (other !is ShapeFunctionCall) false
        else name == other.name && args == other.args
}

// e.g. [A, 1, 2, _]
data class DimShape(val dims : List<Dim>, override val error: STypeException? = null) : Shape(error) {
    val rank by lazy { dims.size }
    constructor(vararg dims: Dim) : this(dims.toList(), null)
    constructor(vararg dims: Int) : this(dims.map { NumericDim(it) }, null)

    override fun toString(): String = dims.toString()
    override fun withException(error: STypeException): DimShape = DimShape(dims, error)
    override fun equals(other: Any?): Boolean {
        return other is DimShape && dims == other.dims
    }

    operator fun get(i: Int): Dim = dims[i]

    fun slice(startIdx: Int, endIdx: Int): DimShape {
        return DimShape(sliceToDimList(startIdx, endIdx))
    }

    fun sliceToDimList(startIdx: Int, endIdx: Int): List<Dim> {
        require(startIdx in 0 until rank)
        require(endIdx in 0..rank)
        return dims.subList(startIdx, endIdx)
    }
}

data class ErrorDim(override val error: STypeException) : Dim(error) {
    override fun toString(): String = "ERROR_DIM"
    override fun withException(error: STypeException): ErrorDim = ErrorDim(error)
}

data class DimDeclarationInfo(val upperBound: Dim, val isStrictBound: Boolean, val scopeID: Int)

data class SymbolicDim(
    val symbol: String,
    val declarationInfo: DimDeclarationInfo,
    override val error: STypeException? = null
): Dim(error) {
    override fun toString() : String = "$symbol: ${declarationInfo.upperBound}"
    override fun withException(error: STypeException): SymbolicDim = SymbolicDim(symbol, declarationInfo, error)

    override fun equals(other: Any?): Boolean {
        if (other !is SymbolicDim) return false
        return this.symbol == other.symbol && this.declarationInfo.scopeID == other.declarationInfo.scopeID
    }
}

data class WildcardDim(override val error: STypeException? = null) : Dim(error) {
    override fun toString() : String = "Dim"
    override fun withException(error: STypeException): WildcardDim = WildcardDim(error)
    companion object {
        val DEFAULT = WildcardDim()
    }
}

data class NumericDim(val value: Int, override val error: STypeException? = null): Dim(error) {
    override fun toString(): String = value.toString()
    override fun withException(error: STypeException): NumericDim = NumericDim(value, error)

    override fun equals(other: Any?): Boolean {
        if (other !is NumericDim) return false
        return this.value == other.value
    }
}