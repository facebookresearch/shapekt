/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import org.junit.Test

class DiagnosticTest: AbstractDiagnosticTest() {
    @Test
    fun testSTypeBasic() {
        diagnosticTest("diagnostics/STypeDiagnosticTest.kt")
    }

    @Test
    fun testSTypeTuple() {
        diagnosticTest("diagnostics/STypeTupleDiagnosticTest.kt")
    }

    @Test
    fun testShapeFunctions() {
        diagnosticTest("diagnostics/ShapeFunctionDiagnosticTest.kt")
    }

    @Test
    fun testExtensionFunctions() {
        diagnosticTest("diagnostics/ExtensionFunctionDiagnosticTest.kt")
    }

    @Test
    fun testClassMethods() {
        diagnosticTest("diagnostics/MethodDiagnosticTest.kt")
    }

    @Test
    fun testClassMembers() {
        diagnosticTest("diagnostics/ClassMemberDiagnosticTest.kt")
    }

    @Test
    fun testRepeatedCallShadowing() {
        diagnosticTest("diagnostics/RepeatedCallShadowingDiagnosticTest.kt")
    }

    @Test
    fun testStarProjection() {
        diagnosticTest("diagnostics/RecursiveKotlinTypeDiagnosticTest.kt")
    }

    @Test
    fun testListOps() {
        diagnosticTest("diagnostics/ListDiagnosticTest.kt")
    }

    @Test
    fun testControlFlow() {
        diagnosticTest("diagnostics/ControlFlowDiagnosticTest.kt")
    }

    @Test
    fun testBadAnnotation() {
        diagnosticTest("diagnostics/BadAnnotationDiagnosticTest.kt")
    }

    @Test
    fun testNamedArg() {
        diagnosticTest("diagnostics/NamedArgDiagnosticTest.kt")
    }

    @Test
    fun testVararg() {
        diagnosticTest("diagnostics/VarargDiagnosticTest.kt")
    }

    @Test
    fun testDefaultArg() {
        // TODO (#88): Disabled until we can get STypes for default values
        // diagnosticTest("diagnostics/DefaultArgDiagnosticTest.kt")
    }

    @Test
    fun testSubstitutedShapeFunction() {
        diagnosticTest("diagnostics/ShapeFunctionSubstitutionDiagnosticTest.kt")
    }

}