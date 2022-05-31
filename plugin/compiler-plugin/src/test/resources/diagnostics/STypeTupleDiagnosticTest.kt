/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.SType
import shapeTyping.plugin.RuntimeShape

@SType("S1: Shape, S2: Shape")
class TwoTensor(val s1: @SType("S1") RuntimeShape, val s2: @SType("S2") RuntimeShape)

fun matmulFirstBasicTest() {

    fun matmulFirst(
        a: @SType("[1,2], [2]") TwoTensor,
        b: @SType("[2,1], [2]") TwoTensor
    ) : @SType("matmul([1,2], [2,1]), [2]") TwoTensor = TODO()

    val a = TwoTensor(RuntimeShape(1, 2), RuntimeShape(2))
    val b = TwoTensor(RuntimeShape(2, 1), RuntimeShape(2))
    val result = matmulFirst(a, b)
}

fun matmulFirstGenericsTest() {

    @SType("A: Dim, B: Dim, C: Dim")
    fun matmulFirst(
        a: @SType("[A,B], [B]") TwoTensor,
        b: @SType("[B,C], [B]") TwoTensor
    ) : @SType("matmul([A,B], [B,C]), [B]") TwoTensor = TODO()

    val a = TwoTensor(RuntimeShape(1, 2), RuntimeShape(2))
    val b = TwoTensor(RuntimeShape(2, 1), RuntimeShape(2))
    val result: @SType("[1,1], [2]") TwoTensor = matmulFirst(a, b)
    val badResult: @SType("[3,1], [2]") TwoTensor = <!STYPE_MISMATCH_ERROR!>matmulFirst(a, b)<!>
}

fun matmulBadInferenceAcrossGenericsTest() {

    @SType("A: Dim, B: Dim, C: Dim")
    fun matmulFirst(
        a: @SType("[A,B], [B]") TwoTensor,
        b: @SType("[B,C], [B]") TwoTensor
    ) : @SType("matmul([A,B], [B,C]), [B]") TwoTensor = TODO()

    val a = TwoTensor(RuntimeShape(1, 2), RuntimeShape(2))
    val b = TwoTensor(RuntimeShape(3, 1), RuntimeShape(2))
    val result = matmulFirst(a, <!STYPE_MISMATCH_ERROR!>b<!>)
}
