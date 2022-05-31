/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

publishing {
    publications {
        create<MavenPublication>("annotations") {
            from(components["java"])
        }
    }
}

dependencies {
    // TODO: Remove this dependency after we remove Matmul.kt
    compileOnly(project(":analysis"))
}
