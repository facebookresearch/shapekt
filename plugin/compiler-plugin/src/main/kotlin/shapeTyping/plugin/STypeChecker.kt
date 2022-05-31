/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.ParametrizedDiagnostic
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import shapeTyping.analysis.SType
import shapeTyping.analysis.ShapeFunctionCall
import shapeTyping.analysis.exceptions.*
import shapeTyping.analysis.isError
import shapeTyping.analysis.utils.containsErrors
import shapeTyping.analysis.utils.containsShapeFunctionCalls
import shapeTyping.analysis.utils.containsSymbols
import shapeTyping.analysis.utils.substitute
import shapeTyping.plugin.analysis.STypeSubtyper.isSubtypeOf
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSType
import shapeTyping.plugin.checkers.utils.inferSubstitutionMap
import shapeTyping.plugin.diagnostics.ShapeTypingErrors

class STypeChecker : AdditionalTypeChecker {

    private fun substituteType(expression: KtExpression, actualSType: SType, expectedSType: SType, c: ResolutionContext<*>): SType? {
        val substitutionMap = actualSType.inferSubstitutionMap(expectedSType, emptyMap())
        val substitutedType = substitutionMap?.let { actualSType.substitute(it) } ?: actualSType
        return if (!substitutedType.isSubtypeOf(expectedSType) && substitutionMap == null) {
            c.trace.reportDiagnosticOnce(
                ShapeTypingErrors.STYPE_MISMATCH_ERROR.on(
                    expression,
                    "Expected shape $expectedSType, got $actualSType"
                )
            )
            null
        } else substitutedType
    }

    private fun diagnosticIfExists(expression: KtExpression, sType: SType): ParametrizedDiagnostic<PsiElement>? {
        val e = sType.error
        return when (e) {
            null -> null
            is ShapeFunctionResolutionError -> ShapeTypingErrors.UNRESOLVED_SHAPE_FUNCTION_ERROR
            is STypeParsingError -> ShapeTypingErrors.STYPE_PARSING_ERROR
            is STypeFailure -> ShapeTypingErrors.INVALID_STYPE_ERROR
            else -> if (e.errorLevel == STypeErrorLevel.ERROR) ShapeTypingErrors.INVALID_STYPE_ERROR
            else null // TODO: Warning lvel reporting
        }?.on(expression, e?.message!!)
    }

    // TODO: Function type subtyping
    private fun checkSingleType(
        expression: KtExpression,
        actualType: KotlinType,
        expectedType: KotlinType?,
        c: ResolutionContext<*>,
        allowsUnreduced: Boolean
    ) {
        val expectedSType = expectedType?.getSType()
        val actualSType = actualType.getSType() ?: return
        if (expectedSType?.isError() == true) return // This should be handled by the declaration checker

        val expressionError = diagnosticIfExists(expression, actualSType)?.also { c.trace.reportDiagnosticOnce(it) }

        val substitutedType = if (expressionError == null && expectedSType != null) {
            substituteType(expression, actualSType, expectedSType, c)
        } else actualSType

        if (!allowsUnreduced) {
            reportUnreducedErrors(substitutedType, expression, c)
        }

    }

    private fun reportUnreducedErrors(sType: SType?, expression: KtExpression, c: ResolutionContext<*>) {
        sType ?: return
        val error =
            if (sType.containsErrors() || sType.propagatesError()) null
            else if (sType.containsShapeFunctionCalls()) ShapeTypingErrors.UNREDUCED_STYPE_ERROR
            else null
        if (error != null)
            c.trace.reportDiagnosticOnce(error.on(expression, sType))
    }

    private fun checkSType(
        expression: KtExpression,
        expressionType: KotlinType,
        expectedType: KotlinType?,
        c: ResolutionContext<*>,
        allowsUnreduced: Boolean
    ) {
        // Check the current top level type.
        checkSingleType(expression, expressionType, expectedType, c, allowsUnreduced)

        // Check arguments recursively.
        val expectedArgs = expectedType?.arguments ?: emptyList()
        val expressionArgs = expressionType.arguments
        if (expectedArgs.size != expressionArgs.size) return

        expectedArgs.zip(expressionArgs).forEach { (expectedArg, expressionArg) ->
            if (!expectedArg.isStarProjection && !expressionArg.isStarProjection) // star projections cannot have annotations.
                checkSType(expression, expressionArg.type, expectedArg.type, c, allowsUnreduced)
        }

    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>,
    ) {
        val expectedType = if (TypeUtils.noExpectedType(c.expectedType)) null else c.expectedType
        val allowsUnreduced = allowsUnreduced(c)

        // Check the current top level type.
        checkSType(
            expression,
            expressionTypeWithSmartCast,
            expectedType,
            c,
            allowsUnreduced
        )

    }

    override fun checkReceiver(
        receiverParameter: ReceiverParameterDescriptor,
        receiverArgument: ReceiverValue,
        safeAccess: Boolean,
        c: CallResolutionContext<*>
    ) {
        super.checkReceiver(receiverParameter, receiverArgument, safeAccess, c)
        (receiverArgument as? ExpressionReceiver)?.expression?.let { expr ->
            checkSingleType(expr, receiverArgument.type, receiverParameter.type, c, allowsUnreduced(c))
        }
    }

    private fun allowsUnreduced(c: ResolutionContext<*>): Boolean {
        val desc = c.scope.ownerDescriptor
        val unreducedAnnotation = desc.parentsWithSelf.firstNotNullOfOrNull {
            it.annotations.findAnnotation(ShapeTypingAnnotationFqNames.ALLOW_UNREDUCED_FQNAME)
        } ?: return false // default behavior (no annotation) is to disallow unreduced STypes

        // Default arg of @AllowUnreduced is true
        return (unreducedAnnotation.allValueArguments.values.firstOrNull()?.value ?: true) as Boolean
    }
}