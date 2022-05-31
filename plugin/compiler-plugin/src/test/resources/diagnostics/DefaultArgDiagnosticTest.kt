/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package defaultArgs

import shapeTyping.annotations.SType

@SType("A: Dim, B: Dim")
class DimPair(val a: @SType("A") Int, val b: @SType("B") Int)

@SType("D1: Dim, D2: Dim")
fun f(x: @SType("D1") Int = 3, y: @SType("D2") Int = 4): @SType("D1,D2") DimPair {
    return DimPair(x, y)
}

val pair: @SType("3,4") DimPair = f()
val pairFail: @SType("3,3") DimPair = <!STYPE_MISMATCH_ERROR!>f()<!>