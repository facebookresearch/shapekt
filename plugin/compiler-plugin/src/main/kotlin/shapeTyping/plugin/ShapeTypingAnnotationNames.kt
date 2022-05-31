/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin

import config.BuildConfig.PLUGIN_GROUP
import org.jetbrains.kotlin.name.FqName

object ShapeTypingAnnotationFqNames {
    const val PACKAGE = "shapeTyping"
    val PREFIX = "$PACKAGE.annotations"
    val STYPE_FQNAME = FqName("$PREFIX.SType")
    val ALLOW_UNREDUCED_FQNAME = FqName("$PREFIX.AllowUnreduced")
    val SHAPEFUNCTION_FQNAME = FqName("$PACKAGE.extensions.annotations.ShapeFunction")
    val NO_STYPE = FqName("$PREFIX.NoSType")
}