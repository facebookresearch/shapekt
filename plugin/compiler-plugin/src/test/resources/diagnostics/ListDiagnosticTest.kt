/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.SType
import shapeTyping.plugin.*

val tList = listOf(Tensor(RuntimeShape(2,3,4)), Tensor(RuntimeShape(2,3,5)))

val generalShape : List<@SType("Shape") Tensor> = tList
val leastUpperBound : List<@SType("[2,3,Dim]") Tensor> = tList

val notASupertype : List<@SType("Dim") Tensor> = <!STYPE_MISMATCH_ERROR!>tList<!>
val copyingLeftType : List<@SType("[2,3,4]") Tensor> = <!STYPE_MISMATCH_ERROR!>tList<!>
val copyingRightType : List<@SType("[2,3,5]") Tensor> = <!STYPE_MISMATCH_ERROR!>tList<!>

val x = tList[0]
val mostSpecificType: @SType("[2,3,Dim]") Tensor = x
val badAssignment: @SType("[2,3,4]") Tensor = <!STYPE_MISMATCH_ERROR!>x<!>