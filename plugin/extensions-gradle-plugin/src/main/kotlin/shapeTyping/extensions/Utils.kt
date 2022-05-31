/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

import config.BuildConfig
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY

internal fun Project.addKaptDependency(
        dependencyNotation: Any
): Dependency? {
    return Kapt3GradleSubplugin.createAptConfigurationIfNeeded(this, SourceSet.MAIN_SOURCE_SET_NAME).let {
        project.dependencies.add(it.name, dependencyNotation)
    }
}

internal fun Project.addTensorTypingDependency(
        subprojectName: String
): Dependency? {
    return project.dependencies.add(COMPILE_ONLY, tensorTypingDependency(subprojectName))
}

internal inline fun <reified T : Task> Project.registerTask(
        name: String,
        noinline configuration: T.() -> Unit
): TaskProvider<T> {
    return this.tasks.register(name, T::class.java, configuration)
}

internal inline fun <reified T> ExtensionContainer.configure(noinline action: T.() -> Unit) {
    this.configure(T::class.java, action)
}

internal fun tensorTypingDependency(subprojectName: String) = "${BuildConfig.PLUGIN_GROUP}:$subprojectName:${BuildConfig.PLUGIN_VERSION}"
