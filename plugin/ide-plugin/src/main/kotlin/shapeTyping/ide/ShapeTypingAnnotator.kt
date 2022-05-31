/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

class ShapeTypingAnnotator : Annotator {
    private fun AnnotationHolder.highlight(range: TextRange, style: TextAttributesKey) {
        this.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(style)
            .create()
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtElement) return
        if (element is KtAnnotationEntry) {
            val context = element.analyzeAndGetResult().bindingContext
            val fqName = context.get(BindingContext.ANNOTATION, element)?.fqName
            //  TODO: Annotate string expressions inside @SData annotations

        }
    }
}
