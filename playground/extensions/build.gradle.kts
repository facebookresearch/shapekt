/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    // Apply the tensor typing extensions plugin so you can start writing shape functions.
    // This will automatically add all necessary dependencies and configurations.
    id("shapeTyping.extensions") version "0.1.0-SNAPSHOT"
}

// Configure the tensor typing extensions.
// These configurations mainly affect building the IntelliJ plugin extension.
// Most of these configuration changes are notably visible in the generated
// plugin.xml file.
shapeTypingExtensions {
    name = "PlaygroundExtensions"
    vendor = "Facebook"
    id = "playground.extensions"
}
