/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package memberDiagnostics

import shapeTyping.annotations.SType
import shapeTyping.plugin.RuntimeShape

@SType("S: Shape")
class Tensor(val shape: @SType("S") RuntimeShape) {
    val member: @SType("S") Tensor = this
    val getMember: @SType("S") Tensor get() = this
}

fun fieldMemberTest() {
    val a = Tensor(RuntimeShape(2, 3))
    val memberPass1: @SType("[2,3]") Tensor = a.member
    val memberPass2: @SType("[Dim,Dim]") Tensor = a.member
    val memberFail1: @SType("[4,3]") Tensor = <!STYPE_MISMATCH_ERROR!>a.member<!>
    val memberFail2: @SType("[Dim,Dim,Dim]") Tensor = <!STYPE_MISMATCH_ERROR!>a.member<!>
}

fun getMemberTest() {
    val a = Tensor(RuntimeShape(2, 3))
    val getMemberPass1: @SType("[2,3]") Tensor = a.getMember
    val getMemberPass2: @SType("[Dim,Dim]") Tensor = a.getMember
    val getMemberFail1: @SType("[4,3]") Tensor = <!STYPE_MISMATCH_ERROR!>a.getMember<!>
    val getMemberFail2: @SType("[Dim,Dim,Dim]") Tensor = <!STYPE_MISMATCH_ERROR!>a.getMember<!>
}