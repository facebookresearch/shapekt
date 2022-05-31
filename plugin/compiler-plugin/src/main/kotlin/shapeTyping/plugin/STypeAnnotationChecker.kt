/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import shapeTyping.analysis.exceptions.STypeParsingError
import shapeTyping.plugin.diagnostics.ShapeTypingErrors
import shapeTyping.plugin.synthetic.parsing.ParseMode
import shapeTyping.plugin.synthetic.parsing.Parser
import shapeTyping.plugin.synthetic.parsing.STypeLexer
import shapeTyping.plugin.synthetic.parsing.parseAndCheckDeclarations

class STypeAnnotationChecker : AdditionalAnnotationChecker {

    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        // Not SType declaration mode; these will be checked through STypeChecker
        if (annotated is KtTypeReference) {
            return
        }

        // Otherwise, report any parsing errors from SType declarations
        entries.forEach { entry ->
            val annotation = trace.get(BindingContext.ANNOTATION, entry) ?: return@forEach
            if (annotation.fqName != ShapeTypingAnnotationFqNames.STYPE_FQNAME) return@forEach

            val arg = annotation.allValueArguments.toList().first().second.value as String
            val text = arg.removeSurrounding("\"")
            try {
                val lexStream = STypeLexer(text, ParseMode.DECLARATION).lex()
                Parser(lexStream, ParseMode.DECLARATION, emptyList(), null).parse()
            } catch (e: STypeParsingError) {
                trace.report(ShapeTypingErrors.STYPE_PARSING_ERROR.on(entry, e.message))
            }
        }
    }
}
