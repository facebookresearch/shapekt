/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */


plugins {
  // When updating the Kotlin version, remember to update the
  // kotlin version in the playground project as well.
  kotlin("jvm") version "1.7.0-dev-444"
  `maven-publish`
}

allprojects {
  apply(plugin = "maven-publish")
  apply(plugin = "kotlin")

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  repositories {
    mavenCentral()
    maven {
      name = "KotlinBootstrap"
      url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
  }

  publishing {
    repositories {
      maven {
        name = "diffkt"
        url = uri("https://maven.pkg.github.com/facebookresearch/diffkt")
        credentials {
          username = System.getenv("GITHUB_ACTOR")
          password = System.getenv("GITHUB_TOKEN")
        }
      }
    }
  }

  group = project.property("group") as String

  version = project.property("version") as String


  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+ProperCheckAnnotationsTargetInTypeUsePositions"
  }
}
