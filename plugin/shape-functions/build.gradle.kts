/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    kotlin("kapt")
}

publishing {
    publications {
        create<MavenPublication>("shape-functions") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":extensions"))
    implementation(project(":analysis"))

    kapt(project(":extensions"))

    implementation("com.squareup:javapoet:1.13.0")
    compileOnly("com.google.auto.service:auto-service:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")

    testImplementation(kotlin("test-junit"))
}
