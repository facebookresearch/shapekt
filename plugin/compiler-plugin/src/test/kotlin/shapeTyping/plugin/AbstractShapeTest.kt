/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import shapeTyping.plugin.analysis.WritableSlices
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import com.intellij.mock.MockProject
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import kotlin.test.assertEquals

abstract class AbstractShapeTest: ShapeTypingTest {
    /**
     * [shapeTest] tests for shape correctness of KtExpressions.
     *
     * The resource file obtained from [resourceName] should include markers to denote expected shapes.
     *
     * The resource file (without shape markers) will be run through the compiler and the tensor typing plugin
     * to obtain the actual shape info for comparison.
     *
     * The format of shape markers should be: <!Shape($expectedShape)!>expression<!>
     *
     * Example of a resource file:
     *
     *  @DeclareParams("A: [___]", "B: [___]")
     *  fun foo(a: @ShapeOf("A") Tensor, b: @ShapeOf("broadcast(A,B)") Tensor) {
     *      <!Shape(A:[___])!>a<!>
     *      <!Shape(broadcast(A:[___],B:[___]))!>b<!>
     *  }
     */
    fun shapeTest(resourceName: String) {
        val file = getResource(resourceName)
        val inputText = file.readText()

        // Create environment
        val environment: KotlinCoreEnvironment = ShapeTypingTest.createEnvironment()

        // Register tensor typing plugin
        ShapeTypingComponentRegistrar().registerProjectComponents(
            environment.project as MockProject, // Ignore this IDE error. The gradle build uses shadow jars to get around this.
            environment.configuration
        )

        // Clear text of shape information.
        val clearText = clearShapeRanges(inputText)

        // Analyze file to obtain shape information.
        val ktfile = ShapeTypingTest.createFile("shapeTestInput.kt", clearText, environment.project)
        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            environment.project,
            listOf(ktfile),
            NoScopeRecordCliBindingTrace(),environment.configuration.copy().apply {
                this.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
            },
            environment::createPackagePartProvider
        )

        // Extract marked shapes and their text ranges.
        val shapeRanges = parseShapeRanges(inputText)
        val ranges = shapeRanges.map { it.range }

        // Find marked expressions.
        val markedExpressions = result.bindingContext.getKeys(WritableSlices.STYPE_FOR_EXPRESSION).filter {
            ranges.contains(it.textRange) // Only check shapes of marked ranges.
        }

        // Collect actual shape information.
        val actualShapeRanges = markedExpressions.map { expression ->
            val range = expression.textRange
            val shapeStr = result.bindingContext.get(WritableSlices.STYPE_FOR_EXPRESSION, expression)!!.toString()
            ShapeRange(shapeStr, range)
        }

        // Re-add expected shape information to text. This ensures that the shape information is cleaned/normalized.
        val expectedText = addShapeRanges(clearText, shapeRanges)

        // Add actual shape information to text.
        val actualText = addShapeRanges(clearText, actualShapeRanges)

        assertEquals(expected = expectedText, actual = actualText)
    }

    companion object {
        private data class ShapeRange(val value: String, val range: TextRange)

        private val startPattern = Regex("<!Shape\\(.+\\)!>")
        private val endPattern = Regex("<!>")
        private fun parseShapeRanges(text: String): List<ShapeRange> {
            val startMatches = startPattern.findAll(text).toList()
            val endMatches = endPattern.findAll(text).toList()
            require(startMatches.size == endMatches.size)
            var offsetCompensation = 0
            return startMatches.zip(endMatches).map { (start, end) ->
                val effectiveOffset = start.range.first - offsetCompensation
                val markedExpressionLen = end.range.first - start.range.last - 1
                offsetCompensation += start.value.length + end.value.length
                ShapeRange(
                    start.value.removePrefix("<!Shape(").removeSuffix(")!>").removeWhiteSpace(),
                    TextRange(effectiveOffset, effectiveOffset + markedExpressionLen)
                )
            }
        }

        private fun clearShapeRanges(text: String): String {
            return text.replace(startPattern, "").replace(endPattern, "")
        }

        private fun String.removeWhiteSpace() = this.replace(Regex("\\s"), "")

        private fun addShapeRanges(text: String, shapeRanges: List<ShapeRange>): String {
            val sortedRanges = shapeRanges.sortedBy { it.range.startOffset }
            val builder = StringBuilder()
            var offset = 0
            sortedRanges.forEach { (value, range) ->
                builder.append(text.substring(offset, range.startOffset))
                builder.append("<!Shape(${value.removeWhiteSpace()})!>")
                builder.append(text.substring(range.startOffset, range.endOffset))
                builder.append("<!>")
                offset = range.endOffset
            }
            builder.append(text.substring(offset))
            return builder.toString()
        }
    }
}
