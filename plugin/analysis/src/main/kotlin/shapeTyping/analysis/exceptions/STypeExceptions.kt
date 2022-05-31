/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.analysis.exceptions

import shapeTyping.analysis.SType

/** Error levels recognized by shapeTyping diagnostics.
 *
 *  ERROR indicates that the shape check always fails given current constraints. This error is reported in all modes.
 *  WARNING indicates that the shapes may be valid for some inputs, but we cannot guarantee success at compile time.
 *          This error is reported in strict mode, but not non-strict.
 *  PROPAGATE indicates that the shape check cannot be completed due to an error in a sub-part.
 **/
enum class STypeErrorLevel {
    ERROR,
    WARNING,
    PROPAGATE
}

open class STypeException(override val message: String, val errorLevel: STypeErrorLevel) : Exception(message)

// TODO: Rewrite existing shape functions with these exceptions. For a quick fix I made them all STypeFailures, but this is likely wrong.

/* Exception which should only be thrown in case of an issue with the plugin or compiler. */
open class STypeCompilerException(override val message: String) : STypeException(message, STypeErrorLevel.ERROR)

/* Exception used in the compiler to streamline diagnostics for cascading errors */
open class STypeErrorPropagationException(exception: STypeException?): STypeException(exception?.message ?: "SType could not be fully resolved due to earlier error", STypeErrorLevel.PROPAGATE)

open class STypeFailure(override val message: String) : STypeException(message, STypeErrorLevel.ERROR)
open class STypeStrictModeException(override val message: String) : STypeException(message, STypeErrorLevel.WARNING)

open class UnsupportedSTypesException(fnName: String, vararg args: SType) : STypeFailure(
    "One or more illegal SType arguments for shape function $fnName: ${args.map { it.javaClass.name }}",
)

open class STypeParsingError(override val message: String) : STypeException(message, STypeErrorLevel.ERROR)
open class ShapeFunctionResolutionError(override val message: String) : STypeException(message, STypeErrorLevel.ERROR)

fun SType.propagatesError() = error?.errorLevel == STypeErrorLevel.PROPAGATE