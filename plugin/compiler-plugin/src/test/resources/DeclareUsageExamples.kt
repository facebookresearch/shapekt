/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import shapeTyping.annotations.SType
import shapeTyping.plugin.Tensor

// declaration on function
@SType("A: [6], B: *, C: [B, B]")
fun bar(a: @SType("[B]") Tensor): @SType("A") Tensor {
    return a as @SType("A") Tensor
}

// declaration on class
@SType("S: Shape, D: Dim")
class Clazz(a: @SType("S") Tensor) {}

// nested-scope declarations
@SType("S: Shape")
class ClassWithMember(val t: @SType("S") Tensor) {
    fun member1(): @SType("S") Tensor = t // should use S from the class

    @SType("A: Dim, B: Dim, C: Dim, S: [A, B, C]")
    fun member2(s: @SType("S") Tensor): @SType("S") Tensor = TODO()  // should use the local S
}