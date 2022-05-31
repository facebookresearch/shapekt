/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import shapeTyping.plugin.ShapeTypingTest.Companion.createEnvironment
import shapeTyping.plugin.ShapeTypingTest.Companion.createFile
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import kotlin.test.assertEquals

abstract class AbstractDiagnosticTest: ShapeTypingTest {
    /**
     * [diagnosticTest] tests for correctness of reported diagnostics.
     *
     * The resource file obtained from [resourceName] should include markers to denote expected diagnostics.
     * The resource file (without the diagnostic markers) will be run through the compiler and the tensor typing plugin
     * to obtain the actual diagnostics. These actual diagnostics will be reapplied to the resource file and compared to
     * the original file.
     *
     * [ignore] is a list of Diagnostics to be filtered from actual diagnostics result.
     * We assume that the input resource file does not contain markers for any ignored diagnostics.
     *
     * Example of a resource file (with [ignore] set to [commonlyIgnored]):
     *  fun foo() {
     *      val a: String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>5<!>
     *  }
     *
     */
    fun diagnosticTest(resourceName: String, ignore: List<DiagnosticFactory<*>> = commonlyIgnored) {
        val file = getResource(resourceName)
        val expectedText = file.readText()

        // Remove diagnostics from input file.
        val clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, ArrayList())

        // Create environment
        val environment: KotlinCoreEnvironment = createEnvironment()

        // Register tensor typing plugin
        ShapeTypingComponentRegistrar().registerProjectComponents(
                environment.project as MockProject, // Ignore this IDE error. The gradle build uses shadow jars to get around this.
                environment.configuration
        )

        // Analyze file to obtain diagnostics.
        val ktfile = createFile("diagnosticsTestInput.kt", clearText, environment.project)
        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.project,
                listOf(ktfile),
                NoScopeRecordCliBindingTrace(),environment.configuration.copy().apply {
            this.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
        },
                environment::createPackagePartProvider
        )

        // Collect actual diagnostics.
        val actualDiagnostics = result.bindingContext.diagnostics.all()
                .filterNot { ignore.contains(it.factory) } // Remove ignored diagnostics.
                .map { ActualDiagnostic(it, null, true) }

        // Add diagnostics to text.
        val resultText = CheckerTestUtil.addDiagnosticMarkersToText(ktfile, actualDiagnostics).toString()

        assertEquals(expected = expectedText, actual = resultText)
    }

    companion object {
        // Commonly ignored diagnostics.
        val commonlyIgnored: List<DiagnosticFactory<*>> = listOf(
                Errors.UNUSED_VARIABLE,
                Errors.UNCHECKED_CAST,
                Errors.UNUSED_PARAMETER,
                Errors.CAST_NEVER_SUCCEEDS
        )
    }
}
