/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers.utils

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsByParametersWith
import shapeTyping.analysis.SType
import shapeTyping.analysis.exceptions.STypeErrorPropagationException
import shapeTyping.analysis.isError
import shapeTyping.plugin.STypeAttribute
import shapeTyping.plugin.checkers.STypeEvaluator
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSType
import shapeTyping.plugin.checkers.utils.AttributesUtils.getSTypeAttribute

fun KotlinType.containsSType(): Boolean = this.getSType() != null ||
        this.arguments.any { !it.isStarProjection && it.type.containsSType() }

private fun KotlinType.substituteAndEvaluateSingleSTypeAttribute(
    substitutionMap: Map<SType, SType>,
    propagationError: Boolean
): KotlinType {
    val sTypeAttribute = this.attributes.getSTypeAttribute() ?: return this
    val sType = sTypeAttribute.sType
    val newSTypeAttribute = when {
        (sType.isError()) -> sTypeAttribute
        else -> {
            val evaluatedSType = STypeEvaluator.evaluateSType(sType, substitutionMap).let {
                if (propagationError) it.withException(STypeErrorPropagationException(null)) else it
            }
            STypeAttribute(sTypeAttribute.annotationType, evaluatedSType)
        }
    }
    val newAttributes = this.attributes.filterNot { it === sTypeAttribute } + newSTypeAttribute
    return this.unwrap().replaceAttributes(TypeAttributes.create(newAttributes))
}

fun KotlinType.replaceSType(
    sType: SType,
    annotationType: KotlinType?,
): KotlinType {
    val sTypeAttribute = this.attributes.getSTypeAttribute()
    val annoType = sTypeAttribute?.annotationType ?: annotationType ?: return this

    val newSTypeAttribute = when {
        (sType.isError()) -> sTypeAttribute ?: return this
        else -> STypeAttribute(annoType, sType)
    }
    val newAttributes = this.attributes.filterNot { it === sTypeAttribute } + newSTypeAttribute
    return this.unwrap().replaceAttributes(TypeAttributes.create(newAttributes))
}

fun KotlinType.substituteAndEvaluateSTypeAttribute(
    substitutionMap: Map<SType, SType>,
    propagationError: Boolean
): KotlinType {
    val newArgs = this.arguments.map { typeArg ->
        when (typeArg) {
            is StarProjectionImpl -> typeArg
            is TypeProjectionImpl -> TypeProjectionImpl(typeArg.type.substituteAndEvaluateSTypeAttribute(substitutionMap, propagationError))
            else -> typeArg
        }
    }
    val withNewArgs = this.replaceArgumentsByParametersWith { newArgs[it.index] }
    return withNewArgs.substituteAndEvaluateSingleSTypeAttribute(substitutionMap, propagationError)
}