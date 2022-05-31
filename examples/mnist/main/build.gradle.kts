/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    id("shapeTyping") version "0.1.0-SNAPSHOT"
}

dependencies {
    // We need a dependency to our extensions in order to use them.
    implementation(project(":extensions"))

    implementation("org.diffkt:api:unspecified")
    implementation("shapeTyping:annotations:0.1.0-SNAPSHOT")
    implementation("shapeTyping:shape-functions:0.1.0-SNAPSHOT")
}
