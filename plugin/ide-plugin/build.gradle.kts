/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    id("org.jetbrains.intellij") version "1.2.1"
}

repositories {
    mavenCentral()
}

intellij {
    pluginName.set("shapeTyping")
    // Should match default value in TensorTypingExtensionsGradlePlugin.Config.intelliJVersion
    version.set("2021.2")
    type.set("IC")
    plugins.set(listOf("org.jetbrains.kotlin"))
}


publishing {
    publications {
        create<MavenPublication>("ide") {
            from(components["java"])
            artifact(tasks.buildPlugin.get())
        }
    }
}

// Disable buildSearchableOptions task because we do not need it, and
// it produces errors nondeterministically
tasks.getByName("buildSearchableOptions").enabled = false

dependencies {
    implementation(project(":compiler-plugin"))
    implementation(project(":extensions"))
    implementation(project(":analysis"))
    implementation(project(":annotations"))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler")
}
