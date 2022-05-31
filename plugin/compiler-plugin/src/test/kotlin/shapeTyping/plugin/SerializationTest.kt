/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.junit.Test
import shapeTyping.analysis.*
import shapeTyping.analysis.utils.serialize
import kotlin.test.assertEquals

class SerializationTest {

    @Test
    fun testNumericSerialization() {
        val a = DimShape(NumericDim(3), NumericDim(4), NumericDim(1))
        assertEquals(a.serialize(), "[3,4,1]")
    }

    @Test
    fun testMixedNumericSymbolicDimShape() {
        val a = DimShape(NumericDim(3), basicDimSymbol("D"), NumericDim(1))
        assertEquals(a.serialize(), "[3,D,1]")
    }

    @Test
    fun testShapeFunctionCall() {
        val call = ShapeFunctionCall("matmul", listOf(
            DimShape(basicDimSymbol("A"), NumericDim(4)),
            DimShape(NumericDim(4), basicDimSymbol("B"))
        ))
        assertEquals(call.serialize(), "matmul([A,4],[4,B])")
    }

    @Test
    fun testSTypeTuple() {
        val dimShape = DimShape(NumericDim(3), basicDimSymbol("D"), NumericDim(1))
        val call = ShapeFunctionCall("matmul", listOf(
            DimShape(basicDimSymbol("A"), NumericDim(4)),
            DimShape(NumericDim(4), basicDimSymbol("B"))
        ))
        val symbolDim = basicDimSymbol("C")
        val numDim = NumericDim(4)
        val tuple = STypeTuple(listOf(dimShape, call, symbolDim, numDim))
        assertEquals(tuple.serialize(), "[3,D,1],matmul([A,4],[4,B]),C,4")
    }

}