/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics.parsing

import shapeTyping.annotations.SType
import shapeTyping.plugin.*

val badTypeRef : <!STYPE_PARSING_ERROR!>@SType("w/w")<!> Tensor = Tensor(RuntimeShape(2,3,4))
val undefinedShapeFunction: <!UNRESOLVED_SHAPE_FUNCTION_ERROR!>@SType("noFunction([2,3,4])")<!> Tensor = Tensor(RuntimeShape(2,3,4))

<!STYPE_PARSING_ERROR!>@SType("S//")<!>
fun malformedDeclaration(): Nothing = TODO()

