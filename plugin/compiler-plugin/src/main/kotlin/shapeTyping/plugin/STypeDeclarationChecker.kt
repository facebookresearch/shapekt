/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import shapeTyping.analysis.exceptions.STypeCompilerException
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.analysis.exceptions.STypeParsingError
import shapeTyping.analysis.exceptions.ShapeFunctionResolutionError
import shapeTyping.plugin.analysis.STypeFunctionError
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSType
import shapeTyping.plugin.diagnostics.ShapeTypingErrors

class STypeDeclarationChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (declaration is KtTypeAlias) {
            return
        }
        if (descriptor is PropertyGetterDescriptor) {
            // Prevent double-reporting when the corresponding PropertyDescriptor is also checked
            return
        }
        declaration.children.forEach {
            (it as? KtTypeReference)?.let { typeRef ->
                val type = context.trace.get(BindingContext.TYPE, typeRef) ?: throw STypeCompilerException("No type available for typeRef $typeRef")
                val toDiagnostic: ((KtAnnotationEntry) -> Diagnostic)? = when (val e = type.getSType()?.error) {
                    is ShapeFunctionResolutionError ->
                        { annotation -> ShapeTypingErrors.UNRESOLVED_SHAPE_FUNCTION_ERROR.on(annotation, e.message) }
                    is STypeParsingError ->
                        { annotation -> ShapeTypingErrors.STYPE_PARSING_ERROR.on(annotation, e.message) }
                    is STypeFunctionError ->
                        { annotation -> ShapeTypingErrors.SHAPE_FUNCTION_ERROR.on(annotation, e.call, e.message) }
                    is STypeFailure ->
                        { annotation -> ShapeTypingErrors.INVALID_STYPE_ERROR.on(annotation, e.message) }
                    else -> null
                }
                if (toDiagnostic != null) reportSTypeError(toDiagnostic, typeRef, context)
            }
        }
    }

    private fun reportSTypeError(
        annotationTodiagnostic: (KtAnnotationEntry) -> Diagnostic,
        typeRef: KtTypeReference,
        context: DeclarationCheckerContext
    ) {
        val annotation = typeRef.annotationEntries.find { entry ->
            context.trace.get(BindingContext.ANNOTATION, entry)?.fqName == ShapeTypingAnnotationFqNames.STYPE_FQNAME
        }
        require(annotation != null) { " Not sure why, but STypeAttribute exists but annotation doesn't for this type" }
        context.trace.report(annotationTodiagnostic(annotation))
    }
}
