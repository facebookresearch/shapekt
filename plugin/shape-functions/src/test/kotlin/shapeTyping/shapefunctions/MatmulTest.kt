/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import org.junit.Test
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.analysis.exceptions.STypeStrictModeException
import kotlin.test.assertEquals

class MatmulTest {
    @Test
    fun dimShapes() {
        val s1 = DimShape(3,2)
        val s2 = DimShape(2,4)

        val expected = DimShape(3,4)
        assertEquals(expected, matmul(s1, s2))
    }

    @Test
    fun dimShapesWithSymbol() {
        val a = SymbolicDim("A", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = DimShape(a, NumericDim(4))

        val expected = DimShape(3,4)
        assertEquals(expected, matmul(s1, s2))
    }

    @Test
    fun dimShapesWithPossiblyEqualSymbols() {
        val a = SymbolicDim("A", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val b = SymbolicDim("B", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = DimShape(b, NumericDim(4))

        val expected = DimShape(3,4)
        val result = matmul(s1, s2)
        assertEquals(expected, result)
        assert(result?.error is STypeStrictModeException)
    }

    @Test
    fun dimShapesWithPossiblyEqualSymbols2() {
        val common = SymbolicDim("C", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val a = SymbolicDim("A", DimDeclarationInfo(common, false, 0))
        val b = SymbolicDim("B", DimDeclarationInfo(common, false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = DimShape(b, NumericDim(4))

        val expected = DimShape(3,4)
        val result = matmul(s1, s2)
        assertEquals(expected, result)
        assert(result?.error is STypeStrictModeException)
    }

    @Test
    fun dimShapesWithConflictingSymbols() {
        val a = SymbolicDim("A", DimDeclarationInfo(NumericDim(3), false, 0))
        val b = SymbolicDim("B", DimDeclarationInfo(NumericDim(4), false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = DimShape(b, NumericDim(4))

        val result = matmul(s1, s2)
        assert(result is ErrorShape)
        assert(result?.error is STypeFailure)
    }

    @Test
    fun dimShapeWithWildcard1() {
        val a = SymbolicDim("A", DimDeclarationInfo(NumericDim(3), false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = WildcardShape.DEFAULT

        val result = matmul(s1, s2)
        assertEquals(DimShape(NumericDim(3), WildcardDim.DEFAULT), result)
        assert(result?.error is STypeStrictModeException)
    }

    @Test
    fun dimShapeWithWildcard2() {
        val a = SymbolicDim("A", DimDeclarationInfo(NumericDim(3), false, 0))
        val s1 = DimShape(NumericDim(3), a)
        val s2 = WildcardShape.DEFAULT

        val result = matmul(s2, s1)
        assertEquals(DimShape(WildcardDim.DEFAULT, a), result)
        assert(result?.error is STypeStrictModeException)
    }

}