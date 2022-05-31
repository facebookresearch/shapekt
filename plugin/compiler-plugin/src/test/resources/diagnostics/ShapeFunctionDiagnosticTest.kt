/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package diagnostics

import shapeTyping.annotations.SType
import shapeTyping.plugin.Tensor

fun correctNestedShapeFnCall() {
    fun foo() : @SType("matmul(matmul([2, 1], [1, 2]), [2,1])") Tensor = TODO()
    val result = foo()
}

// TODO: Look into highlighting just the inner matmul here once the AST offsets are passed
fun nestedShapeFnCallErrorInInner() {
    fun foo() : <!SHAPE_FUNCTION_ERROR!>@SType("matmul(matmul([2, 3], [1, 2]), [2,1])")<!> Tensor = TODO()
    val result = <!INVALID_STYPE_ERROR!>foo()<!>
}

fun nestedShapeFnCallErrorInOuter() {
    fun foo() : <!SHAPE_FUNCTION_ERROR!>@SType("matmul(matmul([2, 1], [1, 2]), [3,1])")<!> Tensor = TODO()
    val result = <!INVALID_STYPE_ERROR!>foo()<!>
}