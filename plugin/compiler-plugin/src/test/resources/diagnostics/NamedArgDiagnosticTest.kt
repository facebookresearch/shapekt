/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package namedArgDiagnostics

import shapeTyping.annotations.SType
import shapeTyping.annotations.AllowUnreduced

@SType("A: Dim, B: Dim")
class DimPair(val a: @SType("A") Int, val b: @SType("B") Int)

@SType("D1: Dim, D2: Dim")
@AllowUnreduced
fun f(x: @SType("D1") Int, y: @SType("D2") Int): @SType("D1,D2") DimPair {
    return DimPair(x, y)
}

val pair: @SType("3,4") DimPair = f(3,4)
val pairFail: @SType("3,3") DimPair = <!STYPE_MISMATCH_ERROR!>f(3,4)<!>