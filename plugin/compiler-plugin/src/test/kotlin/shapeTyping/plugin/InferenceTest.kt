/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.junit.Test
import shapeTyping.analysis.*
import shapeTyping.plugin.checkers.utils.inferArgAgainstConstraint
import kotlin.test.assertEquals

class InferenceTest {

    @Test
    fun testSimpleShapeSubstitution() {
        val a = basicShapeSymbol("A")
        val b = basicShapeSymbol("B", a)
        val substitutions = inferArgAgainstConstraint(b, a, emptyMap())
        assertEquals<Map<SType, SType>?>(substitutions, mapOf(a to b))
    }

    @Test
    fun testSimpleDimSubstitution() {
        val a = basicDimSymbol("A")
        val b = basicDimSymbol("B", a)
        val substitutions = inferArgAgainstConstraint(b, a, emptyMap())
        assertEquals<Map<SType, SType>?>(substitutions, mapOf(a to b))
    }

    @Test
    fun testDimSubstitutionIntoShape() {
        val a = basicDimSymbol("A")
        val b = basicDimSymbol("B", a)
        val shape1 = DimShape(listOf(b, NumericDim(2)))
        val shape2 = DimShape(listOf(a, WildcardDim.DEFAULT))
        val substitutions = inferArgAgainstConstraint(shape1, shape2, emptyMap())
        assertEquals<Map<SType, SType>?>(substitutions, mapOf(a to b))
    }

    @Test
    fun inferSTypeTuple() {
        val a = basicDimSymbol("A")
        val b = basicDimSymbol("B")
        val c = basicDimSymbol("C")
        val sd1 = STypeTuple(listOf(a, DimShape(b, WildcardDim.DEFAULT)))
        val sd2 = STypeTuple(listOf(NumericDim(2), DimShape(NumericDim(3), c)))
        val substitutions = inferArgAgainstConstraint(sd2, sd1, emptyMap())
        assertEquals<Map<SType, SType>?>(substitutions, mapOf(a to NumericDim(2), b to NumericDim(3)))
    }

    @Test
    fun inferMatmul() {
        val a = basicDimSymbol("A")
        val b = basicDimSymbol("B")
        val c = basicDimSymbol("C")
        val s1 = DimShape(NumericDim(2), NumericDim(3))
        val s2 = DimShape(NumericDim(3), NumericDim(4))
        val constraint1 = DimShape(a, b)
        val constraint2 = DimShape(b, c)

        val firstSubstitutions = inferArgAgainstConstraint(s1, constraint1, emptyMap())
        assertEquals<Map<SType, SType>?>(firstSubstitutions, mapOf(a to NumericDim(2), b to NumericDim(3)))

        val substitutions = inferArgAgainstConstraint(s2, constraint2, firstSubstitutions!!)
        assertEquals<Map<SType, SType>?>(substitutions, mapOf(
            a to NumericDim(2),
            b to NumericDim(3),
            c to NumericDim(4)
        ))
    }

    @Test
    fun inferMatmulConflict() {
        val a = basicDimSymbol("A")
        val b = basicDimSymbol("B")
        val c = basicDimSymbol("C")
        val s1 = DimShape(NumericDim(2), NumericDim(3))
        val s2 = DimShape(NumericDim(4), NumericDim(4))
        val constraint1 = DimShape(a, b)
        val constraint2 = DimShape(b, c)

        val firstSubstitutions = inferArgAgainstConstraint(s1, constraint1, emptyMap())
        assertEquals<Map<SType, SType>?>(firstSubstitutions, mapOf(a to NumericDim(2), b to NumericDim(3)))

        val substitutions = inferArgAgainstConstraint(s2, constraint2, firstSubstitutions!!)
        assertEquals(substitutions, null)
    }
}