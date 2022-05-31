/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.diagnostics

import shapeTyping.analysis.ShapeFunctionCall
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import shapeTyping.analysis.SType

object ShapeTypingErrors {
    // Errors from static shape analysis
    @JvmField
    val SHAPE_FUNCTION_ERROR =
            DiagnosticFactory2.create<PsiElement, ShapeFunctionCall, String> (
                    Severity.ERROR
            )

    @JvmField
    val INVALID_STYPE_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    @JvmField
    val UNRESOLVED_SHAPE_FUNCTION_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    @JvmField
    val STYPE_MISMATCH_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    @JvmField
    val STYPE_PARSING_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    // Incorrect input to the @DeclareParams annotation
    @JvmField
    val DECLARE_PARAMS_ANNOTATION_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    // Redeclaration in the @DeclareParams annotation
    @JvmField
    val SHAPEORDIM_REDECLARATION_ERROR =
        DiagnosticFactory2.create<PsiElement, String, String> (
            Severity.ERROR
        )

    @JvmField
    val UNRESOLVED_SHAPEORDIM_REFERENCE_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    @JvmField
    val STYPE_VARIABLE_INFERENCE_ERROR =
        DiagnosticFactory1.create<PsiElement, String> (
            Severity.ERROR
        )

    @JvmField
    val UNREDUCED_STYPE_ERROR =
        DiagnosticFactory1.create<PsiElement, SType> (
            Severity.ERROR
        )

    @JvmField
    val UNCHECKED_SHAPE_CAST =
        DiagnosticFactory0.create<PsiElement> (
            Severity.WARNING
        )

    @JvmField
    val SHAPE_CAST_NEVER_SUCCEEDS =
        DiagnosticFactory0.create<PsiElement> (
            Severity.WARNING
        )

    init {
        Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            ShapeTypingErrors::class.java,
            ShapeTypingErrorMessages()
        )
    }
}