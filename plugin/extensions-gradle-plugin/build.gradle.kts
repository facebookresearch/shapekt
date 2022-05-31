/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
}

gradlePlugin {
    plugins {
        create("extensions-gradle-plugin") {
            val group = project.property("group") as String
            id = "$group.extensions"
            implementationClass = "shapeTyping.extensions.ShapeTypingExtensionsGradlePlugin"
        }
    }
}

repositories {
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    implementation(kotlin("gradle-plugin-api"))
    implementation(project(":config"))
    implementation("org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.7.1")
}
