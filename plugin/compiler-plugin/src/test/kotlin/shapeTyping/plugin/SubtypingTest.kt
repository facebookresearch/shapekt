/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.junit.Test
import shapeTyping.analysis.*
import shapeTyping.analysis.utils.STypeSubtyper.isStrictSubtypeOf
import shapeTyping.analysis.utils.STypeSubtyper.isSubtypeOf

class SubtypingTest {

    @Test
    fun testDimShapeToAnyShape() {
        val anyShape = WildcardShape.DEFAULT
        val dimShape = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT)
        assert(dimShape.isSubtypeOf(anyShape))
        assert(dimShape.isStrictSubtypeOf(anyShape))
    }

    @Test
    fun testSymbolicShapeToAnyShape() {
        val anyShape = WildcardShape.DEFAULT
        val symShape = SymbolicShape("A", ShapeDeclarationInfo(
            upperBound = WildcardShape.DEFAULT,
            isStrictBound = false,
            scopeID = 0
        ))
        assert(symShape.isSubtypeOf(anyShape))
        assert(!symShape.isStrictSubtypeOf(anyShape))
    }

    @Test
    fun testSymbolicShapeBoundedToAnyShape() {
        val anyShape = WildcardShape.DEFAULT
        val symShape = SymbolicShape("A", ShapeDeclarationInfo(
            upperBound = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT),
            isStrictBound = false,
            scopeID = 0
        ))
        assert(symShape.isSubtypeOf(anyShape))
        assert(symShape.isStrictSubtypeOf(anyShape))
    }

    @Test
    fun testSymbolicShapeToAnyShapeStrict() {
        val anyShape = WildcardShape.DEFAULT
        val symShape = SymbolicShape("A", ShapeDeclarationInfo(
            upperBound = WildcardShape.DEFAULT,
            isStrictBound = true,
            scopeID = 0
        ))
        assert(symShape.isSubtypeOf(anyShape))
        assert(symShape.isStrictSubtypeOf(anyShape))
    }

    @Test
    fun testSymbolicToSelf() {
        val symShape = SymbolicShape("A", ShapeDeclarationInfo(
            upperBound = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT),
            isStrictBound = false,
            scopeID = 0
        ))
        assert(symShape.isSubtypeOf(symShape))
        assert(!symShape.isStrictSubtypeOf(symShape))
    }

    @Test
    fun testDimShapeToSelf() {
        val dimShape = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT)
        assert(dimShape.isSubtypeOf(dimShape))
        assert(!dimShape.isStrictSubtypeOf(dimShape))
    }

    @Test
    fun testSymbolicToSymbolic() {
        val supershape = SymbolicShape("A", ShapeDeclarationInfo(
            upperBound = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT),
            isStrictBound = false,
            scopeID = 0
        ))
        val intermediate = SymbolicShape("B", ShapeDeclarationInfo(
            upperBound = supershape,
            isStrictBound = false,
            scopeID = 0
        ))
        val s1 = SymbolicShape("B", ShapeDeclarationInfo(
            upperBound = intermediate,
            isStrictBound = false,
            scopeID = 0
        ))
        val s2 = SymbolicShape("C", ShapeDeclarationInfo(
            upperBound = supershape,
            isStrictBound = false,
            scopeID = 0
        ))
        assert(!s1.isSubtypeOf(s2)) // Share a bound but no subtyping r elation
        assert(s1.isSubtypeOf(supershape))
        assert(s1.isSubtypeOf(intermediate))

        // No strict bounds
        assert(!s1.isStrictSubtypeOf(supershape))
        assert(!s1.isStrictSubtypeOf(intermediate))
    }

    @Test
    fun testShapeWithWildcardDims() {
        val s1 = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT)
        val s2 = DimShape(NumericDim(2), WildcardDim.DEFAULT, WildcardDim.DEFAULT)
        assert(s1.isSubtypeOf(s2))
        assert(s1.isStrictSubtypeOf(s2))
    }

    @Test
    fun testDimShapeMismatch() {
        val s1 = DimShape(NumericDim(2), NumericDim(3), WildcardDim.DEFAULT)
        val s2 = DimShape(NumericDim(3), WildcardDim.DEFAULT, WildcardDim.DEFAULT)
        assert(!s1.isSubtypeOf(s2))
        assert(!s1.isStrictSubtypeOf(s2))
    }

    @Test
    fun testSymbolicToWrongDimShape() {
        val a = SymbolicShape("A", ShapeDeclarationInfo(DimShape(1,2,3), false, 0))
        val b = DimShape(WildcardDim.DEFAULT, NumericDim(3), NumericDim(3))
        assert(!a.isSubtypeOf(b))
        assert(!a.isStrictSubtypeOf(b))
    }

    @Test
    fun testSymbolicToDimShapeIndirectBound() {
        val a = SymbolicShape("A", ShapeDeclarationInfo(DimShape(1,2,3), false, 0))
        val b = DimShape(WildcardDim.DEFAULT, NumericDim(2), NumericDim(3))
        assert(a.isSubtypeOf(b))
        assert(a.isStrictSubtypeOf(b))
    }

}