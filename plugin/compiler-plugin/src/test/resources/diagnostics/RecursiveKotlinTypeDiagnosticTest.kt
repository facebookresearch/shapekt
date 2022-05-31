/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// This test doesn't yet include any shape-checking, but it makes sure we don't break this language feature.

abstract class A<T : A<T>> {
    abstract val components: List<A<*>>
    abstract val componentsWithParameter: List<A<T>>

    val firstFromStarProjection = components.first()
    val firstFromTypeProjection = componentsWithParameter.first()
}