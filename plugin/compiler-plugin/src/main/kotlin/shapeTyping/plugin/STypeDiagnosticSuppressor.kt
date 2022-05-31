/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import shapeTyping.plugin.ShapeTypingAnnotationFqNames.STYPE_FQNAME

class STypeDiagnosticSuppressor : DiagnosticSuppressor {
    companion object {
        fun registerExtension(
            project: Project,
            extension: DiagnosticSuppressor
        ) {
            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
                .registerExtension(extension, project)
        }
    }

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        val element = diagnostic.psiElement
        if (diagnostic.factory == Errors.USELESS_CAST && element is KtBinaryExpressionWithTypeRHS && KtPsiUtil.isCast(element)) {
            val typeRef = element.right ?: return false
            return typeRef.annotationEntries.any {
                if (bindingContext != null)
                    bindingContext.get(BindingContext.ANNOTATION, it)?.fqName == STYPE_FQNAME
                else
                    it.shortName?.asString() == "SType"
            }
        }
        return false
    }
    override fun isSuppressed(diagnostic: Diagnostic): Boolean {
        return isSuppressed(diagnostic, null)
    }
}