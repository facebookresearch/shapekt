/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package playground.extensions

import shapeTyping.analysis.Dim
import shapeTyping.analysis.Shape
import shapeTyping.analysis.WildcardShape
import shapeTyping.extensions.annotations.ShapeFunction

// A user-defined shape function.
// The generated plugin extension ShapeFunctionExtension_foo can be found in
// extensions/build/generated/source/kapt/main/playground.extensions
//
// The generated plugin.xml file can be found in
// extensions/build/tmp/kapt3/classes/main/META-INF/plugin.xml .
// This file will be copied to extensions/resources/META-INF/plugin.xml upon
// calling `./gradlew buildPlugin`.
@ShapeFunction
fun foo(a: Dim, b: Shape): Shape {
    return WildcardShape.DEFAULT
}
