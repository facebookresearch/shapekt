/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.checkers

import shapeTyping.analysis.*
import shapeTyping.analysis.utils.*
import shapeTyping.plugin.analysis.evaluateShapeFunctionCall

object STypeEvaluator {

    fun evaluateSType(
        shape: SType,
        substitutions: Map<SType, SType> = mapOf()
    ): SType {
        val substitutedType =
            if (shape.containsSymbols() && substitutions.isNotEmpty()) shape.substitute(substitutions) else shape
        return when (substitutedType) {
            is ShapeFunctionCall -> {
                val evaluatedArgs = substitutedType.args.map { evaluateSType(it, substitutions) }
                evaluateShapeFunctionCall(ShapeFunctionCall(substitutedType.name, evaluatedArgs))
            }
            is STypeTuple -> substitutedType.map { evaluateSType(it, substitutions) }
            else -> substitutedType
        }
    }
}