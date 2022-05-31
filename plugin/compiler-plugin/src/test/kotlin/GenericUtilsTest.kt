/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import shapeTyping.analysis.DimShape
import shapeTyping.analysis.ShapeDeclarationInfo
import shapeTyping.analysis.SymbolicShape
import shapeTyping.analysis.utils.BoundSubstitutionIterator
import org.junit.Test
import kotlin.test.assertEquals

class GenericUtilsTest {
    @Test
    fun testIterator() {
        val dummyScopeID = 1
        val s = SymbolicShape("A", ShapeDeclarationInfo(DimShape(1), false, dummyScopeID))

        val iterator = BoundSubstitutionIterator(s, s)
        val pairs = iterator.asSequence().toList()

        val expectedPairs = listOf(
            Pair(s, s),
            Pair(s, s.declarationInfo.upperBound),
            Pair(s.declarationInfo.upperBound, s),
            Pair(s.declarationInfo.upperBound, s.declarationInfo.upperBound)
        )
        assertEquals(expectedPairs, pairs)
    }
}