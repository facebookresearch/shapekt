/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.ide

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentsWithSelf
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.util.getType
import shapeTyping.analysis.*
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSType

class ShapeTypingExpressionShapeProvider: ExpressionTypeProvider<KtExpression>() {
    override fun getErrorHint(): String {
        return "No shape available."
    }

    override fun getInformationHint(element: KtExpression): String {
        val context = element.analyze()
        val type = element.getType(context)!!
        val attributes = type.attributes
        val sType = attributes.getSType()!!
        val typeName = type.constructor.declarationDescriptor?.name?.asString() ?: ""
        return "$typeName ${sType.render()}"
    }

    private fun SType.render(): String {
        return when(this) {
            is STypeTuple -> this.types.joinToString(" ") { it.render() }
            is SymbolicShape -> this.symbol
            is WildcardShape -> "Shape"
            is DimShape -> "[${this.dims.joinToString(", ") { it.render() }}]"
            is ShapeFunctionCall -> ""
            is SymbolicDim -> this.symbol
            is WildcardDim -> "Dim"
            is NumericDim -> this.value.toString()
            is ErrorDim, is ErrorUnknownClass, is ErrorShape -> ""
        }
    }

    private fun KtExpression.hasShape(): Boolean {
        val context = this.analyze()
        val attributes = this.getType(context)?.attributes ?: return false
        val sType = attributes.getSType()
        return sType != null
    }

    // Copied from KotlinExpressionTypeProvider
    override fun getExpressionsAt(elementAt: PsiElement): List<KtExpression> {
        val candidates = elementAt.parentsWithSelf.filterIsInstance<KtExpression>().filter { it.hasShape() }.toList()
        val fileEditor =
                elementAt.containingFile?.virtualFile?.let { FileEditorManager.getInstance(elementAt.project).getSelectedEditor(it) }
        val selectionTextRange = if (fileEditor is TextEditor) {
            EditorUtil.getSelectionInAnyMode(fileEditor.editor)
        } else {
            TextRange.EMPTY_RANGE
        }
        val anchor =
                candidates.firstOrNull { selectionTextRange.isEmpty || it.textRange.contains(selectionTextRange) } ?: return emptyList()
        return candidates.filter { it.textRange.startOffset == anchor.textRange.startOffset }
    }
}
