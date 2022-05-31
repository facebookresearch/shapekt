/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package callShadowingDiagnostics

import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import shapeTyping.plugin.RuntimeShape

@SType("A: Shape, B: Shape")
fun concat(a: @SType("A") RuntimeShape, b: @SType("B") RuntimeShape): @SType("concat(A,B)") RuntimeShape {
    TODO()
}

@AllowUnreduced
fun doubleConcat(a: RuntimeShape, b: RuntimeShape): RuntimeShape {
    return concat(concat(a,b),b)
}