/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
  id("java-gradle-plugin")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
  implementation(project(":config"))
}

gradlePlugin {
  plugins {
    create("shapeTyping") {
      id = project.property("group") as String
      implementationClass = "shapeTyping.plugin.ShapeTypingGradlePlugin"
    }
  }
}
