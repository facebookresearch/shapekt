/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.extensions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.COMPILE_ONLY
import org.jetbrains.kotlin.gradle.utils.IMPLEMENTATION
import java.io.File

class ShapeTypingExtensionsGradlePlugin : Plugin<Project> {
    // Gradle Plugin extension for configuring the build. (The class needs to be open.)
    open class Config {
        var name: String = "ShapeTypingExtensionsPlugin"

        var description: String = "This is my wonderful extension to the ShapeTyping plugin!"

        var vendor: String = "DefaultVendor"

        var id: String = "my.project.ide"

        // Should match IntelliJ version of the main plugin. See ide-plugin/build.gradle.kts.
        var intelliJVersion: String = "2021.2"

        var buildSearchableOptions = false

        var generateIntelliJPlugin: Boolean = true
    }

    override fun apply(project: Project) {
        project.plugins.withType(KotlinPluginWrapper::class.java) {
            project.plugins.apply(Kapt3GradleSubplugin::class.java)
            project.extensions.create("shapeTypingExtensions", Config::class.java)
            val config = project.extensions.getByType(Config::class.java)
            if (config.generateIntelliJPlugin) {
                project.plugins.apply(IntelliJPlugin::class.java)

                // Copy generated plugin.xml to the resources folder.
                val generatePluginConfig = project.registerTask<Copy>("generatePluginConfig") {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    val destinationDir = File(project.projectDir, "src/main/resources/META-INF").also { it.mkdirs() }
                    val generatedConfig = File(project.buildDir, "tmp/kapt3/classes/main/META-INF/plugin.xml")
                    if (generatedConfig.exists()) {
                        from(generatedConfig.toURI())
                        into(destinationDir)
                    }
                }

                project.tasks.withType(KotlinCompile::class.java).forEach { it.finalizedBy(generatePluginConfig) }

                project.tasks.getByName("buildPlugin").dependsOn(generatePluginConfig)

                // TODO: Should this really be INCLUDE?
                project.tasks.withType(Jar::class.java).forEach { it.duplicatesStrategy = DuplicatesStrategy.INCLUDE }

                project.addTensorTypingDependency("ide-plugin")

                project.extensions.configure<IntelliJPluginExtension> {
                    pluginName = config.name
                    version = config.intelliJVersion
                    type = "IC"
                    updateSinceUntilBuild = false
                }

                if (!config.buildSearchableOptions) {
                    // This task is enabled by default with IntelliJPlugin.
                    project.tasks.getByName("buildSearchableOptions").enabled = false
                }
            }
            project.addTensorTypingDependency("analysis")
            project.addTensorTypingDependency("extensions")
            project.addKaptDependency(tensorTypingDependency("extensions"))
            project.dependencies.add(IMPLEMENTATION, "com.squareup:javapoet:1.13.0")

            // Kapt configurations
            project.extensions.configure<KaptExtension> {
                // These arguments are used by the annotation processor to generate the plugin.xml file.
                arguments {
                    arg("generateIntelliJPlugin", config.generateIntelliJPlugin)
                    arg("name", config.name)
                    arg("description", config.description)
                    arg("vendor", config.vendor)
                    arg("id", config.id)
                }
            }

            // Kapt
            project.addKaptDependency("com.google.auto.service:auto-service:1.0-rc7")
            project.dependencies.add(COMPILE_ONLY, "com.google.auto.service:auto-service:1.0-rc7")
        }
    }
}
