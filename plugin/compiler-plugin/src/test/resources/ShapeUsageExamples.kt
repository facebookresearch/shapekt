/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import shapeTyping.annotations.SType
import shapeTyping.plugin.Tensor

// demonstrates shape on a parameter type and on a return type
fun foo(a: @SType("[1]") Tensor): @SType("[1]") Tensor {
    val b: @SType("[1]") Tensor = a // demonstrates Shape on a val declaration's shape
    val c = b
    return c as @SType("[1]") Tensor // demonstrates Shape used in a cast
}