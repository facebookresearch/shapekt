/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    kotlin("jvm") version "1.7.0"
}

allprojects {
    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        }
        maven {
            name = "diffkt"
            url = uri("https://maven.pkg.github.com/facebookresearch/diffkt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-XXLanguage:+ProperCheckAnnotationsTargetInTypeUsePositions"
}
