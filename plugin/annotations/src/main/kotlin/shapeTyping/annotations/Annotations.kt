/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package shapeTyping.annotations

/**
 * If used on a type, denotes the shape data of the instance of that type.
 * The shape is computed from its argument string, which is a list of shape arguments (currently dimensions or shapes).
 *
 * Example type usages:
 * val a: @SType("[Batches, 2, 3]") Tensor // Generic dimension vals (like Batches) are supported (also see Declare below)
 * val a: @SType("[1, 2, _]") Tensor // _ is used to denote an unknown dimension value
 * val a: @SType("[___]") Tensor // [___] is used to denote an unknown shape value
 * val a: @SType("matmul([1, 2], [2, 1])") // get the shape of a shape function call. Shape function calls can be nested.
 * val layer: @SType("[Batches, 3], 3") Layer // multiple arguments may describe the shape of an object with several shaped arguments
 *
 * If used on a class or function declaration, it declares new generic shapes or dimensions.
 *
 * _ is used to denote an unknown dimension; [___] is used to denote an unknown shape.
 * Example usage:
 * @SType("D: _", "S1: [D, 10]", "S2: [10, D]", "S3: [___]") // D is a generic dimension; the rest are generic shapes
 * fun foo(a: @SType("S1") Tensor, b: @SType("S2") Tensor, c: @SType("S3") Tensor, d: @SType("[D, D]") Tensor) { ... }
 */
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class SType(val value: String)

/**
 * Applied to a scope, this allows the SType checker to accept incomplete evaluation of STypes - usually in cases where
 * one or more SType variables cannot be resolved to a concrete value.
 *
 * For example, a user might write an identity function:
 *
 * @AllowUnreduced
 * @SType("S: Shape")
 * fun id(t: @SType("S") Tensor): @SType("S") Tensor = t
 *
 * The value of S cannot be further reduced at compile time, but we'd want to allow this usage.
 *
 * NOTE: This annotation is processed by [STypeChecker]. If the default parameter or usage is changed, make sure to
 * update it there.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR
)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowUnreduced(val allow: Boolean = true)

/**
 * This disables any SType checking on functions defined inside the annotated scope.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoSType