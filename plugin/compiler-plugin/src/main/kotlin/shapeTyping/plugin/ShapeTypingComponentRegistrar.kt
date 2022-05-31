/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import shapeTyping.extensions.ShapeFunctionExtension
import shapeTyping.plugin.extensions.ExtensionLoader
import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.TypeAttributeTranslatorExtension
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import java.net.URLClassLoader

@AutoService(ComponentRegistrar::class)
class ShapeTypingComponentRegistrar: ComponentRegistrar {
  @Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
  @OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {
    // Load tensor typing extensions from classpath files.
    val urls = configuration.jvmClasspathRoots.map { it.toURI().toURL() }.toTypedArray()
    val urlClassLoader = URLClassLoader(urls, this::class.java.classLoader)
    ExtensionLoader.load(ShapeFunctionExtension::class, urlClassLoader)

    StorageComponentContainerContributor.registerExtension(project,
        ShapeTypingContainerContributor()
    )

    TypeAttributeTranslatorExtension.registerExtension(project,
        STypeAttributeTranslator()
    )

    CandidateInterceptor.registerExtension(project, STypeCandidateInterceptor())

    STypeDiagnosticSuppressor.registerExtension(project, STypeDiagnosticSuppressor())
  }
}


