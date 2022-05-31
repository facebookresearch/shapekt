/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.analysis

import shapeTyping.analysis.*
import shapeTyping.extensions.ShapeFunctionExtension
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.isCompanionObject
import org.jetbrains.kotlin.types.KotlinType
import shapeTyping.analysis.exceptions.STypeException
import shapeTyping.analysis.exceptions.ShapeFunctionResolutionError

open class STypeFunctionError(e: STypeException, val call: ShapeFunctionCall) : STypeException(e.message, e.errorLevel)

// TODO: This should return an SType based on the SType declared on the shaped class
fun KotlinType.defaultSType() : SType? = null

/**
 * Attempts to evaluate [shapeFnCall].
 *
 * Throws exceptions if [shapeCall]'s name is not found or if the found shape function does not return a Shape.
 *
 * If applying the shape function returns null, then [shapeFnCall] is returned.
 */
fun evaluateShapeFunctionCall(shapeFnCall: ShapeFunctionCall): SType {
    val shapeFnName = shapeFnCall.name
    val args = shapeFnCall.args
    val impl = ShapeFunctionExtension.Companion.extensions[shapeFnName] ?:
        return ErrorUnknownClass(ShapeFunctionResolutionError("Shape function $shapeFnName could not be resolved."))
    if (!SType::class.java.isAssignableFrom(impl.returnType))
        return ErrorUnknownClass(ShapeFunctionResolutionError("$shapeFnName does not return an SType as expected."))

    // Try to evaluate the function. If the function returns null, then the shape function call could
    // not be evaluated further and we return a ShapeFunctionCall instance to represent the current call.
    val evaled = (impl.apply(args) as Shape?) ?: shapeFnCall
    return evaled.error?.let { evaled.withException(STypeFunctionError(it, shapeFnCall)) } ?: evaled
}

fun SType.getGeneralSType(): SType = when (this) {
    is DimShape -> DimShape(this.dims.map { it.getGeneralSType() as Dim })
    is Shape -> WildcardShape.DEFAULT
    is NumericDim -> this
    is Dim -> WildcardDim.DEFAULT
    is STypeTuple -> STypeTuple(this.types.map { it.getGeneralSType()!! })
    is ErrorUnknownClass -> this
}

private fun DeclarationDescriptor.isInvoke() = this.isCompanionObject()

private fun DeclarationDescriptor.actsLikeConstructor() : Boolean {
    return this is ConstructorDescriptor || isInvoke()
}