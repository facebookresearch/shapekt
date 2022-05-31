/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.SType
import shapeTyping.annotations.AllowUnreduced
import shapeTyping.plugin.RuntimeShape

@SType("S: Shape")
class Tensor(val shape: @SType("S") RuntimeShape = RuntimeShape(listOf())) {
    fun id(): @SType("S") Tensor = this
    fun singletonList(): List<@SType("S") Tensor> = listOf(this)
}

// TODO: Get this to work as a subclass of Tensor! e.g. @SData("A: Dim, B:Dim") Matrix(...) : @SData("[A,B]") Tensor
@SType("A: Dim, B: Dim")
class Matrix(val rows: @SType("A") Int, val cols: @SType("B") Int) {
    @SType("C: Dim")
    fun matmul(other: @SType("B, C") Matrix): @SType("A,C") Matrix = TODO()
}

fun identityMethodTests() {
    val a = Tensor(RuntimeShape(2, 3))
    val pass1: @SType("[2,3]") Tensor = a.id()
    val pass2: @SType("[2,*]") Tensor = a.id()
    val fail1: @SType("[Dim]") Tensor = <!STYPE_MISMATCH_ERROR!>a.id()<!>
    val fail2: @SType("[2,4]") Tensor = <!STYPE_MISMATCH_ERROR!>a.id()<!>
}

fun matmulTests() {
    val a = Matrix(2,3)
    val b1 = Matrix(3,4)
    val pass = a.matmul(b1)
    val fail = a.matmul(<!STYPE_MISMATCH_ERROR!>a<!>)
}

fun singletonTests() {
    val a = Tensor(RuntimeShape(2, 3))
    val shouldPass: @SType("[2,3]") Tensor = a.singletonList().first()
    val shouldFail1: @SType("[4,3]") Tensor = <!STYPE_MISMATCH_ERROR!>a.singletonList().first()<!>
    val shouldFail2: @SType("[Dim,Dim,Dim]") Tensor = <!STYPE_MISMATCH_ERROR!>a.singletonList().first()<!>
    val shouldPassAsList: List<@SType("[2,3]") Tensor> = a.singletonList()
    val shouldFailAsList1: List<@SType("[4,3]") Tensor> = <!STYPE_MISMATCH_ERROR!>a.singletonList()<!>
    val shouldFailAsList2: List<@SType("[Dim,Dim,Dim]") Tensor> = <!STYPE_MISMATCH_ERROR!>a.singletonList()<!>
}