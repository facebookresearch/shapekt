/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

rootProject.name = "shapeTyping-plugin"

include(
        ":annotations",
        ":config",
        ":gradle-plugin",
        ":compiler-plugin",
        ":ide-plugin",
        ":extensions",
        ":analysis",
        ":extensions-gradle-plugin",
        ":shape-functions"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        }
        maven {
            name = "shapeTyping"
            url = uri("https://maven.pkg.github.com/facebookresearch/shapeTyping")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
