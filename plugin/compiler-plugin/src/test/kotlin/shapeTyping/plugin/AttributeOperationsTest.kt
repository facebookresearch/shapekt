/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.junit.Test
import shapeTyping.analysis.*
import shapeTyping.plugin.checkers.utils.AttributesUtils.union
import kotlin.test.assertEquals

class AttributeOperationsTest {

    @Test
    fun testSimpleDisjointUnion() {
        val a = basicShapeSymbol("A")
        val b = basicShapeSymbol("B")
        assertEquals(WildcardShape.DEFAULT, a.union(b))
    }

    @Test
    fun testUnionSameTypeSymbolic() {
        val a = basicShapeSymbol("A")
        assertEquals(a, a.union(a))
    }

    @Test
    fun testUnionSameType() {
        val a = basicShapeSymbol("A")
        assertEquals(a, a.union(a))
    }

    @Test
    fun testSimpleSubtypeUnion() {
        val a = basicShapeSymbol("A")
        val b = basicShapeSymbol("B", a)
        assertEquals(a, a.union(b))
    }

    @Test
    fun testSharedSupertype() {
        val a = basicShapeSymbol("A")
        val b = basicShapeSymbol("B", a)
        val c = basicShapeSymbol("C", b)
        val d = basicShapeSymbol("D", a)
        assertEquals(a, c.union(d))
    }

    @Test
    fun testSimpleDimShapeUnion() {
        val s1 = DimShape(basicDimSymbol("A"), NumericDim(2))
        val s2 = DimShape(basicDimSymbol("B"), NumericDim(2))
        assertEquals(DimShape(WildcardDim.DEFAULT, NumericDim(2)), s1.union(s2))
    }

    @Test
    fun testTupleUnion() {
        val s1 = DimShape(basicDimSymbol("S1"), NumericDim(2))
        val s2 = DimShape(basicDimSymbol("2"), NumericDim(2))
        val a = basicShapeSymbol("A")
        val b = basicShapeSymbol("B", a)
        val t1 = STypeTuple(listOf(s1, a))
        val t2 = STypeTuple(listOf(s2, b))

        assertEquals(STypeTuple(listOf(DimShape(WildcardDim.DEFAULT, NumericDim(2)), a)), t1.union(t2))
    }
}