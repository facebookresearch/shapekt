/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.diagnostics

import shapeTyping.analysis.ShapeFunctionCall
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext

class ShapeTypingErrorMessages : DefaultErrorMessages.Extension {
    private val MAP =
            DiagnosticFactoryToRendererMap(
                    "ShapeTyping"
            )
    override fun getMap() = MAP

    init {
        MAP.put(
            ShapeTypingErrors.SHAPE_FUNCTION_ERROR,
            "Shape Function Error in {0}: {1}",
            ShapeFunctionCallRenderer,
            null
        )

        MAP.put(
            ShapeTypingErrors.UNRESOLVED_SHAPE_FUNCTION_ERROR,
            "Unresolved Shape Function: {0}",
            null
        )

        MAP.put(
            ShapeTypingErrors.INVALID_STYPE_ERROR,
            "Invalid SType: {0}",
            null
        )

        MAP.put(
            ShapeTypingErrors.STYPE_PARSING_ERROR,
            "SType Parsing Error: {0}",
            null
        )

        MAP.put(
            ShapeTypingErrors.STYPE_MISMATCH_ERROR,
            "Shape Mismatch Error: {0}",
            null
        )

        MAP.put(
            ShapeTypingErrors.DECLARE_PARAMS_ANNOTATION_ERROR,
            "Bad @DeclareParams input {0}.",
            null
        )

        MAP.put(
            ShapeTypingErrors.SHAPEORDIM_REDECLARATION_ERROR,
            "Redeclaration: conflicting declarations {0} and {1}.",
            null,
            null
        )

        MAP.put(
            ShapeTypingErrors.UNRESOLVED_SHAPEORDIM_REFERENCE_ERROR,
            "Unresolved ShapeOrDim reference: {0}.",
            null
        )

        MAP.put(
            ShapeTypingErrors.STYPE_VARIABLE_INFERENCE_ERROR,
            "Not enough information to infer ShapeOrDim variable {0}.",
            null
        )

        MAP.put(
            ShapeTypingErrors.UNREDUCED_STYPE_ERROR,
            "Unreduced SType {0}. To allow this, use the @AllowUnreduced annotation in an enclosing scope.",
            null
        )

        MAP.put(
            ShapeTypingErrors.UNCHECKED_SHAPE_CAST,
            "Not enough information to guarantee this cast will succeed."
        )

        MAP.put(
            ShapeTypingErrors.SHAPE_CAST_NEVER_SUCCEEDS,
            "This cast can never succeed."
        )
    }
}

object ShapeFunctionCallRenderer: DiagnosticParameterRenderer<ShapeFunctionCall> {
    override fun render(obj: ShapeFunctionCall, renderingContext: RenderingContext): String {
        return obj.toString()
    }

}
