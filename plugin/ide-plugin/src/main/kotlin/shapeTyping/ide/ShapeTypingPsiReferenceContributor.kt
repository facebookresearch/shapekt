/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.ide

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.references.canBeReferenceTo
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression


class ShapeTypingPsiReferenceContributor : PsiReferenceContributor() {
    class ShapeFunctionReference(private val expression: KtStringTemplateExpression, private val function: KtNamedFunction, private val range: TextRange) : PsiReference {
        override fun getElement(): PsiElement = expression

        override fun getRangeInElement(): TextRange = range

        override fun resolve(): PsiElement = function

        override fun getCanonicalText(): String = expression.text

        // TODO: Properly handle this case.
        override fun handleElementRename(newElementName: String): PsiElement = expression

        // TODO: Properly handle this case.
        override fun bindToElement(element: PsiElement): PsiElement = element

        override fun isReferenceTo(target: PsiElement): Boolean = canBeReferenceTo(target)

        override fun isSoft(): Boolean = false

    }

    object ShapeTypingReferenceProvider : PsiReferenceProvider() {

        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            if (element !is KtStringTemplateExpression) return PsiReference.EMPTY_ARRAY

            // TODO: Return an Array of ShapeFunctionReferences for the given KtStringTemplateExpression.
            return PsiReference.EMPTY_ARRAY
        }
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(), ShapeTypingReferenceProvider)
    }
}
