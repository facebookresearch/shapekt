/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.plugin.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import shapeTyping.analysis.SType

/**
 * WritableSlices for recording and extracting shape analysis information.
 */
object WritableSlices {
    val STYPE_FOR_EXPRESSION: WritableSlice<KtExpression, SType> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING, true) // isCollective = true in order to obtain keys for AbstractShapeTest
    val NOSTYPE_ON: WritableSlice<DeclarationDescriptor, Boolean> =
        BasicWritableSlice(RewritePolicy.DO_NOTHING, true)
}