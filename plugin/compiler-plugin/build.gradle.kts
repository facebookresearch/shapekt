/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("kapt")
  id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar> {
  from(sourceSets.main.get().output)

  // This causes weird IntelliJ highlighting behavior (can't find classes, but compiles fine)
  // when adding :compiler-plugin as a gradle dependency on an outside project.
  // As a workaround, consuming projects should not add the :compiler-plugin project as a dependency.
  from(sourceSets.main.get().runtimeClasspath)

  // Relocate imported compiler packages to match kotlin-compiler-embeddable
  relocateCompilerPackages()
}

// Publishing
val pluginJar by tasks.registering(ShadowJar::class)

publishing {
  publications {
    create<MavenPublication>("compiler-plugin") {
      artifact(pluginJar)
    }
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-compiler")
  compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
  kapt("com.google.auto.service:auto-service:1.0-rc7")

  testImplementation(kotlin("test-junit"))
  testImplementation("org.jetbrains.kotlin:kotlin-compiler")
  testImplementation("com.google.auto.service:auto-service-annotations:1.0-rc7")


  implementation(project(":annotations"))
  implementation(project(":config"))
  implementation(project(":analysis"))
  implementation(project(":extensions"))
  implementation(project(":shape-functions"))
}
