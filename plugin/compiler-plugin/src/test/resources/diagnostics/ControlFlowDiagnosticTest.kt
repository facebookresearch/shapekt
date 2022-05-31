/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package controlFlowDiagnostic

import shapeTyping.annotations.AllowUnreduced
import shapeTyping.annotations.SType
import kotlin.random.Random

// TODO (#81): Resolve STypes to most general type for nullable args when null and re-enable ?: and !! test examples

@SType("S: Shape")
class RuntimeShape(val dims : List<Int>) {
    constructor(vararg dims : @SType("S") Int) : this(dims.toList())
    fun isScalar(): Boolean = dims.isEmpty()
}

@SType("S: Shape")
fun ifTest(shape: @SType("S") RuntimeShape): @SType("S") RuntimeShape = if (shape.isScalar()) shape else throw Exception()

@SType("S: Shape")
fun whenTest(shape: @SType("S") RuntimeShape): @SType("S") RuntimeShape = when {
    shape.isScalar() -> shape
    shape.dims.size > 4 -> shape
    else -> throw Exception()
}

@SType("S: Shape")
fun elvisTest(shape: @SType("S") RuntimeShape?): @SType("S") RuntimeShape = shape ?: RuntimeShape()

@SType("S: Shape")
fun exclExclTest(shape: @SType("S") RuntimeShape?): @SType("S") RuntimeShape = shape!!

val unionCheck: @SType("[2,Dim]") RuntimeShape = if (Random.nextInt() > 10) RuntimeShape(2,3) else RuntimeShape(2,2)

val ifTestSuccess: @SType("[]") RuntimeShape = ifTest(RuntimeShape())
val ifTestFail: @SType("[2,2]") RuntimeShape = <!STYPE_MISMATCH_ERROR!>ifTest(RuntimeShape())<!>

val whenTestSuccess1: @SType("[2,2,2,2]") RuntimeShape = whenTest(RuntimeShape(2,2,2,2))
val whenTestSuccess2: @SType("[]") RuntimeShape = whenTest(RuntimeShape())
val whenTestFailure1: @SType("[2,2]") RuntimeShape = <!STYPE_MISMATCH_ERROR!>whenTest(RuntimeShape(2,2,2,2))<!>

val elvisSuccess1: @SType("[2]") RuntimeShape = elvisTest(RuntimeShape(2))
// val elvisSuccess2: @SType("Shape") RuntimeShape? = elvisTest(null)
val elvisFailure1: @SType("[2,2]") RuntimeShape = <!STYPE_MISMATCH_ERROR!>elvisTest(RuntimeShape(2))<!>
val elvisFailure2: @SType("[]") RuntimeShape? = <!STYPE_MISMATCH_ERROR!>elvisTest(null)<!>

val exclExclSuccess1: @SType("[2]") RuntimeShape = exclExclTest(RuntimeShape(2))
// val exclExclSuccess2: @SType("Shape") RuntimeShape? = exclExclTest(null)
val exclExclFailure1: @SType("[2,2]") RuntimeShape = <!STYPE_MISMATCH_ERROR!>exclExclTest(RuntimeShape(2))<!>
val exclExclFailure2: @SType("[]") RuntimeShape? = <!STYPE_MISMATCH_ERROR!>exclExclTest(null)<!>