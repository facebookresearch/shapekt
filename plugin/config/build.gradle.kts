/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

plugins {
    id("com.github.gmazzo.buildconfig") version "3.0.2"
}

buildConfig {
    packageName("config")
    buildConfigField("String", "PLUGIN_GROUP", "\"$group\"")
    buildConfigField("String", "PLUGIN_NAME", "\"compiler-plugin\"")
    buildConfigField("String", "PLUGIN_ID", "\"\$PLUGIN_GROUP.\$PLUGIN_NAME\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"$version\"")
}

publishing {
    publications {
        create<MavenPublication>("config") {
            from(components["java"])
        }
    }
}
