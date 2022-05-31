/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package playground

import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType


@SType("S: Shape")
class RuntimeShape(val dims : List<Int>) {
    constructor(vararg dims : @SType("S") Int) : this(dims.toList())
}

@SType("S: Shape")
@AllowUnreduced
class Tensor(val shape: @SType("S") RuntimeShape) {
    companion object {
        @SType("S: Shape")
        fun zeros(shape: @SType("S") RuntimeShape): @SType("S") Tensor = Tensor(shape)
    }
}