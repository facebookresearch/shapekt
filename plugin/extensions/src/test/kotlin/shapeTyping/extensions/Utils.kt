/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

import shapeTyping.extensions.annotations.processors.ShapeFunctionProcessor
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.io.File

fun getResource(resourceName: String): File =
        File(Thread.currentThread().contextClassLoader.getResource(resourceName)!!.path)

fun SourceFile.Companion.fromResource(resourceName: String): SourceFile =
        fromPath(getResource(resourceName))

fun compile(
        sourceFiles: List<SourceFile>,
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        annotationProcessors = listOf(ShapeFunctionProcessor())
        inheritClassPath = true
    }.compile()
}

fun compile(
        sourceFile: SourceFile,
): KotlinCompilation.Result {
    return compile(listOf(sourceFile))
}