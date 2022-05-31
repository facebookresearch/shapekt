/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isInt
import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.plugin.analysis.getGeneralSType
import shapeTyping.plugin.checkers.utils.AttributesUtils.getDefaultSType
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSType
import shapeTyping.plugin.checkers.utils.AttributesUtils.union

data class ArgumentInfo(
    val basicArgumentInfo: List<BasicArgumentInfo>,
    val varargArgumentInfo: VarargArgumentInfo?,
    val receiverArgumentInfo: ReceiverArgumentInfo?
) {
    fun toArgConstraintPairs(context: BindingContext): List<ArgTypeToConstraint> {
        fun wrapInList(item: ArgTypeToConstraint?) = item?.let { listOf(it) } ?: emptyList()
        val receiverPair = wrapInList(receiverArgumentInfo?.toArgConstraintPair(context))
        val mainArgumentPairs = basicArgumentInfo.mapNotNull { it.toArgConstraintPair(context) }
        val varargInfo = wrapInList(varargArgumentInfo?.toArgConstraintPair(context))
        return receiverPair + mainArgumentPairs + varargInfo
    }
}

data class BasicArgumentInfo(val arg: ValueArgument, val param: ValueParameterDescriptor) {
    fun toArgConstraintPair(context: BindingContext): ArgTypeToConstraint? {
        val paramSType = param.type.getSType() ?: param.type.getDefaultSType() ?: return null
        val argSType = getInferredSType(arg, param, param.module.builtIns.intType, paramSType, context)
        return ArgTypeToConstraint(argSType, paramSType)
    }
}

data class ReceiverArgumentInfo(val arg: ExpressionReceiver, val param: ReceiverParameterDescriptor) {
    fun toArgConstraintPair(context: BindingContext): ArgTypeToConstraint? {
        val paramSType = param.type.getSType() ?: param.type.getDefaultSType() ?: return null
        val argSType = getInferredSType(arg, param, param.module.builtIns.intType, paramSType, context)
        return ArgTypeToConstraint(argSType, paramSType)
    }
}

data class VarargArgumentInfo(val args: List<ValueArgument>, val param: ValueParameterDescriptor) {
    private val intType = param.module.builtIns.intType
    fun toArgConstraintPair(context: BindingContext): ArgTypeToConstraint? {
        val paramSType = param.varargElementType?.getSType() ?: param.varargElementType?.getDefaultSType() ?: return null
        val argSTypes = args.map { getInferredSType(it, param, intType, paramSType, context) }
        val combinedArgSType = if (paramSType is Shape && argSTypes.all { it is Dim }) {
            DimShape(argSTypes as List<Dim>)
        } else {
            // TODO: When we have more validation on the annotations, change this to a compiler exception since
            //       it should never get to this point.
            argSTypes.reduce { s1, s2 -> s1.union(s2) ?: ErrorUnknownClass(STypeFailure("Incompatible STypes $s1 and $s2")) }
        }
        return ArgTypeToConstraint(combinedArgSType, paramSType)
    }
}

fun substitutionMapFromArguments(
    callableDescriptor: CallableDescriptor,
    call: Call,
    context: BindingContext
): Map<SType, SType> =
    substitutionMapAndStatusFromArguments(callableDescriptor, call, context).first

// Returns as many valid substitutions as possible from this set of arguments, and whether substitution was fully successful.
fun substitutionMapAndStatusFromArguments(
    callableDescriptor: CallableDescriptor,
    call: Call,
    context: BindingContext
): Pair<Map<SType, SType>, Boolean> {
    val argumentInfo = getArgumentInfo(call, callableDescriptor)
    val argConstraintPairs = argumentInfo.toArgConstraintPairs(context)
    return latestValidInference(argConstraintPairs, emptyMap())
}

fun getArgumentInfo(call: Call, callableDescriptor: CallableDescriptor): ArgumentInfo {
    val valParams = callableDescriptor.valueParameters
    val basicArguments: MutableList<BasicArgumentInfo> = mutableListOf()
    val varargArguments: MutableList<ValueArgument> = mutableListOf()
    var varargParam: ValueParameterDescriptor? = null
    val vpMap = valParams.associateBy {
        if (it.isVararg) varargParam = it
        it.name
    }
    call.valueArguments.forEachIndexed { i, arg ->
        val param = vpMap[arg.getArgumentName()?.asName]
        when {
            param == null -> // Positional matching.
                if (i >= valParams.size - 1 && varargParam != null) varargArguments.add(arg)
                else basicArguments.add(BasicArgumentInfo(arg, valParams[i]))
            param.isVararg -> varargArguments.add(arg) // Vararg name matching
            else -> basicArguments.add(BasicArgumentInfo(arg, param)) // Name matching
        }
    }

    val varargInfo = varargParam?.let { VarargArgumentInfo(varargArguments, it) }
    val receiverInfo = (call.explicitReceiver as? ExpressionReceiver)?.let {
        val param = callableDescriptor.extensionReceiverParameter ?: callableDescriptor.dispatchReceiverParameter
        if (param != null) ReceiverArgumentInfo(it, param) else null
    }
    return ArgumentInfo(basicArguments, varargInfo, receiverInfo)

}

private fun getCompileTimeDim(
    arg: KtExpression,
    intType: KotlinType,
    context: BindingContext
): Dim {
    val compileTimeValue = context.get(BindingContext.COMPILE_TIME_VALUE, arg)
        ?: return WildcardDim.DEFAULT
    return NumericDim(compileTimeValue.toConstantValue(intType).value as Int)
}

private fun getInferredSType(
    arg: ValueArgument,
    param: ValueParameterDescriptor,
    intType: KotlinType,
    paramSType: SType,
    context: BindingContext
): SType {
    val argExp = arg.getArgumentExpression()
    argExp?.getType(context)?.getSType()?.let { return it }
    return if (argExp != null && (param.type.isInt() || param.varargElementType?.isInt() == true)) {
        return getCompileTimeDim(argExp, intType, context)
    } else paramSType.getGeneralSType()
}

private fun getInferredSType(
    arg: ExpressionReceiver,
    param: ReceiverParameterDescriptor,
    intType: KotlinType,
    paramSType: SType,
    context: BindingContext
): SType {
    arg.type.getSType()?.let { return it }
    return if (param.type.isInt()) {
        return getCompileTimeDim(arg.expression, intType, context)
    } else paramSType.getGeneralSType()
}