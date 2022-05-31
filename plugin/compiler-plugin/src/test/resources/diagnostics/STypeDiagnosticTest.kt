/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.*
import shapeTyping.plugin.Tensor
import shapeTyping.plugin.RuntimeShape

@Suppress("USELESS_CAST")
fun foo(a: @SType("[1,2,3]") Tensor, b: @SType("[Dim,Dim]") Tensor): @SType("Shape") Tensor = a as @SType("Shape") Tensor

@SType("S1: Shape, S2: Shape")
@AllowUnreduced
fun reshape(x: @SType("S1") Tensor, newShape: @SType("S2") RuntimeShape): @SType("S2") Tensor = Tensor(newShape)

val empty: @SType("[]") Tensor = Tensor(RuntimeShape())

fun main() {
    val a = Tensor(RuntimeShape(1,2,3))
    val	b: @SType("[Dim,Dim,Dim]") Tensor =  a
    val c: @SType("[1,2,3]") Tensor = a
    val d: @SType("Shape") Tensor = a
    val e = foo(a, <!STYPE_MISMATCH_ERROR!>a<!>)
    val f: @SType("[1,2,3]") Tensor = <!STYPE_MISMATCH_ERROR!>e<!>
}