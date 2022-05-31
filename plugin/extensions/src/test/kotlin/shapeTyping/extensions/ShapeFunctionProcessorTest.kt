/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

import shapeTyping.analysis.Dim
import shapeTyping.analysis.Shape
import shapeTyping.analysis.WildcardDim
import shapeTyping.analysis.WildcardShape
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShapeFunctionProcessorTest {
    @Test
    fun test() {
        val source = SourceFile.kotlin("Extensions.kt", """
            import shapeTyping.extensions.annotations.ShapeFunction
            import shapeTyping.analysis.*

            @ShapeFunction
            fun foo(a: Dim, b: Shape): Shape {
                return WildcardShape.DEFAULT
            }
        """.trimIndent())

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generatedClass = result.classLoader.loadClass("ShapeFunctionExtension_foo")
        println(generatedClass.declaredMethods.map { it.name })
        val instance = generatedClass.newInstance()

        assertEquals(
                "foo",
                generatedClass.getDeclaredMethod("getName").invoke(instance)
        )

        assertEquals(
                listOf("a" to Dim::class.java, "b" to Shape::class.java),
                generatedClass.getDeclaredMethod("getParameters").invoke(instance)
        )

        assertEquals(
                Shape::class.java,
                generatedClass.getDeclaredMethod("getReturnType").invoke(instance)
        )

        assertEquals(
                WildcardShape.DEFAULT,
                generatedClass.getDeclaredMethod("apply", List::class.java).invoke(
                        instance,
                        listOf(WildcardDim.DEFAULT, WildcardShape.DEFAULT)
                )
        )

        // Bad arguments.
        assertFailsWith(InvocationTargetException::class) {
            generatedClass.getDeclaredMethod("apply", List::class.java).invoke(
                    instance,
                    listOf(WildcardShape.DEFAULT, WildcardDim.DEFAULT)
            )
        }

        assertFailsWith(InvocationTargetException::class) {
            generatedClass.getDeclaredMethod("apply", List::class.java).invoke(
                    instance,
                    listOf(WildcardDim.DEFAULT)
            )
        }
    }
}
