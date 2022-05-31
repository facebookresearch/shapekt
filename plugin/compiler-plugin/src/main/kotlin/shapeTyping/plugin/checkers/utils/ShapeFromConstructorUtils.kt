/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import shapeTyping.analysis.DimShape
import shapeTyping.analysis.NumericDim
import shapeTyping.analysis.SType
import shapeTyping.analysis.WildcardDim

fun getShapeFromRuntimeShapeConstructor(
    candidateDescriptor: ClassConstructorDescriptor,
    call: Call,
    trace: BindingTrace
): SType {
    val dims = call.valueArguments.map { arg ->
        val compileTimeValue =
            trace.get(BindingContext.COMPILE_TIME_VALUE, arg.getArgumentExpression())
                ?: return@map WildcardDim.DEFAULT
        val value = compileTimeValue.toConstantValue(candidateDescriptor.module.builtIns.intType).value as Int
        NumericDim(value)
    }
    return DimShape(dims)
}