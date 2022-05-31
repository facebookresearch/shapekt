/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.shapefunctions

import shapeTyping.analysis.*
import org.junit.Test
import kotlin.test.assertEquals

class ConcatTest {
    @Test
    fun dimShapes() {
        val s1 = DimShape(1,2)
        val s2 = DimShape(3,4,5)

        val expected = DimShape(1,2,3,4,5)
        assertEquals(expected, concat(s1, s2))
    }

    @Test
    fun wildcardShape() {
        val s1 = WildcardShape.DEFAULT
        val s2 = DimShape(3,4,5)
        assert(concat(s1, s2) is WildcardShape)
    }

    @Test
    fun symbolicShape() {
        val s1 = SymbolicShape("A", ShapeDeclarationInfo(WildcardShape.DEFAULT, false, 1))
        val s2 = DimShape(3,4,5)

        assertEquals(null, concat(s1, s2))
    }

    @Test
    fun shapeFunctionCall() {
        val s1 = ShapeFunctionCall("f", listOf(WildcardShape.DEFAULT))
        val s2 = DimShape(3,4,5)

        assertEquals(null, concat(s1, s2))
    }
}