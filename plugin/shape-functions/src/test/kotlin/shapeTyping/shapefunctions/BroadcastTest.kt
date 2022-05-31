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
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BroadcastTest {
    private val dummyScopeID = 1

    private fun basicShapeSymbol(
        symbol: String,
        bound: Shape = WildcardShape.DEFAULT,
        scopeId: Int = dummyScopeID
    ): SymbolicShape = SymbolicShape(
        symbol,
        ShapeDeclarationInfo(bound, false, scopeId)
    )

    private fun basicDimSymbol(symbol: String, bound: Dim = WildcardDim.DEFAULT): SymbolicDim = SymbolicDim(
        symbol,
        DimDeclarationInfo(bound, false, dummyScopeID)
    )

    @Test
    fun dimShapesBroadcastable() {
        val s1 = DimShape(1, 3)
        val s2 = DimShape(5, 5, 1)

        val expected = DimShape(5, 5, 3)
        assertEquals(expected, broadcast(s1, s2))
    }

    @Test
    fun dimShapesNonReduceable() { // broadcast([D1], [D2])
        val d1 = basicDimSymbol("D1")
        val d2 = basicDimSymbol("D2")
        val s1 = DimShape(d1)
        val s2 = DimShape(d2)
        assertEquals(null, broadcast(s1, s2))
    }

    @Test
    fun dimShapesIncompatible() { // broadcast([2], [3])
        val s1 = DimShape(2)
        val s2 = DimShape(3)
        assertFailsWithSTypeError<STypeFailure>("Cannot broadcast [2] and [3] with incompatible dims 2 and 3") {
            broadcast(s1, s2)
        }
    }

    @Test
    fun dimShapesNonreduceableAndIncompatible() {
        // If we find that the shapes are not reduceable, we continue checking to report any definite failures
        val d1 = basicDimSymbol("D1")
        val d2 = basicDimSymbol("D2")
        // we know the result is not reduceable when we compare d1 and d2, but we continue and check the next dims
        // (2 and 3) which are incompatible
        val s1 = DimShape(d1, NumericDim(2))
        val s2 = DimShape(d2, NumericDim(3))
        assertFailsWithSTypeError<STypeFailure>("Cannot broadcast [D1: Dim, 2] and [D2: Dim, 3] with incompatible dims 2 and 3") {
            broadcast(s1, s2)
        }
    }

    @Test
    fun wildcardShapes() {
        assertIs<WildcardShape>(broadcast(WildcardShape.DEFAULT, WildcardShape.DEFAULT))
    }

    @Test
    fun equalSymbolicShapes() {
        val s = basicShapeSymbol("S")
        assertEquals(s, broadcast(s, s))
    }

    @Test
    fun differentSymbolicShapes() {
        val s1 = basicShapeSymbol("S")
        val s2 = basicShapeSymbol("S", scopeId = dummyScopeID + 1)
        assertEquals(null, broadcast(s1, s2))
    }

    @Test
    fun equalShapeFnCalls() {
        val s1 = ShapeFunctionCall(
            "Dummy",
            listOf(basicShapeSymbol("S"))
        )
        assertEquals(s1, broadcast(s1, s1))
    }

    @Test
    fun differentShapeFnCalls() {
        val s1 = ShapeFunctionCall(
            "Dummy",
            listOf(basicShapeSymbol("S1"))
        )
        val s2 = ShapeFunctionCall(
            "Dummy",
            listOf(basicShapeSymbol("S2"))
        )
        assertEquals(null, broadcast(s1, s2))
    }
}
