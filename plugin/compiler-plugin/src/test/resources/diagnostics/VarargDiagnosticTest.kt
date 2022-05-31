/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package varargDiagnostics

import shapeTyping.annotations.*

@SType("S: Shape")
class DimShape(vararg val a: @SType("S") Int)

@SType("D: Dim")
class DimList(vararg val a: @SType("D") Int)

val dimsToShapePass: @SType("[2,3,4]") DimShape = DimShape(2,3,4)
val dimsToShapeFail: @SType("[2,3,4,5]") DimShape = <!STYPE_MISMATCH_ERROR!>DimShape(2,3,4)<!>

val dimsToListPass: @SType("Dim") DimList = DimList(2,3,4)
val dimsToListFail: @SType("2") DimList = <!STYPE_MISMATCH_ERROR!>DimList(2,3,4)<!>

@SType("S: Shape")
fun collectShapesToList(vararg s: @SType("S") DimShape): List<@SType("S") DimShape> {
    return s.toList()
}

val collectShapesPass: List<@SType("[2,Dim]") DimShape> = collectShapesToList(DimShape(2,3), DimShape(2,4))
val collectShapesFail1: List<@SType("[2,3]") DimShape> = <!STYPE_MISMATCH_ERROR!>collectShapesToList(DimShape(2,3), DimShape(2,4))<!>
val collectShapesFail2: List<@SType("[2,4]") DimShape> = <!STYPE_MISMATCH_ERROR!>collectShapesToList(DimShape(2,3), DimShape(2,4))<!>
