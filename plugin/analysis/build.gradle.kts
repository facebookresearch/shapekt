/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

publishing {
    publications {
        create<MavenPublication>("analysis") {
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
