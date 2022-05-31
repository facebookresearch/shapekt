/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.synthetic.parsing

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import shapeTyping.plugin.checkers.utils.DeclarationInfo

fun parseAndCheckDeclarations(
    containingDeclaration: DeclarationDescriptor?,
    annotation: AnnotationDescriptor,
    declarationInfoInScope: List<DeclarationInfo>
): DeclarationInfo {

    val arg = annotation.allValueArguments.toList().first().second.value as String

    val text = arg.removeSurrounding("\"")
    val lexStream = STypeLexer(text, ParseMode.DECLARATION).lex()

    val currScopeID = containingDeclaration.hashCode()
    val parser = Parser(lexStream, ParseMode.DECLARATION, declarationInfoInScope, currScopeID)

    parser.parse()

    return parser.declarationInfo!!
}