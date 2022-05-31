/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import shapeTyping.analysis.SType
import shapeTyping.plugin.synthetic.parsing.STypeDeclaration

fun FunctionDescriptor.getReceiverDescriptor() = this.extensionReceiverParameter ?: this.dispatchReceiverParameter

fun KtThisExpression.getType(context: CallCheckerContext) = this.getResolvedCall(context.trace.bindingContext)?.resultingDescriptor?.returnType

fun FunctionDescriptorImpl.setFlagsFrom(sourceDescriptor: FunctionDescriptor) {
    this.isOperator = sourceDescriptor.isOperator
    this.isInfix = sourceDescriptor.isInfix
    this.isExternal = sourceDescriptor.isExternal
    this.isInline = sourceDescriptor.isInline
    this.isTailrec = sourceDescriptor.isTailrec
    this.isExpect = sourceDescriptor.isExpect
    this.isActual = sourceDescriptor.isActual
    this.overriddenDescriptors = sourceDescriptor.overriddenDescriptors
}

fun KtElement.isTodo(context: CallCheckerContext) : Boolean {
    if (this !is KtCallExpression) return false
    val callee = this.calleeExpression
    if (callee !is KtNameReferenceExpression) return false
    val calleeDescriptor = context.trace.get(BindingContext.REFERENCE_TARGET, callee)!!
    return calleeDescriptor.fqNameSafe == FqName("kotlin.TODO")
}

/**
 * At a dot call such as t1.op(t2), the selector is the part after the dot (op(t2))
 */
internal fun KtElement.isSelectorExprOfDotCall(): Boolean {
    val parent = this.parent
    return parent is KtDotQualifiedExpression && parent.selectorExpression === this
}

internal fun KtExpression.deparenthesized() = if (this is KtParenthesizedExpression) this.expression else this

internal fun getFullDotCallExprFromSelector(callElement: KtElement): KtDotQualifiedExpression {
    return callElement.parent as KtDotQualifiedExpression
}

data class DeclarationInfo(val declarations: Map<String, STypeDeclaration>, val scopeID: Int) {
    fun addToScope(dec: STypeDeclaration) {
        // TODO: Neater way to handle this? Should we make a mutable subclass?
        if (declarations is MutableMap) {
            declarations[dec.id.name] = dec
        }
        else throw IllegalArgumentException("Declaration info is not mutable")
    }

    fun lookupInScope(id: String): STypeDeclaration? = declarations[id]
}

fun isRhsOfReturn(expr: KtElement) = expr.parent is KtReturnExpression

fun isRhsOfReassignment(expr: KtElement): Boolean {
    val parent = expr.parent
    if (parent !is KtExpression) return false
    val assignment = parent.asAssignment() ?: return false
    return assignment.right === expr
}

fun isRhsOfAssignment(expr: KtElement) =
    isRhsOfDeclaration(expr) || isRhsOfReassignment(expr)

fun isRhsOfDeclaration(expr: KtElement) = expr.parent is KtDeclaration