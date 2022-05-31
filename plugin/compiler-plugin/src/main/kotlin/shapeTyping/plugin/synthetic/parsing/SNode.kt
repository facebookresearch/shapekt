/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

sealed interface SNode {
    val startIdx: Int
    val endIdx: Int
}

// Top-level nodes for an @SData argument, depending on the annotation target:

data class ArgumentList(
    val arguments: List<STypeNode>,
    override val startIdx: Int,
    override val endIdx: Int
): SNode

data class DeclarationList(
    val declarations: List<STypeDeclaration>,
    override val startIdx: Int,
    override val endIdx: Int
): SNode

// TODO: Doing this for now to avoid duplicating a lot of similar information, but it might be safer to each of these things in its own node type instead.
//       Used for identifiers, wildcards, and function names.
enum class DataType {
    SHAPE,
    DIM,
    ERROR,
}

sealed interface STypeNode: SNode {
    val dataType: DataType? // TODO: Non-null when resolution is plugged in
}

data class STypeDeclaration(
    val id: STypeIdentifier,
    override val startIdx: Int,
    override val endIdx: Int
): SNode

data class SFunctionCall(
    val name: FunctionIdentifier,
    val arguments: ArgumentList,
    override val dataType: DataType?,
    override val startIdx: Int,
    override val endIdx: Int
): STypeNode

data class STypeIdentifier(
    val name: String,
    override val dataType: DataType?, // TODO: We should always be able to resolve to something non null
    override val startIdx: Int,
    override val endIdx: Int,
    val upperBound: STypeNode?,
    val isStrictBound: Boolean,
    val scopeId: Int? // TODO: non-nullable after resolution/parsing are hooked together
): STypeNode

data class FunctionIdentifier(
    val name: String,
    override val startIdx: Int,
    override val endIdx: Int
): SNode

// Shapes:

data class ShapeLiteral(
    val dims: List<STypeNode>, // TODO: Parser and resolver are responsible for making sure this is all dims, but we could also continue messing with the class organization
    override val startIdx: Int,
    override val endIdx: Int
): STypeNode {
    override val dataType: DataType get() = DataType.SHAPE
}

data class IntLiteral(
    val value: Int,
    override val startIdx: Int,
    override val endIdx: Int
): STypeNode {
    override val dataType: DataType get() = DataType.DIM
}

data class Wildcard(
    override val dataType: DataType?,
    override val startIdx: Int,
    override val endIdx: Int,
): STypeNode