/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import org.junit.Test
import shapeTyping.analysis.*
import shapeTyping.analysis.exceptions.STypeFailure
import shapeTyping.analysis.exceptions.STypeStrictModeException
import kotlin.test.assertEquals

class ConcatOnAxisTest {

    @Test
    fun basicDimShapes() {
        val s1 = DimShape(3,4,2)
        val s2 = DimShape(2,4,2)

        val expected = DimShape(5,4,2)
        assertEquals(expected, concatOnAxis(s1, s2, NumericDim(0)))
    }

    @Test
    fun basicDimShapesIncompatible() {
        val s1 = DimShape(3,4,2)
        val s2 = DimShape(2,4,2)

        val result = concatOnAxis(s1, s2, NumericDim(1))
        assert(result is ErrorShape && result.error is STypeFailure)
    }

    @Test
    fun dimShapesWithSymbols() {
        val a = SymbolicDim("A", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val b = SymbolicDim("B", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val s1 = DimShape(a, NumericDim(9), NumericDim(2))
        val s2 = DimShape(NumericDim(3), NumericDim(4), b)

        val expected = DimShape(3,13,2)
        val result = concatOnAxis(s1, s2, NumericDim(1))
        assertEquals(expected, result)
        assert(result is DimShape)
        result as DimShape

        // Dims are inferred to the more specific dim where possible, but not a guaranteed match
        val e1 = result[0].error
        val e2 = result[2].error
        assert(e1 is STypeStrictModeException && e2 is STypeStrictModeException)
        assertEquals("concatOnAxis: Dim may not be exactly equal to 3", e1?.message)
        assertEquals("concatOnAxis: Dim may not be exactly equal to 2", e2?.message)

        assert(result[1].error == null)
    }

    @Test
    fun dimShapesWithWildcard() {
        val b = SymbolicDim("B", DimDeclarationInfo(WildcardDim.DEFAULT, false, 0))
        val s1 = DimShape(WildcardDim.DEFAULT, NumericDim(9), NumericDim(2))
        val s2 = DimShape(NumericDim(3), NumericDim(4), b)

        val expected = DimShape(3,13,2)
        val result = concatOnAxis(s1, s2, NumericDim(1))
        assertEquals(expected, result)
        assert(result is DimShape)
        result as DimShape

        // Dims are inferred to the more specific dim where possible, but not a guaranteed match
        val e1 = result[0].error
        val e2 = result[2].error
        assert(e1 is STypeStrictModeException && e2 is STypeStrictModeException)
        assertEquals("concatOnAxis: Dim may not be exactly equal to 3", e1?.message)
        assertEquals("concatOnAxis: Dim may not be exactly equal to 2", e2?.message)

        assert(result[1].error == null)
    }

    @Test
    fun dimShapesWithWildcardOnAxis() {
        val s1 = DimShape(WildcardDim.DEFAULT, NumericDim(9), NumericDim(2))

        val result = concatOnAxis(s1, s1, NumericDim(0))
        assertEquals(s1, result)
    }

}
