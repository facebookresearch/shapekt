/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

publishing {
    publications {
        create<MavenPublication>("extensions") {
            from(components["java"])
        }
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    from(sourceSets.main.get().output)

    // This causes weird IntelliJ highlighting behavior (can't find classes, but compiles fine)
    // when adding :compiler-plugin as a gradle dependency on an outside project.
    // As a workaround, consuming projects should not add the :compiler-plugin project as a dependency.
    from(sourceSets.main.get().runtimeClasspath)

    // Relocate imported compiler packages to match kotlin-compiler-embeddable
    relocateCompilerPackages()
}

// The dependency com.github.tschuchortdev:kotlin-compile-testing requires kotlin-compiler-embeddable
// so we need to use the relocated shadowJar.
tasks.withType<Test> {
    // Run shadowJar task first
    dependsOn(tasks.shadowJar)

    // Add shadowJar to classpath for tests
    this.classpath += tasks.shadowJar.get().outputs.files
    // Remove the old classpaths that are replaced by the shadowJar
    this.classpath -= sourceSets.main.get().runtimeClasspath

    // show print output
    testLogging.showStandardStreams = true
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}

dependencies {
    compileOnly(project(":analysis"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler")

    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")

    implementation("com.squareup:javapoet:1.13.0")

    testImplementation(project(":analysis"))
    testImplementation(kotlin("test-junit"))
    testImplementation("com.google.auto.service:auto-service-annotations:1.0-rc7")

    // We are using 1.5.31 for testing because kotlin-compile-testing is not currently compatible with Kotlin 1.6
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.5.31")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.4")
}
