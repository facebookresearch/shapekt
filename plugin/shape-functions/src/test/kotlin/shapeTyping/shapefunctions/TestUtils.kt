/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.ErrorDim
import shapeTyping.analysis.ErrorShape
import shapeTyping.analysis.ErrorUnknownClass
import shapeTyping.analysis.SType
import shapeTyping.analysis.exceptions.STypeErrorLevel
import shapeTyping.analysis.exceptions.STypeException

// TODO: These (after cleanup/evaluation) should be made available to third-party shape function writers

inline fun <reified T : STypeException> assertFailsWithSTypeError(message: String? = null, block: () -> SType?): T {
    val result = block()
    val error = when (result) {
        is ErrorShape, is ErrorDim, is ErrorUnknownClass -> result.error
        else -> throw AssertionError("Expected to fail with error $message, instead evaluated successfully")
    }
    when {
        (error == null) ->
            if (message != null) throw AssertionError("Expected error with message $message, instead got default error")
        (error !is T) ->
            throw AssertionError("Got error of class ${error.javaClass} instead of expected type ${T::class}")
        (error.errorLevel != STypeErrorLevel.ERROR) ->
            throw AssertionError("Expected to fail with error $message, instead got warning/strict error ${error.message}")
        (error.message != message) ->
            throw AssertionError("Expected to fail with error $message, instead got message ${error.message}")
    }
    return error!! as T
}