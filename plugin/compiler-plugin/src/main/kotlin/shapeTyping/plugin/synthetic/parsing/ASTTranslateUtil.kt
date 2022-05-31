/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeFailure

private val SHAPE_BASE_TYPE_NAME = "Shape"
private val DIM_BASE_TYPE_NAME = "Dim"

fun STypeNode.toShapeType(): Shape {
    // TODO: Check datatype
    return when (this) {
        is SFunctionCall -> ShapeFunctionCall(this.name.name, this.arguments.toSTypes())
        is ShapeLiteral -> DimShape(this.dims.map { it.toDimType() })
        is Wildcard -> WildcardShape.DEFAULT
        is STypeIdentifier -> when (this.name) {
            SHAPE_BASE_TYPE_NAME -> WildcardShape.DEFAULT
            else -> SymbolicShape(this.name,ShapeDeclarationInfo(this.upperBound!!.toShapeType(), this.isStrictBound, this.scopeId!!))
        }
        else -> throw Exception("Passed dim-typed argument to STypeNode.toShapeType")
    }
}

fun STypeNode.toDimType(): Dim {
    // TODO: Check datatype and resolution info
    return when (this) {
        is IntLiteral -> NumericDim(this.value)
        is Wildcard -> WildcardDim.DEFAULT
        is STypeIdentifier -> when (this.name) {
            DIM_BASE_TYPE_NAME -> WildcardDim.DEFAULT
            else -> SymbolicDim(this.name, DimDeclarationInfo(this.upperBound!!.toDimType(), this.isStrictBound, this.scopeId!!))
        }
        // TODO: Case for SFunctionCall?
        else -> throw Exception("Passed shape-typed argument to STypeNode.toDimType")
    }
}

fun STypeNode.toSType(): SType {
    return when (this) {
        is Shape, is ShapeLiteral -> this.toShapeType()
        is Dim, is IntLiteral -> this.toDimType()
        // TODO: Currently we do not resolve function names at this stage, so the datatype of an SFunctionCall is null.
        //       Assuming shape for now
        is SFunctionCall -> this.toShapeType()
        is STypeIdentifier, is Wildcard -> when (this.dataType) {
            DataType.SHAPE -> this.toShapeType()
            DataType.DIM -> this.toDimType()
            DataType.ERROR, null -> ErrorUnknownClass(STypeFailure("Datatype not inferred"))
        }
    }
}

fun STypeDeclaration.toSType(): SType {
    return this.id.toSType()
}

fun ArgumentList.toSType(): SType = if (this.arguments.size == 1) this.arguments.single().toSType()
    else STypeTuple(this.arguments.map { it.toSType() })

fun DeclarationList.toSType(): SType = if (this.declarations.size == 1) this.declarations.single().toSType()
    else STypeTuple(this.declarations.map { it.toSType() })

fun ArgumentList.toSTypes(): List<SType> = this.arguments.map { it.toSType() }