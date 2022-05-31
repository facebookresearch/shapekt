/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeConstructor
import shapeTyping.analysis.utils.serialize
import org.jetbrains.kotlin.extensions.TypeAttributeTranslatorExtension
import shapeTyping.analysis.ErrorUnknownClass
import shapeTyping.analysis.exceptions.STypeParsingError
import shapeTyping.plugin.ShapeTypingAnnotationFqNames.STYPE_FQNAME
import shapeTyping.plugin.checkers.STypeEvaluator
import shapeTyping.plugin.synthetic.parsing.*


class STypeAttributeTranslator : TypeAttributeTranslatorExtension {
    private fun STypeAttribute.parseToString(): String = this.sType.serialize()

    override fun toAnnotations(attributes: TypeAttributes): Annotations {
        val attribute = attributes.find { it is STypeAttribute } as STypeAttribute? ?: return Annotations.EMPTY
        return Annotations.create(
            listOf(
                AnnotationDescriptorImpl(
                    attribute.annotationType,
                    mapOf(Name.identifier("value") to StringValue(attribute.parseToString())),
                    SourceElement.NO_SOURCE
                )
            )
        )
    }

    override fun toAttributes(
        annotations: Annotations,
        typeConstructor: TypeConstructor?,
        containingDeclaration: DeclarationDescriptor?
    ): TypeAttributes {
        // TODO: If type is shaped but not explicitly annotated, add appropriate wildcard attribute here (hardcode to tensor or see #22).
        // TODO: use typeConstructor to check declared params for types, related to above
        val sDataAnnotation = annotations.findAnnotation(STYPE_FQNAME) ?: return TypeAttributes.Empty
        val string = sDataAnnotation.allValueArguments.values.first().value as String

        val sTypeDeclarations = containingDeclaration?.parentsWithSelf?.toList()?.mapNotNull {
            it.annotations.findAnnotation(STYPE_FQNAME)?.let { annotation ->
                try {
                    parseAndCheckDeclarations(it, annotation, emptyList())
                } catch (e: STypeParsingError) {
                    // These errors will be reported by STypeAnnotationChecker, so suppress them here
                    null
                }
            }
        } ?: emptyList()
        val sType = try {
            Parser.parseSTypeArguments(string, containingDeclaration, sTypeDeclarations).toSType()
        } catch (e: Exception) {
            ErrorUnknownClass(STypeParsingError(e.message ?: "Could not parse argument to @SType"))
        }
        // TODO: May take substitutions from enclosing scope?
        val eval = STypeEvaluator.evaluateSType(sType, emptyMap())
        return TypeAttributes.create(listOf(STypeAttribute(sDataAnnotation.type, eval)))
    }

}