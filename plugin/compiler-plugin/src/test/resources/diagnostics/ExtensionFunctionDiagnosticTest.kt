/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.SType
import shapeTyping.annotations.AllowUnreduced
import shapeTyping.plugin.Tensor
import shapeTyping.plugin.RuntimeShape

@SType("S1: Shape, S2: Shape")
fun (@SType("S1") Tensor).reshape(newShape: @SType("S2") RuntimeShape): @SType("S2") Tensor = Tensor(newShape)

fun reshapeTests() {
    val a = Tensor(RuntimeShape(1,2,3))
    val reshaped = a.reshape(RuntimeShape(2,3,4))
    val	b: @SType("[Dim,Dim,Dim]") Tensor = reshaped
    val c: @SType("[2,3,4]") Tensor = reshaped
    val d: @SType("[2,*,4]") Tensor = reshaped
    val e: @SType("[1,2,3]") Tensor = <!STYPE_MISMATCH_ERROR!>reshaped<!>
    val f: @SType("[2,3,4]") Tensor = <!STYPE_MISMATCH_ERROR!>b<!>
}

@SType("A: Dim, B <: Dim, C: Dim")
fun (@SType("[A,B]") Tensor).matmul(other: @SType("[B,C]") Tensor): @SType("[A,C]") Tensor = Tensor(RuntimeShape()) as @SType("[A,C]") Tensor

fun matmulTests() {
    val a = Tensor(RuntimeShape(2,3))
    val b = Tensor(RuntimeShape(3,4))
    val wrongRank = Tensor(RuntimeShape(3,4,3))
    val result: @SType("[2,4]") Tensor = a.matmul(b)
    a.matmul(<!STYPE_MISMATCH_ERROR!>wrongRank<!>)
    <!STYPE_MISMATCH_ERROR!>wrongRank<!>.matmul(b)
}