/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package unreducedShapeFunction

import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import shapeTyping.plugin.RuntimeShape

object TensorOps {
    @SType("S: Shape")
    @AllowUnreduced
    fun idGradientShape(s: @SType("S") RuntimeShape): @SType("concat(S,S)") RuntimeShape = TODO()

    @SType("A: Shape") // Note: This can also be S, but using different names for debugger readability
    @AllowUnreduced
    fun callsIdGradientShape(s: @SType("A") RuntimeShape): @SType("concat(A,A)") RuntimeShape =
            idGradientShape(s)
}