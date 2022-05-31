/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:Suppress("unused") // usages in build scripts are not tracked properly

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// The below code was taken from the Bridge plugin: https://github.com/facebookincubator/differentiable/pull/519/files

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
    listOf("com.intellij",
        "com.google",
        "com.sampullara",
        "org.apache",
        "org.jdom",
        "org.picocontainer",
        "org.jline",
        "kotlinx.coroutines",
        "net.jpountz",
        "one.util.streamex")


/*
 * Relocation logic taken from <KotlinRoot>/buildSrc/src/main/kotlin/embeddable.kt
 * Allows us to go from kotlin-compiler -> kotlin-compiler-embeddable
 */
fun ShadowJar.relocateCompilerPackages() {
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    relocate("com.intellij.openapi.util", "org.jetbrains.kotlin.com.intellij.openapi.util")

    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it") {
            exclude("com.google.auto.service.AutoService")
        }
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}
